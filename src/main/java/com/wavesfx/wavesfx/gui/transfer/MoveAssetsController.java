package com.wavesfx.wavesfx.gui.transfer;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.MasterController;
import com.wavesfx.wavesfx.gui.dialog.ConfirmMoveAssetsController;
import com.wavesfx.wavesfx.gui.style.StyleHandler;
import com.wavesfx.wavesfx.logic.*;
import com.wavesfx.wavesfx.utils.ApplicationSettings;
import com.wavesplatform.wavesj.PrivateKeyAccount;
import com.wavesplatform.wavesj.Transaction;
import com.wavesplatform.wavesj.Transactions;
import com.wavesplatform.wavesj.transactions.TransferTransaction;
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

public class MoveAssetsController extends MasterController {

    private final ObservableList<Transferable> assetList;
    private final BehaviorSubject<List<TransferTransaction>> transactionsSubject;

    @FXML private AnchorPane rootPane;
    @FXML private Button sendButton;
    @FXML private Button selectAllButton;
    @FXML private Button unselectAllButton;
    @FXML private ListView<Transferable> assetListView;
    @FXML private TextField recipientTextField;
    @FXML private TextField feeTextField;

    public MoveAssetsController(RxBus rxBus) {
        super(rxBus);
        assetList = FXCollections.observableArrayList();
        transactionsSubject = BehaviorSubject.create();
    }

    @FXML
	public void initialize(){
        initializeListView();
        rxBus.getAssetList().observeOn(JavaFxScheduler.platform())
                .subscribe(this::populateList);

        final var recipientIsValidObservable = JavaFxObservable.valuesOf(recipientTextField.textProperty())
                .observeOn(Schedulers.io())
                .throttleLast(ApplicationSettings.INPUT_REQUEST_DELAY, TimeUnit.MILLISECONDS)
                .map(address -> NodeAddressValidator.isValidAddress(address, nodeSubject.getValue()))
                .retry();

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

        StyleHandler.setBorderDisposable(recipientIsValidObservable, recipientTextField);
        StyleHandler.setBorderDisposable(selectionIsNotEmptyObservable, assetListView);

        final var validFormObservable = Observable.combineLatest(
                recipientIsValidObservable, selectionIsNotEmptyObservable, FormValidator::areValid);

        final var selectedTransactionsObservable = validFormObservable
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
                .map(transferTransactionV2s -> transferTransactionV2s.stream().map(TransferTransaction::getFee).reduce(Long::sum))
                .filter(Optional::isPresent)
                .map(Optional::get);

        final var hasSufficientFundsObservable = estimatedFeeObservable
                .map(estimatedFee -> estimatedFee <= getAccountBalance(assetList).orElse(0L))
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
        rxBus.getTransactions().onNext(fetchFinalTransactionList(transactionsSubject.getValue()));
        final var parent = loadParent(FXMLView.CONFIRM_MOVE_ASSETS, new ConfirmMoveAssetsController(rxBus));
        createDialog(parent);
        assetListView.getSelectionModel().clearSelection();
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
        final var selectedItems = assetListView.getSelectionModel().getSelectedIndices().stream()
                .mapToInt(value -> value).toArray();
        assetList.clear();
        assetList.setAll(list);
        if(selectedItems.length != 0) {
            Arrays.stream(selectedItems).forEach(assetListView.getSelectionModel()::select);
        }
    }

    private Observable<List<TransferTransaction>> calculateEstimatedFeeObservable() {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var selectedAssets = assetListView.getSelectionModel().getSelectedItems();

        return Observable.just(selectedAssets)
                .subscribeOn(Schedulers.io())
                .map(transferable -> createDummyTransaction(privateKeyAccount, transferable))
                .map(this::updateTransactionFees)
                .retry().doOnError(Throwable::printStackTrace);
    }

    private List<TransferTransaction> createDummyTransaction(PrivateKeyAccount privateKeyAccount, List<Transferable> transferableList) {
        final var recipient = recipientTextField.getText();
        final var address = AddressConverter.toRawString(recipient, privateKeyAccount.getChainId());
        return transferableList.stream()
                .map(transferable -> Transactions.makeTransferTx(privateKeyAccount, address, transferable.balanceAsLong(),
                        transferable.getAssetId(), Waves.FEE, Waves.ASSET_ID, ""))
                .collect(Collectors.toUnmodifiableList());
    }

    private List<TransferTransaction> updateTransactionFees (List<TransferTransaction> transferTransactions){
        return transferTransactions.stream().parallel()
                .map(tx -> updateTransactionFee(tx, getNodeService().calculateFee(tx).orElse(tx.getFee())))
                .collect(Collectors.toUnmodifiableList());
    }

    private TransferTransaction updateTransactionFee(final TransferTransaction tx, final long fee) {
        final var privateKeyAccount = getPrivateKeyAccount();
        return Transactions.makeTransferTx(privateKeyAccount, tx.getRecipient(), tx.getAmount(), tx.getAssetId(), fee,
                tx.getFeeAssetId(), "");
    }

    private Optional<Long> getAccountBalance(final List<Transferable> transferableList){
        return transferableList.stream().parallel()
                .findFirst()
                .filter(tr -> tr.getAssetId().equals(Waves.ASSET_ID) || tr.getAssetId() == null)
                .map(Transferable::balanceAsLong);
    }

    private List<TransferTransaction> fetchFinalTransactionList(final List<TransferTransaction> transactions) {
        final var privateKeyAccount = getPrivateKeyAccount();
        final var fee = transactions.stream().map(Transaction::getFee).reduce(Long::sum).orElse(0L);
        final var wavesTx = transactions.stream().filter(tx -> tx.getAssetId() == null).findAny();

        if (wavesTx.isEmpty()){
            return transactions;
        } else {
            final var oldTx = wavesTx.get();
            final var newTx = Transactions.makeTransferTx(privateKeyAccount, oldTx.getRecipient(), oldTx.getAmount() - fee,
                    oldTx.getAssetId(), oldTx.getFee(), oldTx.getFeeAssetId(), "");
            transactions.remove(oldTx);
            transactions.add(newTx);
            return transactions;
        }

    }
}
