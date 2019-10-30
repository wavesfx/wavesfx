package com.wavesfx.wavesfx.gui.dialog;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.utils.ApplicationSettings;
import com.wavesplatform.wavesj.AssetDetails;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;

public class AssetInfoController extends DialogController  {
    private final AssetDetails assetDetails;

    @FXML private AnchorPane rootPane;
    @FXML private TextField issuerTextField;
    @FXML private TextField idTextField;
    @FXML private TextField nameTextField;
    @FXML private TextField totalAmountTextField;
    @FXML private TextField decimalsTextField;
    @FXML private TextField typeTextField;
    @FXML private TextField issueDateTextField;
    @FXML private TextArea descriptionTextField;
    @FXML private Button closeButton;

    public AssetInfoController(final RxBus rxBus) {
        super(rxBus);
        assetDetails = rxBus.getAssetDetails().getValue();
    }

    @FXML
	public void initialize() {
        initializeTextFields(assetDetails);
    }

    private void initializeTextFields(AssetDetails assetDetails) {
        final var dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(assetDetails.getIssueTimestamp()), ZoneId.systemDefault());
        issuerTextField.setText(assetDetails.getIssuer());
        idTextField.setText(assetDetails.getAssetId());
        nameTextField.setText(assetDetails.getName());
        totalAmountTextField.setText(toReadable(assetDetails.getQuantity(), assetDetails.getDecimals()));
        decimalsTextField.setText(String.valueOf(assetDetails.getDecimals()));
        typeTextField.setText(assetDetails.getReissuable() ? getMessages().getString("reissuable") : getMessages().getString("not_reissuable"));
        issueDateTextField.setText(ApplicationSettings.FORMATTER.format(dateTime));
        descriptionTextField.setText(assetDetails.getDescription());
    }
}
