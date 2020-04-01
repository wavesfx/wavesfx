package com.wavesfx.wavesfx.gui.transfer;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.assets.AssetCell;
import com.wavesfx.wavesfx.gui.assets.AssetConverterProfile;
import com.wavesfx.wavesfx.gui.dialog.ConfirmMassTransferController;
import com.wavesfx.wavesfx.gui.style.StyleHandler;
import com.wavesfx.wavesfx.logic.AddressValidator;
import com.wavesfx.wavesfx.logic.FormValidator;
import com.wavesfx.wavesfx.logic.Transferable;
import com.wavesfx.wavesfx.utils.ApplicationSettings;
import com.wavesplatform.wavesj.Transactions;
import com.wavesplatform.wavesj.Transfer;
import com.wavesplatform.wavesj.transactions.MassTransferTransaction;
import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toLong;
import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;
import static com.wavesfx.wavesfx.logic.FormValidator.MASS_TX_PATTERN;

public class MassTransferController extends TransferTransactionController  {
    private static final Logger log = LogManager.getLogger();

    @FXML private ComboBox<Transferable> assetComboBox;
    @FXML private TextField amountTextField;
    @FXML private TextField feeTextField;
    @FXML private TextArea messageTextArea;
    @FXML private TextArea recipientTextArea;
    @FXML private Button sendButton;
    @FXML private Label messageLimitLabel;
    @FXML private Label errorLabel;
    @FXML private Tooltip errorTooltip;

    MassTransferController(final RxBus rxBus) {
        super(rxBus);
    }

    @FXML
	public void initialize() {
        assetComboBox.setConverter(new AssetConverterProfile());
        assetComboBox.setCellFactory(param ->  new AssetCell());

        rxBus.getAssetList().observeOn(JavaFxScheduler.platform())
                .subscribe(this::updateComboBoxes);

        final var assetObservable = JavaFxObservable.nullableValuesOf(assetComboBox.valueProperty());

        final var assetIsNotEmptyObservable = assetObservable
                .map(transferable -> !assetComboBox.getSelectionModel().isEmpty());


        final var recipientsObservable = JavaFxObservable.valuesOf(recipientTextArea.textProperty());

        final var recipientIsValidObservable = recipientsObservable
                .observeOn(Schedulers.computation())
                .map(this::hasValidRecipients);

        final var validMassTxFormObservable = recipientsObservable
                .doOnNext(s -> disableSendButton())
                .observeOn(Schedulers.io())
                .filter(this::hasValidRecipients);

        final var validMassTxFormBooleanObservable = recipientsObservable
                .doOnNext(s -> disableSendButton())
                .observeOn(Schedulers.io())
                .map(this::hasValidRecipients);

        final var transferListObservable = validMassTxFormObservable
                .map(this::getRecipients)
                .map(strings -> fetchListOfTransfers(assetComboBox.getSelectionModel().getSelectedItem(), strings));

        final var messageIsValidObservable = JavaFxObservable.valuesOf(messageTextArea.textProperty())
                .map(this::isValidMessage);

        final var assetAndAmountIsValidObservable = ConnectableObservable.combineLatest(
                JavaFxObservable.valuesOf(amountTextField.textProperty()),
                JavaFxObservable.valuesOf(assetComboBox.valueProperty()),
                privateKeyAccountSubject, (amount, asset, privateKeyAccount) -> isValidAmount(amount)).cache();

        StyleHandler.setBorderDisposable(recipientIsValidObservable, recipientTextArea);
        StyleHandler.setBorderDisposable(assetAndAmountIsValidObservable, amountTextField);
        StyleHandler.setBorderDisposable(messageIsValidObservable, messageTextArea);
        StyleHandler.setBorderDisposable(assetIsNotEmptyObservable, assetComboBox);

        transferListObservable
                .observeOn(JavaFxScheduler.platform())
                .doOnNext(t -> setCalculateFeeText())
                .observeOn(Schedulers.computation())
                .map(transfers -> toReadable(calculateTotalAmount(transfers), assetComboBox.getSelectionModel().getSelectedItem().getDecimals()))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(amountTextField::setText);

        final var transactionsWithFeeObservable =
                transferListObservable
                .observeOn(Schedulers.io())
                .filter(transfers -> assetComboBox.getSelectionModel().getSelectedItem() != null)
                .switchMap(s -> Observable.just(s).delay(ApplicationSettings.INPUT_REQUEST_DELAY, TimeUnit.MILLISECONDS))
                .map(this::calculateFees)
                .doOnNext(rxBus.getMassTransferTransactions()::onNext);

        final var hasSufficientFundsObservable = transactionsWithFeeObservable
                .map(this::getTotalFee)
                .map(this::isValidFee);

        StyleHandler.setBorderDisposable(hasSufficientFundsObservable, feeTextField);

        transactionsWithFeeObservable
                .map(this::getTotalFee)
                .map(fee -> toReadable(fee, mainToken.getDecimals()))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(feeTextField::setText);

        Observable.combineLatest(validMassTxFormBooleanObservable, messageIsValidObservable, assetIsNotEmptyObservable,
                hasSufficientFundsObservable, FormValidator::areValid)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(b -> sendButton.setDisable(!b));

        JavaFxObservable.actionEventsOf(sendButton)
                .subscribe(ae -> sendTranscation());

    }

