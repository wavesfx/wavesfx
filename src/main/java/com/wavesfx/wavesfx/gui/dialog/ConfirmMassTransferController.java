package com.wavesfx.wavesfx.gui.dialog;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.logic.AssetDetailsService;
import com.wavesfx.wavesfx.logic.Waves;
import com.wavesplatform.wavesj.Transaction;
import com.wavesplatform.wavesj.Transfer;
import com.wavesplatform.wavesj.transactions.MassTransferTransaction;
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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;
import static com.wavesfx.wavesfx.utils.ApplicationSettings.NET_REQUEST_DELAY;
import static java.text.MessageFormat.format;

public class ConfirmMassTransferController extends DialogController  {

    private static final Logger log = LogManager.getLogger();

    private final List<MassTransferTransaction> transactions;
    private final double summand;
    private final AssetDetailsService assetDetailsService;

    @FXML private Button cancelButton;
    @FXML private Button sendButton;
    @FXML private TextArea resultTextArea;
    @FXML private TextField assetTextField;
    @FXML private TextField feeTextField;
    @FXML private TextField transactionsTextField;
    @FXML private TextField totalAmountTextField;
    @FXML private ProgressBar progressBar;

    public ConfirmMassTransferController(RxBus rxBus) {
        super(rxBus);
        transactions = rxBus.getMassTransferTransactions().getValue();
        summand = (double) 1/transactions.size();
        assetDetailsService = rxBus.getAssetDetailsService().getValue();
    }

    @FXML
    public void initialize() {
        final var assetDetails = assetDetailsService.fetchAssetDetails(transactions.get(0).getAssetId());
        assetTextField.setText(assetDetails.getName());

        final var numberOfTransactions = transactions.stream()
                .map(MassTransferTransaction::getTransfers)
                .map(Collection::size)
                .reduce(Integer::sum).orElse(0);
        transactionsTextField.setText(numberOfTransactions.toString());

        feeTextField.setText(transactions.stream()
                .map(Transaction::getFee)
                .reduce(Long::sum)
                .map(aLong -> toReadable(aLong, Waves.DECIMALS))
                .orElse(""));

        final var totalAmount = transactions.stream()
                .map(MassTransferTransaction::getTransfers)
                .flatMap(Collection::stream)
                .map(Transfer::getAmount)
                .reduce(Long::sum).orElse(0L);
        totalAmountTextField.setText(toReadable(totalAmount, assetDetails.getDecimals()));

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

    private String broadcastTransaction(MassTransferTransaction transaction) {
        final var assetName = assetDetailsService.fetchAssetDetails(transaction.getAssetId()).getName();
        final var tx = getNodeService().sendTransaction(transaction);
        final Function<String, String> statusMessage = (message) ->
                format(getMessages().getString(message), assetName) + " (" + transaction.getId() + ")";
        if (tx.isPresent())
            return statusMessage.apply("transaction_success");
        else
            return statusMessage.apply("transaction_failed");
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
