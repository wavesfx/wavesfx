package com.wavesfx.wavesfx.gui.tokenIssue;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.MasterController;
import com.wavesfx.wavesfx.gui.dialog.ConfirmTransferController;
import com.wavesfx.wavesfx.gui.style.StyleHandler;
import com.wavesfx.wavesfx.logic.FormValidator;
import com.wavesfx.wavesfx.utils.ApplicationSettings;
import com.wavesplatform.wavesj.Transactions;
import com.wavesplatform.wavesj.transactions.IssueTransactionV2;
import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toLong;
import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;
import static com.wavesfx.wavesfx.logic.FormValidator.*;

public class TokenIssueController extends MasterController {

    private static final Logger log = LogManager.getLogger();
    private static final int MAX_SIZE_NAME = 16;
    private static final int MAX_SIZE_DESCRIPTION = 948;

    @FXML private TextField assetNameTextField;
    @FXML private TextField amountTextField;
    @FXML private TextField feeTextField;
    @FXML private ComboBox<String> reissuableComboBox;
    @FXML private TextArea descriptionTextField;
    @FXML private TextArea scriptTextField;
    @FXML private Button generateButton;
    @FXML private Label nameLimitLabel;
    @FXML private Label descriptionLimitLabel;
    @FXML private Slider decimalSlider;

    public TokenIssueController(final RxBus rxBus) {
        super(rxBus);
    }

    @FXML
	public void initialize() {
        final var assetNameIsValidObservable = JavaFxObservable.valuesOf(assetNameTextField.textProperty())
                .observeOn(Schedulers.io())
                .map(this::isValidName);

        final var descriptionIsValidObservable = JavaFxObservable.valuesOf(descriptionTextField.textProperty())
                .observeOn(Schedulers.io())
                .map(this::isValidDescription);

        final var amountIsValidObservable = ConnectableObservable.combineLatest(
                JavaFxObservable.valuesOf(amountTextField.textProperty()).throttleLast(ApplicationSettings.INPUT_REQUEST_DELAY, TimeUnit.MILLISECONDS),
                JavaFxObservable.valuesOf(decimalSlider.valueProperty()),
                this::isValidTokenAmount)
                .onErrorReturn(throwable -> false);

        final var scriptIsValidObservable = JavaFxObservable.valuesOf(scriptTextField.textProperty())
                .observeOn(Schedulers.io())
                .map(this::isValidScript);

        inputCorrectorObservable(amountTextField, MAX_LONG_PATTERN);

        StyleHandler.setBorderDisposable(assetNameIsValidObservable, assetNameTextField);
        StyleHandler.setBorderDisposable(descriptionIsValidObservable, descriptionTextField);
        StyleHandler.setBorderDisposable(scriptIsValidObservable, scriptTextField);
        StyleHandler.setBorderDisposable(amountIsValidObservable, amountTextField);

        final var formIsValidObservable = ConnectableObservable.combineLatest(assetNameIsValidObservable, descriptionIsValidObservable,
                amountIsValidObservable, scriptIsValidObservable, FormValidator::areValid);

        final var hasSufficientFundsObservable = formIsValidObservable.observeOn(Schedulers.io())
                .filter(b -> b)
                .map(this::hasSufficientFunds)
                .doOnError(Throwable::printStackTrace);

        StyleHandler.setBorderDisposable(hasSufficientFundsObservable, feeTextField);

        ConnectableObservable.combineLatest(formIsValidObservable, hasSufficientFundsObservable, FormValidator::areValid)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(b -> generateButton.setDisable(!b));

        JavaFxObservable.actionEventsOf(generateButton)
                .subscribe(actionEvent -> sendTransaction());

        initializeComboBox();

    }

    private void initializeComboBox() {
        reissuableComboBox.getItems().addAll(getMessages().getString("reissuable"), getMessages().getString("not_reissuable"));
        reissuableComboBox.selectionModelProperty().get().select(0);
    }

    private boolean hasSufficientFunds(boolean b) {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var node = getNode();
        final var tx = signTransaction();
        try {
            final var balance = node.getBalanceDetails(privateKeyAccount.getAddress()).getAvailable();
            final var fee = node.calculateFee(tx).getFeeAmount();
            Observable.just(fee).subscribeOn(JavaFxScheduler.platform())
                    .subscribe(calculatedFee -> feeTextField.setText(toReadable(fee, mainToken.getDecimals())));
            return fee <= balance;
        } catch (IOException e) {
            log.error("Error fetching funds", e);
        }
        return false;
    }

    private boolean isValidName(final String string) {
        final var byteLength = string.getBytes().length;
        final var template = "/" + MAX_SIZE_NAME;
        Observable.just(byteLength + template).subscribeOn(JavaFxScheduler.platform())
                .subscribe(nameLimitLabel::setText);

        if (!isWellFormed(string, TOKEN_NAME_PATTERN))
            return false;
        return byteLength <= MAX_SIZE_NAME;
    }

    private boolean isValidDescription(final String string) {
        final int size = MAX_SIZE_DESCRIPTION;
        final var byteLength = string.getBytes().length;
        final var template = "/" + size;
        Observable.just(byteLength + template).subscribeOn(JavaFxScheduler.platform())
                .subscribe(descriptionLimitLabel::setText);

        return byteLength < size;
    }

    private boolean isValidTokenAmount(final String amount, final Number precision) {
        try {
            if (!amount.isEmpty()) {
                final var totalAmount = Long.parseLong(amount) * (Math.pow(10, precision.intValue()));
                return totalAmount > 0 && totalAmount < ApplicationSettings.TOKEN_MAX_AMOUNT;
            }
            return false;
        } catch (NumberFormatException e) {
            log.error("Decimal conversion error");
            return false;
        }
    }

    private boolean isValidScript(String script) {
        final var node = getNode();
        if (script.isEmpty())
            return true;
        try {
            node.compileScript(script);
            return true;
        } catch (IOException e) {
            log.error("Compiling script failed");
            return false;
        }
    }

    private Optional<String> getCompiledScript(String script) {
        final var node = getNode();
        try {
            return Optional.of(node.compileScript(script));
        } catch (IOException e) {
            log.error("Compiling script failed");
            return Optional.empty();
        }
    }

    private IssueTransactionV2 signTransaction() {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var assetName = assetNameTextField.getText();
        final var description = descriptionTextField.getText();
        final var amount = Long.valueOf(amountTextField.getText());
        final var decimals = (byte) decimalSlider.getValue();
        final var reissuable = reissuableComboBox.selectionModelProperty().get().getSelectedIndex() == 0;
        final var script = scriptTextField.getText().isEmpty() ? null : getCompiledScript(scriptTextField.getText()).orElse(null);

        try {
            return Transactions.makeIssueTx(privateKeyAccount, privateKeyAccount.getChainId(), assetName, description,
                    amount, decimals, reissuable, script,
                    feeTextField.getText().isEmpty() ? 100000000L : toLong(feeTextField.getText(), mainToken.getDecimals())
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendTransaction() {
        try {
            final var tx = signTransaction();
            rxBus.getTransaction().onNext(tx);
            createDialog(loadParent(FXMLView.CONFIRM_TRANSACTION, new ConfirmTransferController(rxBus)));
        } catch (Exception e) {
            log.error("Creating dialog failed", e);
        }
    }

}
