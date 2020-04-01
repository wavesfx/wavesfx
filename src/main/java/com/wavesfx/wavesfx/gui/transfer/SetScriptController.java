package com.wavesfx.wavesfx.gui.transfer;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.MasterController;
import com.wavesfx.wavesfx.gui.dialog.ConfirmTransferController;
import com.wavesfx.wavesfx.gui.style.StyleHandler;
import com.wavesfx.wavesfx.logic.AssetNumeralFormatter;
import com.wavesplatform.wavesj.Transaction;
import com.wavesplatform.wavesj.Transactions;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;
import static com.wavesfx.wavesfx.utils.ApplicationSettings.INPUT_REQUEST_DELAY;
import static com.wavesfx.wavesfx.utils.ApplicationSettings.MAIN_TOKEN;

public class SetScriptController extends MasterController {

    private static final Logger log = LogManager.getLogger();

    @FXML private TextArea scriptTextArea;
    @FXML private Label scriptWarningLabel;
    @FXML private TextField feeTextField;
    @FXML private Button sendButton;

    public SetScriptController(RxBus rxBus) {
        super(rxBus);
    }

    @FXML
	public void initialize(){
        BehaviorSubject<Optional<String>> optionalBehaviorSubject = BehaviorSubject.create();
        JavaFxObservable.actionEventsOf(sendButton)
                .observeOn(Schedulers.io())
                .subscribe(ae -> sendTransaction());

        JavaFxObservable.valuesOf(scriptTextArea.textProperty())
                .doOnNext(s -> optionalBehaviorSubject.onNext(Optional.empty()))
                .observeOn(Schedulers.io())
                .throttleLast(INPUT_REQUEST_DELAY, TimeUnit.MILLISECONDS)
                .filter(Objects::nonNull)
                .map(s -> getNodeService().compileScript(s))
                .subscribe(optionalBehaviorSubject::onNext, Throwable::printStackTrace);

        final var feeObservable = optionalBehaviorSubject.observeOn(Schedulers.io())
                .filter(Optional::isPresent)
                .map(optionalScript -> signTransaction(optionalScript.get(), 1000000L))
                .map(tx -> getNodeService().calculateFee(tx))
                .retry()
                .filter(Optional::isPresent).map(Optional::get)
                .cache()
                .observeOn(JavaFxScheduler.platform())
                .doOnNext(aLong -> feeTextField.setText(toReadable(aLong, MAIN_TOKEN.getDecimals())));

        final var hasSufficientFundsObservable = ConnectableObservable.combineLatest(feeObservable, rxBus.getPrivateKeyAccount(),
                (fee, privateKeyAccount) -> fee).observeOn(Schedulers.io())
                .map(this::accountHasSufficientFunds).cache();

        optionalBehaviorSubject
                .subscribe(optional -> StyleHandler.setBorder(optional.isPresent(), scriptTextArea), Throwable::printStackTrace);

        StyleHandler.setBorderDisposable(hasSufficientFundsObservable, feeTextField);

        final var formObservable = ConnectableObservable.combineLatest(
                optionalBehaviorSubject, hasSufficientFundsObservable, (b1, b2) -> b1.isPresent() && b2);

        formObservable.observeOn(JavaFxScheduler.platform())
                .map(aBoolean -> !aBoolean)
                .subscribe(sendButton::setDisable);
    }

    private Transaction signTransaction(final String script, final long fee) {
        final var privateKeyAccount = getPrivateKeyAccount();
        return Transactions.makeScriptTx(privateKeyAccount, script, privateKeyAccount.getChainId(), fee);
    }

    private boolean accountHasSufficientFunds(final long fee) {
        final var address = getPrivateKeyAccount().getAddress();
        final var balance = getNodeService().fetchBalance(address).orElse(0L);
        return fee <= balance;
    }

    private void sendTransaction() {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var compiledScript = getNodeService().compileScript(scriptTextArea.getText());
        final var fee = AssetNumeralFormatter.toLong(feeTextField.getText(), MAIN_TOKEN.getDecimals());
        final var tx = Transactions.makeScriptTx(privateKeyAccount, compiledScript.orElse(""), privateKeyAccount.getChainId(), fee);
        final var parent = loadParent(FXMLView.CONFIRM_TRANSACTION, new ConfirmTransferController(rxBus));
        rxBus.getTransaction().onNext(tx);
        compiledScript.ifPresent(s -> createDialog(parent));
    }
}
