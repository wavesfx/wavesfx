package com.wavesfx.wavesfx.gui.sign;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.MasterController;
import com.wavesfx.wavesfx.gui.style.StyleHandler;
import com.wavesfx.wavesfx.logic.AddressValidator;
import com.wavesfx.wavesfx.logic.FormValidator;
import com.wavesfx.wavesfx.logic.Profile;
import com.wavesfx.wavesfx.logic.TxBroadcastGenerator;
import com.wavesplatform.wavesj.Transaction;
import com.wavesplatform.wavesj.TransactionWithProofs;
import com.wavesplatform.wavesj.Transactions;
import com.wavesplatform.wavesj.transactions.LeaseCancelTransactionV2;
import com.wavesplatform.wavesj.transactions.LeaseTransactionV2;
import com.wavesplatform.wavesj.transactions.TransferTransactionV2;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.subjects.BehaviorSubject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.IntStream;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toLong;
import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;

public class SignController extends MasterController {

    private static final Logger log = LogManager.getLogger();

    private final BehaviorSubject<Profile> profileBehaviorSubject;
    private final String cancel_lease;
    private final String lease;
    private final String transfer;

    @FXML private Button sendButton;
    @FXML private ComboBox<Integer> decimalsComboBox;
    @FXML private ComboBox<String> txTypeComboBox;
    @FXML private Label messageLimitLabel;
    @FXML private Label recipientLabel;
    @FXML private TextArea messageTextArea;
    @FXML private TextField amountTextField;
    @FXML private TextField assetIdTextField;
    @FXML private TextField feeTextField;
    @FXML private TextField recipientAndTxIdTextField;

    public SignController(RxBus rxBus) {
        super(rxBus);
        profileBehaviorSubject = rxBus.getProfile();
        cancel_lease = getMessages().getString("cancel_lease");
        lease = getMessages().getString("lease");
        transfer = getMessages().getString("transfer");
    }

    @FXML
    public void initialize() {
        initializeControls();

        final var txTypeValueObservable = JavaFxObservable.valuesOf(txTypeComboBox.valueProperty());

        txTypeValueObservable.subscribe(this::toggleControls);

        final var isValidRecipientOrTxIdObservable = Observable.combineLatest(
                txTypeValueObservable,
                JavaFxObservable.valuesOf(recipientAndTxIdTextField.textProperty()), (txType, recipient) -> recipient)
                .map(this::isValidRecipientOrTxId);

        final var isValidAmountObservable = Observable.combineLatest(
                txTypeValueObservable,
                JavaFxObservable.valuesOf(amountTextField.textProperty()), (txType, amount) -> amount)
                .map(this::isValidAmount);

        final var isValidFeeObservable = JavaFxObservable.valuesOf(feeTextField.textProperty())
                .map(this::isValidFee);

        StyleHandler.setBorderDisposable(isValidRecipientOrTxIdObservable, recipientAndTxIdTextField);
        StyleHandler.setBorderDisposable(isValidAmountObservable, amountTextField);
        StyleHandler.setBorderDisposable(isValidFeeObservable, feeTextField);

        final var validFormObservable = Observable.combineLatest(
                isValidRecipientOrTxIdObservable,
                isValidAmountObservable,
                isValidFeeObservable, FormValidator::areValid)
                .map(Boolean::booleanValue);

        validFormObservable.map(bool -> !bool)
                .subscribe(sendButton::setDisable);

        sendButton.setOnAction(ae -> {
            createHtml();
        });
    }

    private boolean isValidRecipientOrTxId(String recipient) {
        if (txTypeComboBox.getValue().equals(cancel_lease))
            return !recipient.isEmpty();
        else
            return AddressValidator.validateAddress(recipient, getNode().getChainId());
    }

    private void initializeControls() {
        txTypeComboBox.getItems().addAll(transfer, lease, cancel_lease);
        txTypeComboBox.getSelectionModel().select(transfer);
        assetIdTextField.setPromptText(mainToken.getAssetId());

        final var minFee = toReadable(mainToken.getMinSponsoredAssetFee(), mainToken.getDecimals());
        feeTextField.setText(minFee);

        IntStream.rangeClosed(0, mainToken.getDecimals()).forEach(decimalsComboBox.getItems()::add);
        setMainToken();
    }

