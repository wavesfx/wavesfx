package com.wavesfx.wavesfx.gui.dialog;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.logic.AssetDetailsService;
import com.wavesfx.wavesfx.logic.AssetNumeralFormatter;
import com.wavesfx.wavesfx.logic.Waves;
import com.wavesplatform.wavesj.Transaction;
import com.wavesplatform.wavesj.transactions.BurnTransaction;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.wavesfx.wavesfx.utils.ApplicationSettings.NET_REQUEST_DELAY;
import static java.text.MessageFormat.format;

public class ConfirmBurnAssetsController extends DialogController  {

    private static final Logger log = LogManager.getLogger();

    private final List<BurnTransaction> transactions;
    private final double summand;
    private final AssetDetailsService assetDetailsService;

    @FXML private Button cancelButton;
    @FXML private Button sendButton;
    @FXML private TextArea resultTextArea;
    @FXML private TextField assetsTextField;
    @FXML private TextField feeTextField;
    @FXML private TextField recipientTextField;
    @FXML private ProgressBar progressBar;

    public ConfirmBurnAssetsController(RxBus rxBus) {
        super(rxBus);
        transactions = rxBus.getBurnTransactions().getValue();
        summand = (double) 1/transactions.size();
        assetDetailsService = rxBus.getAssetDetailsService().getValue();
    }

    @FXML
	public void initialize() {
        assetsTextField.setText(String.valueOf(transactions.size()));
        feeTextField.setText(transactions.stream()
                .map(Transaction::getFee)
                .reduce(Long::sum)
                .map(aLong -> AssetNumeralFormatter.toReadable(aLong, Waves.DECIMALS))
                .orElse(""));

        JavaFxObservable.actionEventsOf(sendButton)
                .flatMap(ae -> getObservableResult())
                .subscribe(this::updateComponents);
    }

    private void disableButtons() {
        sendButton.setDisable(true);
        cancelButton.setText(getMessages().getString("close"));
    }

    private ObservableSource<String> getObservableResult() {
        progressBar.setVisible(true);
        return Observable.fromIterable(transactions)
                .delay(NET_REQUEST_DELAY, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .map(this::broadcastTransaction)
                .doOnError(Throwable::printStackTrace)
                .retry(1)
                .observeOn(JavaFxScheduler.platform())
                .doOnNext(result -> updateProgressBar());
    }

    private String broadcastTransaction(BurnTransaction transaction) {
        final var assetName = assetDetailsService.fetchAssetDetails(transaction.getAssetId()).getName();
        final var tx = getNodeService().sendTransaction(transaction);
        if (tx.isPresent())
            return format(getMessages().getString("burn_success"), assetName);
        else
            return format(getMessages().getString("burn_fail"), assetName);
    }

    private void updateComponents(String result){
        updateResultText(result);
        disableButtons();
    }

    private void updateProgressBar() {
        final var progress = progressBar.getProgress();
        progressBar.setProgress(progress+summand);
    }

    private void updateResultText(String result) {
        final var oldResultText = resultTextArea.getText();
        final var newResultText = oldResultText + "\n" + result;
        resultTextArea.setText(newResultText);
        resultTextArea.setScrollTop(Double.MAX_VALUE);
    }

}
