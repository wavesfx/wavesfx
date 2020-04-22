package com.wavesfx.wavesfx.gui.transfer;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.assets.AssetCell;
import com.wavesfx.wavesfx.gui.assets.AssetConverterProfile;
import com.wavesfx.wavesfx.gui.dialog.ConfirmTransferController;
import com.wavesfx.wavesfx.gui.style.StyleHandler;
import com.wavesfx.wavesfx.logic.*;
import com.wavesfx.wavesfx.utils.ApplicationSettings;
import com.wavesplatform.wavesj.Transactions;
import com.wavesplatform.wavesj.transactions.TransferTransactionV2;
import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toLong;
import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;
import static com.wavesfx.wavesfx.logic.FormValidator.AMOUNT_PATTERN;
import static com.wavesfx.wavesfx.logic.FormValidator.inputCorrectorObservable;

public class SendController extends TransferTransactionController  {

    private static final Logger log = LogManager.getLogger();

    @FXML private Button sendButton;
    @FXML private ComboBox<Transferable> assetComboBox;
    @FXML private ComboBox<Transferable> feeAssetComboBox;
    @FXML private Label messageLimitLabel;
    @FXML private TextField recipientTextField;
    @FXML private TextField amountTextField;
    @FXML private TextField feeTextField;
    @FXML private TextArea messageTextArea;

    public SendController(final RxBus rxBus) {
        super(rxBus);
    }

    @FXML
	public void initialize() {
        assetComboBox.setConverter(new AssetConverterProfile());
        assetComboBox.setCellFactory(param -> new AssetCell());
        feeAssetComboBox.setConverter(new AssetConverterProfile());

        rxBus.getAssetList().observeOn(JavaFxScheduler.platform())
                .subscribe(this::updateComboBoxes);

        final var amountIsValidObservable = JavaFxObservable.valuesOf(amountTextField.textProperty())
                .map(this::isValidAmount).cache();

        BehaviorSubject<Boolean> observableRecipientIsValid = BehaviorSubject.create();

        JavaFxObservable.valuesOf(recipientTextField.textProperty())
                .doOnNext(s -> observableRecipientIsValid.onNext(false))
                .observeOn(Schedulers.computation())
                .throttleLast(ApplicationSettings.INPUT_REQUEST_DELAY, TimeUnit.MILLISECONDS)
                .map(address -> NodeAddressValidator.isValidAddress(address, nodeSubject.getValue()))
                .retry()
                .subscribe(observableRecipientIsValid::onNext, Throwable::printStackTrace);

        final var messageIsValidObservable = JavaFxObservable.valuesOf(messageTextArea.textProperty())
                .map(this::isValidMessage);

        final var assetFeeIsNotEmptyObservable = JavaFxObservable.nullableValuesOf(feeAssetComboBox.valueProperty())
                .map(transferable -> !feeAssetComboBox.getSelectionModel().isEmpty());

        final var assetIsNotEmptyObservable = JavaFxObservable.nullableValuesOf(assetComboBox.valueProperty())
                .map(transferable -> !assetComboBox.getSelectionModel().isEmpty());

        final var assetAndAmountIsValidObservable = Observable.combineLatest(
                JavaFxObservable.valuesOf(amountTextField.textProperty()),
                JavaFxObservable.valuesOf(assetComboBox.valueProperty()),
                privateKeyAccountSubject,(amount, asset, privateKeyAccount) -> isValidAmount(amount)).cache();

        StyleHandler.setBorderDisposable(observableRecipientIsValid, recipientTextField);
        StyleHandler.setBorderDisposable(assetAndAmountIsValidObservable, amountTextField);
        StyleHandler.setBorderDisposable(messageIsValidObservable, messageTextArea);
        StyleHandler.setBorderDisposable(assetFeeIsNotEmptyObservable, feeAssetComboBox);
        StyleHandler.setBorderDisposable(assetIsNotEmptyObservable, assetComboBox);
        StyleHandler.setBorderDisposable(amountIsValidObservable, amountTextField);

        JavaFxObservable.actionEventsOf(sendButton)
                .subscribe(ae -> sendTransaction());

        inputCorrectorObservable(amountTextField, AMOUNT_PATTERN);

        final var validFormObservable = ConnectableObservable
                .combineLatest(observableRecipientIsValid, messageIsValidObservable, assetAndAmountIsValidObservable,
                        assetFeeIsNotEmptyObservable, assetIsNotEmptyObservable, FormValidator::areValid)
                        .map(Boolean::booleanValue);

        validFormObservable.filter(aBoolean -> !aBoolean)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(aBoolean -> feeTextField.clear());

        final var feeObservable = validFormObservable.observeOn(Schedulers.io()).filter(Boolean::booleanValue)
                .map(b2 -> signTransaction())
                .map(tx -> getNodeService().calculateFee(tx))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .cache()
                .doOnError(Throwable::printStackTrace);

        final var feeIsValidObservable = feeObservable.observeOn(Schedulers.io())
                .map(this::hasSufficientFunds).cache();

        feeObservable.observeOn(JavaFxScheduler.platform())
                .subscribe(aLong -> feeTextField.setText(toReadable(aLong, feeAssetComboBox.getSelectionModel().getSelectedItem().getDecimals())));

        ConnectableObservable.combineLatest(validFormObservable, feeIsValidObservable, FormValidator::areValid)
                .observeOn(Schedulers.io())
                .map(aBoolean -> !aBoolean)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(sendButton::setDisable);
    }

    private void sendTransaction() {
        final var tx = signTransaction();
        rxBus.getTransaction().onNext(tx);
        final var parent = loadParent(FXMLView.CONFIRM_TRANSACTION, new ConfirmTransferController(rxBus));
        createDialog(parent);
    }

    private TransferTransactionV2 signTransaction() {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var asset = assetComboBox.getSelectionModel().getSelectedItem();
        final var assetFee = feeAssetComboBox.getSelectionModel().getSelectedItem();
        final var recipient = recipientTextField.getText();
        return Transactions.makeTransferTx(
                privateKeyAccount,
                AddressConverter.toRawString(recipient, privateKeyAccount.getChainId()),
                toLong(amountTextField.getText(), asset.getDecimals()),
                asset.getAssetId(),
                feeTextField.getText().isEmpty() ? Waves.FEE : toLong(feeTextField.getText(), assetFee.getDecimals()),
                assetFee.getAssetId(),
                messageTextArea.getText()
        );
    }

    private void updateComboBoxes(List<Transferable> assetList) {
        reinitializeComboBox(assetComboBox, assetList);
        final Predicate<Transferable> filter = t ->
                t.getMinFee() != null && t.balanceAsLong() >= t.minFeeAsLong() && t.sponsorBalanceAsLong() >= Waves.FEE;
        final var feeList = assetList.stream().parallel()
                .filter(filter)
                .collect(Collectors.toUnmodifiableList());

        reinitializeComboBox(feeAssetComboBox, feeList);
    }


    private boolean hasSufficientFunds(final long fee) {
        final var selectedAsset = assetComboBox.getSelectionModel().getSelectedItem();
        if (selectedAsset == null)
            return false;
        final var selectedAssetFee = feeAssetComboBox.getSelectionModel().getSelectedItem();
        final var selectedAssetBalance = selectedAsset.balanceAsLong();

        if (!selectedAsset.getAssetId().equals(selectedAssetFee.getAssetId())) {
            return selectedAssetFee.balanceAsLong() >= fee;
        }

        final var amount = toLong(amountTextField.getText(), selectedAsset.getDecimals());
        return selectedAssetBalance >= (amount + fee);
    }
}
