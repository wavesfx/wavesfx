package com.wavesfx.wavesfx.gui.dialog;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.utils.ApplicationSettings;
import com.wavesplatform.wavesj.AssetDetails;
import com.wavesplatform.wavesj.Transaction;
import com.wavesplatform.wavesj.Transfer;
import com.wavesplatform.wavesj.transactions.*;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;

public class ConfirmTransferController extends DialogController  {
    private static final Logger log = LogManager.getLogger();

    private final Transaction transaction;

    @FXML private AnchorPane rootPane;
    @FXML private Button sendButton;
    @FXML private Button cancelButton;
    @FXML private VBox dialogVBox;
    @FXML private GridPane amountGridPane;
    @FXML private GridPane recipientGridPane;
    @FXML private GridPane assetGridPane;
    @FXML private TextField txTypeTextField;
    @FXML private TextField assetTextField;
    @FXML private TextField amountTextField;
    @FXML private TextField feeTextField;
    @FXML private TextField txIdTextField;
    @FXML private TextField timestampTextField;
    @FXML private TextField recipientTextField;
    @FXML private TextArea resultTextArea;

    public ConfirmTransferController(RxBus rxBus) {
        super(rxBus);
        transaction = rxBus.getTransaction().getValue();
    }

    @FXML
	public void initialize() {
        initializeTextFields();
        JavaFxObservable.actionEventsOf(sendButton)
                .observeOn(Schedulers.io())
                .map(actionEvent -> broadcastTransaction(transaction))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::updateComponents);
    }

    private void initializeTextFields() {
        if (transaction instanceof TransferTransaction) {
            generateTransferTransactionInfo();
        } else if (transaction instanceof MassTransferTransaction) {
            generateMassTransferTransactionInfo();
        } else if (transaction instanceof LeaseTransaction) {
            generateLeaseTransactionInfo();
        } else if (transaction instanceof LeaseCancelTransaction) {
            generateLeaseCancelTransactionInfo();
        } else if (transaction instanceof BurnTransaction) {
            generateBurnTransactionInfo();
        } else if (transaction instanceof IssueTransaction) {
            generateIssueTransactionInfo();
        } else if (transaction instanceof ReissueTransaction) {
            generateReissueTransactionInfo();
        } else if (transaction instanceof AliasTransaction){
            generateAliasTransactionInfo();
        } else if (transaction instanceof SetScriptTransaction){
            generateSetScriptTransactionInfo();
        }
        timestampTextField.setText(formatTimestamp(transaction.getTimestamp()));
    }

    private void generateSetScriptTransactionInfo() {
        dialogVBox.getChildren().removeAll(recipientGridPane, amountGridPane, assetGridPane);
        final var setScriptTransaction = (SetScriptTransaction) transaction;
        txTypeTextField.setText(getMessages().getString("set_script"));
        feeTextField.setText(toReadable(setScriptTransaction.getFee(), mainToken.getDecimals()));
        txIdTextField.setText(setScriptTransaction.getId().toString());
    }

    private void generateAliasTransactionInfo() {
        dialogVBox.getChildren().removeAll(recipientGridPane, amountGridPane, assetGridPane);
        final var aliasTransaction = (AliasTransaction) transaction;
        txTypeTextField.setText(getMessages().getString("create_alias"));
        feeTextField.setText(toReadable(aliasTransaction.getFee(), mainToken.getDecimals()));
        txIdTextField.setText(aliasTransaction.getId().toString());
    }

    private void generateReissueTransactionInfo() {
        dialogVBox.getChildren().remove(recipientGridPane);
        final var reissueTransaction = (ReissueTransaction) transaction;
        final var assetDetails = getAssetDetails(reissueTransaction.getAssetId());
        assetTextField.setText(assetDetails.getName());
        txTypeTextField.setText(getMessages().getString("reissue_token"));
        amountTextField.setText(toReadable(reissueTransaction.getQuantity(), assetDetails.getDecimals()));
        feeTextField.setText(toReadable(reissueTransaction.getFee(), mainToken.getDecimals()));
        txIdTextField.setText(reissueTransaction.getId().toString());
    }

    private void generateIssueTransactionInfo() {
        dialogVBox.getChildren().remove(recipientGridPane);
        final var issueTx = (IssueTransaction) transaction;
        assetTextField.setText(issueTx.getName());
        txTypeTextField.setText(getMessages().getString("issue_token"));
        amountTextField.setText(String.valueOf(issueTx.getQuantity()));
        feeTextField.setText(toReadable(issueTx.getFee(), mainToken.getDecimals()));
        txIdTextField.setText(issueTx.getId().toString());
    }

    private void generateBurnTransactionInfo() {
        dialogVBox.getChildren().remove(recipientGridPane);
        final var transferTx = (BurnTransaction) transaction;
        final var assetDetails = getAssetDetails(transferTx.getAssetId());
        assetTextField.setText(assetDetails.getName());
        txTypeTextField.setText(getMessages().getString("burn_asset"));
        amountTextField.setText(toReadable(transferTx.getAmount(), assetDetails.getDecimals()));
        final var fee = toReadable(transaction.getFee(), mainToken.getDecimals());
        feeTextField.setText(fee + " " + mainToken.getName());
        txIdTextField.setText(transaction.getId().toString());
    }

    private void generateLeaseCancelTransactionInfo() {
        dialogVBox.getChildren().remove(recipientGridPane);
        dialogVBox.getChildren().remove(amountGridPane);
        final var leaseTx = (LeaseCancelTransaction) transaction;
        assetTextField.setText(mainToken.getName());
        txTypeTextField.setText(getMessages().getString("cancel_lease"));
        feeTextField.setText(toReadable(transaction.getFee(), mainToken.getDecimals()));
        txIdTextField.setText(transaction.getId().toString());
    }

    private void generateLeaseTransactionInfo() {
        final var leaseTx = (LeaseTransaction) transaction;
        assetTextField.setText(mainToken.getName());
        recipientTextField.setText(leaseTx.getRecipient());
        txTypeTextField.setText(getMessages().getString("lease"));
        amountTextField.setText(toReadable(leaseTx.getAmount(), mainToken.getDecimals()));
        feeTextField.setText(toReadable(transaction.getFee(), mainToken.getDecimals()));
        txIdTextField.setText(transaction.getId().toString());
    }

    private void generateMassTransferTransactionInfo() {
        dialogVBox.getChildren().remove(recipientGridPane);
        final var assetDetails = getAssetDetails(((MassTransferTransaction) transaction).getAssetId());
        final var massTransferTx = (MassTransferTransaction) transaction;
        assetTextField.setText(assetDetails.getName());
        txTypeTextField.setText(getMessages().getString("mass_transfer"));
        final var amount = calcTotalAmount(massTransferTx.getTransfers());
        amountTextField.setText(toReadable(amount, assetDetails.getDecimals()));
        feeTextField.setText(toReadable(transaction.getFee(), assetDetails.getDecimals()));
        txIdTextField.setText(transaction.getId().toString());
    }

    private void generateTransferTransactionInfo() {
        final var transferTx = (TransferTransaction) transaction;
        final var assetDetails = getAssetDetails(transferTx.getAssetId());
        final var feeAssetDetails = getAssetDetails(transferTx.getFeeAssetId());
        recipientTextField.setText(transferTx.getRecipient());
        assetTextField.setText(assetDetails.getName());
        txTypeTextField.setText(getMessages().getString("transfer"));
        amountTextField.setText(toReadable(transferTx.getAmount(), assetDetails.getDecimals()));
        final var fee = toReadable(transferTx.getFee(), feeAssetDetails.getDecimals());
        feeTextField.setText(fee + " " + feeAssetDetails.getName());
        txIdTextField.setText(transferTx.getId().toString());
    }

    private String formatTimestamp(long timestamp) {
        final var dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return ApplicationSettings.FORMATTER.format(dateTime);
    }

    private AssetDetails getAssetDetails(String assetId) {
        final var node = getNodeService().getNode();
        if (assetId == null)
            return mainToken;
        try {
            return node.getAssetDetails(assetId);
        } catch (IOException e) {
            log.error("Broadcasting Transaction failed", e);
            return null;
        }
    }

    private String broadcastTransaction(Transaction transaction) {
        final var node = getNodeService().getNode();
        try {
            node.send(transaction);
            return getMessages().getString("transaction_success");
        } catch (IOException e) {
            return getMessages().getString("transaction_failed") + " \n"+ e.getLocalizedMessage();
        }
    }

    private void updateComponents(String result) {
        disableButtons();
        resultTextArea.setText(result);
    }

    private Long calcTotalAmount(final Collection<Transfer> transferList) {
        return transferList.stream()
                .map(Transfer::getAmount)
                .reduce(0L, Long::sum);
    }

    private void disableButtons() {
        sendButton.setDisable(true);
        cancelButton.setText(getMessages().getString("close"));
    }
}
