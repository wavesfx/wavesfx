package com.wavesfx.wavesfx.gui.dialog;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.style.StyleHandler;
import com.wavesfx.wavesfx.logic.Transferable;
import com.wavesplatform.wavesj.AssetDetails;
import com.wavesplatform.wavesj.Transactions;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toLong;
import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;
import static com.wavesfx.wavesfx.logic.FormValidator.AMOUNT_PATTERN;
import static com.wavesfx.wavesfx.logic.FormValidator.inputCorrectorObservable;
import static com.wavesfx.wavesfx.utils.ApplicationSettings.TOKEN_MAX_AMOUNT;

public class ReissueTokenController extends DialogController  {

    private static final Logger log = LogManager.getLogger();

    private final Transferable transferable;
    private final AssetDetails assetDetails;

    @FXML private AnchorPane rootPane;
    @FXML private TextField tokenTextField;
    @FXML private TextField quantityTextField;
    @FXML private TextField additionalAmountTextField;
    @FXML private TextField feeTextField;
    @FXML private Button cancelButton;
    @FXML private Button sendButton;
    @FXML private ComboBox<String> reissuableComboBox;

    public ReissueTokenController(final RxBus rxBus) {
        super(rxBus);
        transferable = rxBus.getTransferable().getValue();
        assetDetails = rxBus.getAssetDetails().getValue();
    }

    @FXML
	public void initialize() {
        sendButton.setOnAction(event -> sendTransaction());

        initializeTextFields(assetDetails);
        initializeComboBox();

        inputCorrectorObservable(additionalAmountTextField, AMOUNT_PATTERN);

        final var isValidAmountObservable = JavaFxObservable.valuesOf(additionalAmountTextField.textProperty())
                .observeOn(Schedulers.io())
                .map(this::isValidAdditionalAmount);

        final var isValidFeeOvservable = JavaFxObservable.valuesOf(feeTextField.textProperty())
                .observeOn(Schedulers.io())
                .map(this::isValidFee);

        StyleHandler.setBorderDisposable(isValidAmountObservable, additionalAmountTextField);

        isValidAmountObservable.observeOn(Schedulers.io())
                .filter(Boolean::booleanValue)
                .map(aBoolean -> calculateFee())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(feeTextField::setText);

        ConnectableObservable.combineLatest(isValidAmountObservable, isValidFeeOvservable, (b1, b2) -> !(b1 && b2))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(sendButton::setDisable);
    }

    private void initializeTextFields(AssetDetails assetDetails) {
        tokenTextField.setText(assetDetails.getName());
        quantityTextField.setText(toReadable(assetDetails.getQuantity(), assetDetails.getDecimals()));
    }

    private boolean isValidFee(String fee) {
        final var privateKeyAccount = getPrivateKeyAccount();
        if (fee.isEmpty())
            return false;

        final var balance = getNodeService().fetchBalanceDetails(privateKeyAccount.getAddress());
        return balance.filter(balanceDetails -> toLong(fee, mainToken.getDecimals()) <= balanceDetails.getAvailable()).isPresent();
    }

    private String calculateFee() {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var reissuable = reissuableComboBox.selectionModelProperty().get().getSelectedIndex() == 0;
        final var reissueTx = Transactions.makeReissueTx(privateKeyAccount, privateKeyAccount.getChainId(),
                transferable.getAssetId(), toLong(additionalAmountTextField.getText(), transferable.getDecimals()),
                reissuable, 0L);
        rxBus.getTransaction().onNext(reissueTx);

        final var fee = getNodeService().calculateFee(reissueTx);
        if (fee.isPresent())
            return toReadable(fee.get(), mainToken.getDecimals());
        return "";
    }

    private void sendTransaction() {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var reissuable = reissuableComboBox.selectionModelProperty().get().getSelectedIndex() == 0;
        final var reissueTx = Transactions.makeReissueTx(privateKeyAccount, privateKeyAccount.getChainId(),
                transferable.getAssetId(), toLong(additionalAmountTextField.getText(), transferable.getDecimals()),
                reissuable, toLong(feeTextField.getText(), mainToken.getDecimals()));
        rxBus.getTransaction().onNext(reissueTx);

        closeDialog();
        final var parent = loadParent(FXMLView.CONFIRM_TRANSACTION, new ConfirmTransferController(rxBus));
        createDialog(parent);
    }

    private boolean isValidAdditionalAmount(final String amount) {
        if (!amount.isEmpty()) {
            final var quantity = assetDetails.getQuantity();
            final var decimals = assetDetails.getDecimals();
            try {
                final var amountAsLong = toLong(amount, decimals);
                final var totalAmount = quantity + amountAsLong;
                return amountAsLong > 0 && totalAmount < TOKEN_MAX_AMOUNT;
            } catch (NumberFormatException | ArithmeticException e) {
                log.error("Number conversion error");
                return false;
            }
        }
        return false;
    }

    private void initializeComboBox() {
        reissuableComboBox.getItems().addAll(getMessages().getString("reissuable"), getMessages().getString("not_reissuable"));
        reissuableComboBox.selectionModelProperty().get().select(0);
    }
}
