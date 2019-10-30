package com.wavesfx.wavesfx.gui.dialog;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.logic.AssetDetailsService;
import com.wavesfx.wavesfx.logic.TransactionDetails;
import com.wavesfx.wavesfx.utils.ApplicationSettings;
import com.wavesplatform.wavesj.Transaction;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TransactionInfoController extends DialogController  {
    private static final Logger log = LogManager.getLogger();

    private final TransactionDetails transactionDetails;
    private final Transaction transaction;
    private final AssetDetailsService assetDetailsService;

    @FXML private AnchorPane rootPane;
    @FXML private Button closeButton;
    @FXML private GridPane amountGridPane;
    @FXML private GridPane dateGridPane;
    @FXML private GridPane feeGridPane;
    @FXML private GridPane idGridPane;
    @FXML private GridPane recipientGridPane;
    @FXML private GridPane senderGridPane;
    @FXML private GridPane typeGridPane;
    @FXML private TextField amountText;
    @FXML private TextField dateText;
    @FXML private TextField feeText;
    @FXML private TextField idText;
    @FXML private TextField recipientText;
    @FXML private TextField senderText;
    @FXML private TextField typeText;
    @FXML private VBox dialogVbox;

    public TransactionInfoController(final RxBus rxBus) {
        super(rxBus);
        transactionDetails = rxBus.getTransactionDetails().getValue();
        transaction = transactionDetails.getTransaction();
        assetDetailsService = transactionDetails.getAssetDetailsService();
    }

    @FXML
	public void initialize() {
        idText.setText(transactionDetails.getTransactionId());
        dateText.setText(transactionDetails.getDateTime());
    }

    private String formatTimestamp(long timestamp) {
        final var dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return ApplicationSettings.FORMATTER.format(dateTime);
    }

}
