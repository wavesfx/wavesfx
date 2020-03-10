package com.wavesfx.wavesfx.gui.transfer;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.assets.AssetCell;
import com.wavesfx.wavesfx.gui.assets.AssetConverterProfile;
import com.wavesfx.wavesfx.gui.dialog.ConfirmTransferController;
import com.wavesfx.wavesfx.gui.style.StyleHandler;
import com.wavesfx.wavesfx.logic.AddressValidator;
import com.wavesfx.wavesfx.logic.FormValidator;
import com.wavesfx.wavesfx.logic.Transferable;
import com.wavesplatform.wavesj.Transactions;
import com.wavesplatform.wavesj.Transfer;
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
import java.util.stream.Collectors;

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

        final var recipientsObservable = JavaFxObservable.valuesOf(recipientTextArea.textProperty());

        final var amountIsValidObservable = JavaFxObservable.valuesOf(amountTextField.textProperty())
                .map(this::isValidAmount).cache();

        final var feeIsValidObservable = JavaFxObservable.valuesOf(feeTextField.textProperty())
                .map(this::isValidFee).cache();

        final var recipientIsValidObservable = recipientsObservable
                .map(this::hasValidRecipients);

        final var messageIsValidObservable = JavaFxObservable.valuesOf(messageTextArea.textProperty())
                .map(this::isValidMessage);

        final var assetIsNotEmptyObservable = assetObservable
                .map(transferable -> !assetComboBox.getSelectionModel().isEmpty());

        final var assetAndAmountIsValidObservable = ConnectableObservable.combineLatest(
                JavaFxObservable.valuesOf(amountTextField.textProperty()),
                JavaFxObservable.valuesOf(assetComboBox.valueProperty()),
                privateKeyAccountSubject, (amount, asset, privateKeyAccount) -> isValidAmount(amount)).cache();

        final var transferListObservable = recipientsObservable
                .observeOn(Schedulers.io())
                .filter(this::hasValidRecipients)
                .map(this::getRecipients)
                .map(strings -> fetchListOfTransfers(assetComboBox.getSelectionModel().getSelectedItem(), strings));

        StyleHandler.setBorderDisposable(recipientIsValidObservable, recipientTextArea);
        StyleHandler.setBorderDisposable(assetAndAmountIsValidObservable, amountTextField);
        StyleHandler.setBorderDisposable(messageIsValidObservable, messageTextArea);
        StyleHandler.setBorderDisposable(assetIsNotEmptyObservable, assetComboBox);
        StyleHandler.setBorderDisposable(amountIsValidObservable, amountTextField);
        StyleHandler.setBorderDisposable(feeIsValidObservable, feeTextField);

        JavaFxObservable.actionEventsOf(sendButton)
                .subscribe(ae -> sendTranscation());

        transferListObservable
                .observeOn(Schedulers.computation())
                .map(transfers -> toReadable(calculateTotalAmount(transfers), assetComboBox.getSelectionModel().getSelectedItem().getDecimals()))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(amountTextField::setText);

        ConnectableObservable.combineLatest(transferListObservable, assetObservable, (list, asset) -> list)
                .observeOn(Schedulers.io())
                .filter(transfers -> assetComboBox.getSelectionModel().getSelectedItem() != null)
                .map(transfers -> toReadable(calculateFee(transfers), mainToken.getDecimals()))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(feeTextField::setText);

        Observable.combineLatest(recipientIsValidObservable, messageIsValidObservable, assetAndAmountIsValidObservable,
                assetIsNotEmptyObservable, FormValidator::areValid)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(b -> sendButton.setDisable(!b));

    }

    private void updateComboBoxes(List<Transferable> assetList) {
        reinitializeComboBox(assetComboBox, assetList);
    }

    private boolean hasValidRecipients(final String address) {
        final var selectedAsset = assetComboBox.getSelectionModel().getSelectedItem();
        final String[] splitString = getRecipients(address);
        if (isWellFormed(splitString) || selectedAsset == null) return false;
        if (splitString.length > 100) return false;

        final var transferList = fetchListOfTransfers(selectedAsset, splitString);
        final var listOfInvalidAddresses = getListOfInvalidAddresses(transferList);
        setTooltipMessage(listOfInvalidAddresses);
        return listOfInvalidAddresses.isEmpty();
    }


    private String[] getRecipients(final String address) {
        return address.replaceAll(" ", "").split("\\n");
    }

    private void sendTranscation() {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var selectedAsset = assetComboBox.getSelectionModel().getSelectedItem();
        final var splitString = getRecipients(recipientTextArea.getText());
        final var transferList = fetchListOfTransfers(selectedAsset, splitString);
        final var fee = toLong(feeTextField.getText(), mainToken.getDecimals());
        final var tx = Transactions.makeMassTransferTx(privateKeyAccount, selectedAsset.getAssetId(), transferList,
                fee, messageTextArea.getText());
        rxBus.getTransaction().onNext(tx);

        final var parent = loadParent(FXMLView.CONFIRM_TRANSACTION, new ConfirmTransferController(rxBus));
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
                .map(string -> string.split(","))
                .map(s -> new Transfer(s[0], toLong(s[1], selectedAsset.getDecimals())))
                .collect(Collectors.toUnmodifiableList());
    }

    private boolean isWellFormed(final String[] transfers) {
        return Arrays.stream(transfers)
                .anyMatch(strings -> !FormValidator.isWellFormed(strings, MASS_TX_PATTERN));
    }

    private long calculateFee(final List<Transfer> transferList) {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var assetId = Optional.ofNullable(assetComboBox.getSelectionModel().getSelectedItem().getAssetId()).orElse("");
        final var massTx = Transactions.makeMassTransferTx(privateKeyAccount, assetId, transferList, 0L, messageTextArea.getText());
        return getNodeService().calculateFee(massTx).orElse(0L);
    }

    private boolean isValidFee(final String fee) {
        final var assets = assetComboBox.getItems().stream()
                .filter(transferable -> transferable.getName().equals(mainToken.getName()))
                .findAny();
        final var selectedAsset = Optional.ofNullable(assetComboBox.getSelectionModel().getSelectedItem());
        if (fee.isEmpty() || selectedAsset.isEmpty()) return false;

        final var waves = assets.get();
        final var wavesBalance = toLong(waves.getBalance(), waves.getDecimals());
        final var feeAsLong = toLong(fee, waves.getDecimals());

        if (selectedAsset.get().getAssetId().equals(mainToken.getAssetId())) {
            final var assetAmount = toLong(amountTextField.getText(), waves.getDecimals());
            return feeAsLong <= wavesBalance - -assetAmount - feeAsLong;
        }
        return feeAsLong <= wavesBalance - feeAsLong;
    }

}
