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
import javafx.scene.control.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toLong;
import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;
import static com.wavesfx.wavesfx.logic.FormValidator.*;

public class BurnController extends DialogController  {

    private static final Logger log = LogManager.getLogger();

    private final Transferable transaction;
    private final AssetDetails assetDetails;

    @FXML private TextField tokenTextField;
    @FXML private TextField balanceTextField;
    @FXML private TextField amountTextField;
    @FXML private TextField feeTextField;
    @FXML private Button cancelButton;
    @FXML private Button sendButton;

    public BurnController(final RxBus rxBus) {
        super(rxBus);
        transaction = rxBus.getTransferable().getValue();
        assetDetails = rxBus.getAssetDetails().getValue();
    }

    @FXML
	public void initialize() {
        sendButton.setOnAction(event -> sendTransaction());

        initializeTextFields(assetDetails);

        inputCorrectorObservable(amountTextField, AMOUNT_PATTERN);

        final var isValidAmountObservable = JavaFxObservable.valuesOf(amountTextField.textProperty())
                .observeOn(Schedulers.io())
                .map(this::isValidAmount);

        final var isValidFeeObservable = JavaFxObservable.valuesOf(feeTextField.textProperty())
                .observeOn(Schedulers.io())
                .map(this::isValidFee);

        StyleHandler.setBorderDisposable(isValidAmountObservable, amountTextField);

        isValidAmountObservable
                .filter(Boolean::booleanValue)
                .map(aBoolean -> calculateFee())
                .filter(Optional::isPresent).map(Optional::get)
                .map(fee -> toReadable(fee, mainToken.getDecimals()))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(feeTextField::setText);

        ConnectableObservable.combineLatest(isValidAmountObservable, isValidFeeObservable, (b1, b2) -> !(b1 && b2))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(sendButton::setDisable);
    }

    private void initializeTextFields(AssetDetails assetDetails) {
        tokenTextField.setText(assetDetails.getName());
        balanceTextField.setText(transaction.getBalance());
    }

    private boolean isValidAmount(String amount) {
        if (amount.isEmpty() || !isWellFormed(amount, AMOUNT_PATTERN))
            return false;
        try {
            final var amountAsLong = toLong(amount, transaction.getDecimals());
            return amountAsLong <= transaction.balanceAsLong() && amountAsLong >= 1;
        } catch (NumberFormatException e) {
            log.error("Decimal conversion error");
            return false;
        }
    }

    private boolean isValidFee(String fee) {
        final var address = getPrivateKeyAccount().getAddress();
        if (fee.isEmpty())
            return false;
        final var balance = getNodeService().fetchBalanceDetails(address);
        return balance.filter(balanceDetails -> toLong(fee, mainToken.getDecimals()) <= balanceDetails.getAvailable()).isPresent();
    }

    private Optional<Long> calculateFee() {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var burnTx = Transactions.makeBurnTx(privateKeyAccount, privateKeyAccount.getChainId(),
                transaction.getAssetId(), toLong(amountTextField.getText(), transaction.getDecimals()), 0L);
        return getNodeService().calculateFee(burnTx);
    }

    private void sendTransaction() {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var burnTx = Transactions.makeBurnTx(privateKeyAccount, privateKeyAccount.getChainId(),
                transaction.getAssetId(), toLong(amountTextField.getText(), transaction.getDecimals()),
                toLong(feeTextField.getText(), mainToken.getDecimals()));
        rxBus.getTransaction().onNext(burnTx);
        closeDialog();
        final var parent = loadParent(FXMLView.CONFIRM_TRANSACTION, new ConfirmTransferController(rxBus));
        createDialog(parent);
    }

}