    private void updateComboBoxes(List<Transferable> assetList) {
        reinitializeComboBox(assetComboBox, assetList);
    }

    private boolean hasValidRecipients(final String address) {
        final var selectedAsset = assetComboBox.getSelectionModel().getSelectedItem();
        final String[] splitString = getRecipients(address);
        if (isWellFormed(splitString) || selectedAsset == null) return false;

        final var transferList = fetchListOfTransfers(selectedAsset, splitString);
        final var listOfInvalidAddresses = getListOfInvalidAddresses(transferList);
        setTooltipMessage(listOfInvalidAddresses);
        return listOfInvalidAddresses.isEmpty();
    }


    private String[] getRecipients(final String address) {
        return address.replaceAll(" ", "").split("\\n");
    }

    private void sendTranscation() {
        final var parent = loadParent(FXMLView.CONFIRM_MASS_TRANSFER, new ConfirmMassTransferController(rxBus));
        createDialog(parent);
    }

    private void setTooltipMessage(final List<String> listOfInvalidAddresses) {
        final var errorMessage = getMessages().getString("invalid_addresses") + ":\n";
        if (!listOfInvalidAddresses.isEmpty()) {
            errorLabel.setVisible(true);
            errorTooltip.setText(errorMessage + tooltipErrorMessage(listOfInvalidAddresses));
        } else {
            errorLabel.setVisible(false);
            errorTooltip.setText("");
        }
    }

    private Long calculateTotalAmount(final List<Transfer> transferList) {
        return transferList.stream()
                .map(Transfer::getAmount)
                .reduce(0L, Long::sum);
    }

    private String tooltipErrorMessage(final List<String> hasValidAddresses) {
        return hasValidAddresses.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(",\n"));
    }

    private List<String> getListOfInvalidAddresses(final List<Transfer> transferList) {
        final var privateKeyAccount = getPrivateKeyAccount();
        return transferList.stream()
                .map(Transfer::getRecipient)
                .filter(recipient -> !AddressValidator.validateAddress(recipient, privateKeyAccount.getChainId()))
                .limit(10)
                .collect(Collectors.toUnmodifiableList());
    }

    private List<Transfer> fetchListOfTransfers(final Transferable selectedAsset, final String[] transfers) {
        return Arrays.stream(transfers)
                .parallel()
                .map(string -> string.split(","))
                .map(s -> new Transfer(s[0], toLong(s[1], selectedAsset.getDecimals())))
                .collect(Collectors.toUnmodifiableList());
    }

    private boolean isWellFormed(final String[] transfers) {
        return Arrays.stream(transfers)
                .anyMatch(strings -> !FormValidator.isWellFormed(strings, MASS_TX_PATTERN));
    }

    private List<MassTransferTransaction> calculateFees(final List<Transfer> transferList) {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var assetId = Optional.ofNullable(assetComboBox.getSelectionModel().getSelectedItem().getAssetId()).orElse("");
        final var batchedTransferList = getBatches(transferList, ApplicationSettings.MAX_MASS_TX_SIZE);
        return batchedTransferList.stream()
                .map(txList -> Transactions.makeMassTransferTx(privateKeyAccount, assetId, txList, 0L, messageTextArea.getText()))
                .map(this::recalculateFee)
                .collect(Collectors.toUnmodifiableList());
    }

    private MassTransferTransaction recalculateFee(MassTransferTransaction transaction) {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var fee = getNodeService().calculateFee(transaction).orElse(ApplicationSettings.TOKEN_MAX_AMOUNT);
        return Transactions.makeMassTransferTx(privateKeyAccount, transaction.getAssetId(), transaction.getTransfers(), fee,"");
    }

    private long getTotalFee(List<MassTransferTransaction> transactionList) {
        return transactionList.stream()
                .map(MassTransferTransaction::getFee)
                .reduce(Long::sum).orElse(ApplicationSettings.TOKEN_MAX_AMOUNT);
    }

    private boolean isValidFee(final long fee) {
        final var assets = assetComboBox.getItems().stream()
                .filter(transferable -> transferable.getName().equals(mainToken.getName()))
                .findAny();
        final var selectedAsset = Optional.ofNullable(assetComboBox.getSelectionModel().getSelectedItem());
        if (fee == 0 || selectedAsset.isEmpty()) return false;

        final var waves = assets.get();
        final var wavesBalance = toLong(waves.getBalance(), waves.getDecimals());

        if (selectedAsset.get().getAssetId().equals(mainToken.getAssetId())) {
            final var assetAmount = toLong(amountTextField.getText(), waves.getDecimals());
            return fee <= wavesBalance - assetAmount - fee;
        }
        return fee <= wavesBalance - fee;
    }

    private <T> List<List<T>> getBatches(List<T> collection, int batchSize) {
        return IntStream.iterate(0, i -> i < collection.size(), i -> i + batchSize)
                .mapToObj(i -> collection.subList(i, Math.min(i + batchSize, collection.size())))
                .collect(Collectors.toUnmodifiableList());
    }

    private void setCalculateFeeText(){
        feeTextField.setText(getMessages().getString("calculating_fee"));
        sendButton.setDisable(true);
    }

    private void disableSendButton() {
        amountTextField.clear();
        feeTextField.clear();
        sendButton.setDisable(true);
    }
}
