package com.wavesfx.wavesfx.gui.transfer;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.MasterController;
import com.wavesfx.wavesfx.gui.dialog.ConfirmBurnAssetsController;
import com.wavesfx.wavesfx.gui.style.StyleHandler;
import com.wavesfx.wavesfx.logic.AssetNumeralFormatter;
import com.wavesfx.wavesfx.logic.Transferable;
import com.wavesfx.wavesfx.logic.Waves;
import com.wavesfx.wavesfx.utils.ApplicationSettings;
import com.wavesplatform.wavesj.PrivateKeyAccount;
import com.wavesplatform.wavesj.Transactions;
import com.wavesplatform.wavesj.transactions.BurnTransaction;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BurnAssetsController extends MasterController {

    private final ObservableList<Transferable> assetList;
    private final BehaviorSubject<List<BurnTransaction>> transactionsSubject;

    @FXML private AnchorPane rootPane;
    @FXML private Button sendButton;
    @FXML private Button selectAllButton;
    @FXML private Button unselectAllButton;
    @FXML private ListView<Transferable> assetListView;
    @FXML private TextField feeTextField;

    public BurnAssetsController(RxBus rxBus) {
        super(rxBus);
        assetList = FXCollections.observableArrayList();
        transactionsSubject = BehaviorSubject.create();
    }

    @FXML
    public void initialize(){
        initializeListView();
        rxBus.getAssetList().observeOn(JavaFxScheduler.platform())
                .subscribe(this::populateList);

        final var unselectAllActionEventsObservable = JavaFxObservable.actionEventsOf(unselectAllButton);
        final var selectAllActionEventsObservable = JavaFxObservable.actionEventsOf(selectAllButton);
        selectAllActionEventsObservable.subscribe(ae -> assetListView.getSelectionModel().selectAll());
        unselectAllActionEventsObservable.subscribe(ae -> assetListView.getSelectionModel().clearSelection());
        final var selectionEventObservable = Observable.merge(selectAllActionEventsObservable, unselectAllActionEventsObservable);

        final var assetListViewEventsObservable = Observable.merge(
                privateKeyAccountSubject,
                selectionEventObservable,
                JavaFxObservable.eventsOf(assetListView, MouseEvent.MOUSE_CLICKED),
                JavaFxObservable.eventsOf(assetListView, KeyEvent.ANY));

        final var selectionIsNotEmptyObservable = assetListViewEventsObservable.observeOn(Schedulers.io())
                .map(ae -> !assetListView.getSelectionModel().getSelectedItems().isEmpty());

        StyleHandler.setBorderDisposable(selectionIsNotEmptyObservable, assetListView);

        final var selectedTransactionsObservable = selectionIsNotEmptyObservable
                .observeOn(JavaFxScheduler.platform())
                .doOnNext(s -> disableSendButton())
                .filter(Boolean::booleanValue)
                .doOnNext(b -> setCalculateFeeText())
                .observeOn(Schedulers.io())
                .switchMap(s -> Observable.just(s).delay(ApplicationSettings.INPUT_REQUEST_DELAY, TimeUnit.MILLISECONDS))
                .flatMap(aBoolean -> calculateEstimatedFeeObservable())
                .cache()
                .retry()
                .doOnNext(transactionsSubject::onNext);

        final var estimatedFeeObservable = selectedTransactionsObservable
                .map(burnTransactions -> burnTransactions.stream().map(BurnTransaction::getFee).reduce(Long::sum))
                .filter(Optional::isPresent)
                .map(Optional::get);

        final var hasSufficientFundsObservable = estimatedFeeObservable
                .map(estimatedFee -> estimatedFee <= getAccountBalance(rxBus.getAssetList().getValue()).orElse(0L))
                .cache();

        StyleHandler.setBorderDisposable(hasSufficientFundsObservable, feeTextField);

        estimatedFeeObservable.observeOn(JavaFxScheduler.platform())
                .map(aLong -> AssetNumeralFormatter.toReadable(aLong, Waves.DECIMALS))
                .subscribe(feeTextField::setText, Throwable::printStackTrace);

        hasSufficientFundsObservable
                .observeOn(JavaFxScheduler.platform())
                .subscribe(aBoolean -> sendButton.setDisable(!aBoolean), Throwable::printStackTrace);

        JavaFxObservable.actionEventsOf(sendButton)
                .subscribe(actionEvent -> createConfirmTransactionDialog(), Throwable::printStackTrace);
    }

    private void createConfirmTransactionDialog() {
        rxBus.getBurnTransactions().onNext(transactionsSubject.getValue());
        final var parent = loadParent(FXMLView.CONFIRM_BURN_ASSETS, new ConfirmBurnAssetsController(rxBus));
        createDialog(parent);
    }

    private void disableSendButton() {
        feeTextField.clear();
        sendButton.setDisable(true);
    }

    private void setCalculateFeeText(){
        feeTextField.setText(getMessages().getString("calculating_fee"));
    }

    private void initializeListView() {
        StyleHandler.setBorder(false, assetListView);
        assetListView.setCellFactory(param -> new AssetChoiceCell(assetListView));
        assetListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        assetListView.setItems(assetList);
    }

    private void populateList(List<Transferable> list) {
        final var filteredList = list.stream().filter(transferable -> !transferable.getAssetId().equals(Waves.ASSET_ID))
                .collect(Collectors.toUnmodifiableList());
        final var selectedItems = assetListView.getSelectionModel().getSelectedIndices().stream()
                .mapToInt(value -> value).toArray();
        assetList.clear();
        assetList.setAll(filteredList);
        if(selectedItems.length != 0) {
            Arrays.stream(selectedItems).forEach(assetListView.getSelectionModel()::select);
        }
    }

    private Observable<List<BurnTransaction>> calculateEstimatedFeeObservable() {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var selectedAssets = assetListView.getSelectionModel().getSelectedItems();

        return Observable.just(selectedAssets)
                .subscribeOn(Schedulers.io())
                .map(transferable -> createDummyTransaction(privateKeyAccount, transferable))
                .map(this::updateTransactionFees)
                .retry().doOnError(Throwable::printStackTrace);
    }

    private List<BurnTransaction> createDummyTransaction(PrivateKeyAccount privateKeyAccount, List<Transferable> transferableList) {
        return transferableList.stream()
                .map(transferable -> Transactions.makeBurnTx(privateKeyAccount, privateKeyAccount.getChainId(), transferable.getAssetId(),
                        transferable.balanceAsLong(), Waves.FEE*3))
                .collect(Collectors.toUnmodifiableList());
    }

    private List<BurnTransaction> updateTransactionFees (List<BurnTransaction> burnTransactionList){
        return burnTransactionList.stream().parallel()
                .map(tx -> updateTransactionFee(tx, getNodeService().calculateFee(tx).orElse(tx.getFee())))
                .collect(Collectors.toUnmodifiableList());
    }

    private BurnTransaction updateTransactionFee(final BurnTransaction tx, final long fee) {
        final var privateKeyAccount = getPrivateKeyAccount();
        return Transactions.makeBurnTx(privateKeyAccount, privateKeyAccount.getChainId(), tx.getAssetId(), tx.getAmount(), fee);
    }

    private Optional<Long> getAccountBalance(final List<Transferable> transferableList){
        return transferableList.stream().parallel()
                .findFirst()
                .filter(tr -> tr.getAssetId().equals(Waves.ASSET_ID) || tr.getAssetId() == null)
                .map(Transferable::balanceAsLong);
    }
}