    private void toggleControls(String txType) {
        if (txType.equals(lease)) {
            setMainToken();
            recipientLabel.setText(getMessages().getString("recipient"));
            amountTextField.setDisable(false);
            multiSetDisable(true, assetIdTextField, decimalsComboBox, messageTextArea);
        } else if (txType.equals(cancel_lease)) {
            setMainToken();
            recipientLabel.setText(getMessages().getString("transaction_id"));
            multiSetDisable(true, assetIdTextField, decimalsComboBox, messageTextArea, amountTextField);
        } else {
            recipientLabel.setText(getMessages().getString("recipient"));
            multiSetDisable(false, assetIdTextField, decimalsComboBox, messageTextArea, amountTextField);
        }
    }

    private void multiSetDisable(boolean setDisable, Control... controls){
        Arrays.stream(controls).forEach(control -> control.setDisable(setDisable));
    }

    private void setMainToken() {
        assetIdTextField.setText(mainToken.getAssetId());
        decimalsComboBox.getSelectionModel().select(mainToken.getDecimals());
    }

    private boolean isValidAmount(String amount) {
        if (txTypeComboBox.getValue().equals(cancel_lease))
            return true;
        else
            return !amount.isEmpty() && FormValidator.isWellFormed(amount, FormValidator.AMOUNT_PATTERN);
    }

    private boolean isValidFee(String fee) {
        return isValidAmount(fee) && toLong(fee, mainToken.getDecimals()) >= mainToken.getMinSponsoredAssetFee();
    }

    private TransferTransactionV2 generateTransferTransaction() {
        return Transactions.makeTransferTx(
                getPrivateKeyAccount(),
                recipientAndTxIdTextField.getText(),
                toLong(amountTextField.getText(), decimalsComboBox.getValue()),
                assetIdTextField.getText(),
                toLong(feeTextField.getText(), mainToken.getDecimals()),
                mainToken.getAssetId(),
                "");
    }

    private LeaseTransactionV2 generateLeaseTransaction() {
        return Transactions.makeLeaseTx(
                getPrivateKeyAccount(),
                recipientAndTxIdTextField.getText(),
                toLong(amountTextField.getText(), mainToken.getDecimals()),
                toLong(feeTextField.getText(), mainToken.getDecimals()),
                System.currentTimeMillis());
    }

    private LeaseCancelTransactionV2 generateLeaseCancelTransaction() {
        return Transactions.makeLeaseCancelTx(
                getPrivateKeyAccount(),
                getPrivateKeyAccount().getChainId(),
                recipientAndTxIdTextField.getText(),
                toLong(feeTextField.getText(), mainToken.getDecimals()),
                System.currentTimeMillis()
        );
    }

    private File createFileChooser() {
        final var fileChooser = new FileChooser();
        final var extFilter = new FileChooser.ExtensionFilter("HTML file (*.html)", "*.html");
        fileChooser.setTitle("Save File");
        fileChooser.setInitialFileName("broadcastTransaction");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        return fileChooser.showSaveDialog(getStage());
    }

    private void writeToFile(File file, String json) {
        if (file != null) {
            try {
                final var printWriter = new PrintWriter(file);
                printWriter.print(json);
                printWriter.close();
            } catch (IOException ex) {
                log.error("Could not save file", ex);
            }
        }
    }

    private void createHtml() {
        final var txType = txTypeComboBox.getValue();

        if (txType.equals(transfer)){
            saveHtml(generateTransferTransaction());
        } else if (txType.equals(cancel_lease)){
            saveHtml(generateLeaseCancelTransaction());
        } else {
            saveHtml(generateLeaseTransaction());
        }
    }

    private void saveHtml (TransactionWithProofs<? extends Transaction> transactionWithProofs) {
        final var html = generateTxBroadcastGenerator(transactionWithProofs);
        final var file = createFileChooser();
        writeToFile(file, html.getHtml());
    }

    private TxBroadcastGenerator generateTxBroadcastGenerator(TransactionWithProofs<? extends Transaction> tx) {
        return new TxBroadcastGenerator(tx, mainToken.getAssetId(), profileBehaviorSubject.getValue().getNode());
    }
}
