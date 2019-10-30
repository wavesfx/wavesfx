package com.wavesfx.wavesfx.gui.dashboard;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.MasterController;
import com.wavesfx.wavesfx.gui.dialog.ConfirmTransferController;
import com.wavesfx.wavesfx.gui.dialog.DialogWindow;
import com.wavesfx.wavesfx.gui.style.StyleHandler;
import com.wavesfx.wavesfx.logic.AssetNumeralFormatter;
import com.wavesfx.wavesfx.logic.Waves;
import com.wavesfx.wavesfx.utils.ApplicationSettings;
import com.wavesplatform.wavesj.Alias;
import com.wavesplatform.wavesj.BalanceDetails;
import com.wavesplatform.wavesj.BlockHeader;
import com.wavesplatform.wavesj.Transactions;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;

public class DashboardController extends MasterController {

    private static final Logger log = LogManager.getLogger();

    private final ObservableList<BlockHeader> blockHeaderList;
    private final ObservableList<String> aliasList;
    private final BehaviorSubject<Long> emitter;

    private final BehaviorSubject<Long> feeSubject;

    @FXML private Button createAliasButton;
    @FXML private Label regularBalanceLabel;
    @FXML private Label availableBalanceLabel;
    @FXML private Label heightLabel;
    @FXML private Label versionLabel;
    @FXML private Label feeLabel;
    @FXML private ListView<String> aliasListView;
    @FXML private TextField aliasTextField;
    @FXML private VBox blockHeaderVbox;

    public DashboardController(final RxBus rxBus) {
        super(rxBus);
        blockHeaderList = FXCollections.observableArrayList();
        aliasList = FXCollections.observableArrayList();
        emitter = rxBus.getEmitter();
        feeSubject = BehaviorSubject.create();
    }

    @FXML
	public void initialize() {
        aliasListView.setItems(aliasList);

        JavaFxObservable.valuesOf(aliasTextField.textProperty())
                .doOnNext(s -> {
                    createAliasButton.setDisable(true);
                    StyleHandler.setBorder(false, aliasTextField);
                })
                .observeOn(Schedulers.computation())
                .throttleLast(ApplicationSettings.INPUT_REQUEST_DELAY, TimeUnit.MILLISECONDS)
                .map(this::isValidAlias)
                .observeOn(JavaFxScheduler.platform())
                .doOnNext(b -> StyleHandler.setBorder(b, aliasTextField))
                .subscribe(aBoolean -> createAliasButton.setDisable(!aBoolean), Throwable::printStackTrace);

        emitter.observeOn(Schedulers.io())
                .map(aLong -> getNodeService().fetchNodeversion())
                .filter(Optional::isPresent).map(Optional::get)
                .retry()
                .observeOn(JavaFxScheduler.platform())
                .subscribe(versionLabel::setText, Throwable::printStackTrace);

        emitter.observeOn(Schedulers.io())
                .map(aLong -> getNodeService().fetchHeight())
                .filter(Optional::isPresent).map(Optional::get)
                .map(String::valueOf)
                .retry()
                .observeOn(JavaFxScheduler.platform())
                .subscribe(heightLabel::setText, Throwable::printStackTrace);

        emitter.observeOn(Schedulers.io())
                .map(aLong -> getPrivateKeyAccount())
                .map(pKeyAccount -> Transactions.makeAliasTx(pKeyAccount, "xxxx", pKeyAccount.getChainId(), Waves.FEE))
                .map(getNodeService()::calculateFee)
                .filter(Optional::isPresent).map(Optional::get)
                .retry()
                .doOnNext(feeSubject::onNext)
                .map(calculatedFee -> AssetNumeralFormatter.toReadable(calculatedFee, mainToken.getDecimals()))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(feeLabel::setText, Throwable::printStackTrace);

        Observable.merge(emitter, rxBus.getPrivateKeyAccount())
                .observeOn(Schedulers.io())
                .map(aLong -> getPrivateKeyAccount().getAddress())
                .map(getNodeService()::fetchBalanceDetails)
                .retry()
                .filter(Optional::isPresent).map(Optional::get)
                .doOnNext(rxBus.getBalanceDetails()::onNext)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::setBalanceLabels, Throwable::printStackTrace);

        aliasListView.setCellFactory(param -> new AliasCell(getMessages()));

        emitter.observeOn(Schedulers.io())
                .map(aLong -> getNodeService().fetchHeight())
                .retry()
                .filter(Optional::isPresent).map(Optional::get)
                .map(height -> getNodeService().fetchBlockHeader(height >= 6 ? height - 5 : 1, height))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .observeOn(JavaFxScheduler.platform())
                .map(this::blockList)
                .subscribe(blockHeaderVbox.getChildren()::setAll, Throwable::printStackTrace);

        Observable.merge(emitter, rxBus.getPrivateKeyAccount())
                .observeOn(Schedulers.io())
                .map(aLong -> getPrivateKeyAccount().getAddress())
                .map(getNodeService()::fetchAliasByAddress)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .retry()
                .map(strings -> strings.stream().map(s -> Alias.fromString(s).getName()).sorted().collect(Collectors.toList()))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::updateAliasList, Throwable::printStackTrace);

        JavaFxObservable.actionEventsOf(createAliasButton)
                .subscribe(actionEvent -> sendTransaction());

        emitter.onNext(0L);
    }

    private void setBalanceLabels(BalanceDetails balanceDetails) {
        regularBalanceLabel.setText(toReadable(balanceDetails.getRegular(), mainToken.getDecimals()));
        availableBalanceLabel.setText(toReadable(balanceDetails.getAvailable(), mainToken.getDecimals()));
    }

    private void updateAliasList(List<String> newAliasList){
        final var selection = aliasListView.getSelectionModel().getSelectedIndex();
        aliasList.setAll(newAliasList);
        aliasListView.getSelectionModel().select(selection);
    }

    private List<HBox> blockList(List<BlockHeader> blockHeaders){
        Collections.reverse(blockHeaders);
        blockHeaderVbox.getChildren().clear();
        final AtomicInteger counter = new AtomicInteger();
        final Function<AtomicInteger, Integer> blockCount = aInt -> aInt.intValue() < 2 ? aInt.incrementAndGet() : 0;

        return blockHeaders.stream()
                .map(blockHeader -> new BlockInfoBox(blockHeader, getMessages(), blockCount.apply(counter)))
                .collect(Collectors.toList());
    }

    private boolean isValidAlias(final String alias) {
        if (alias.isEmpty() || alias.length()<4 || alias.length()>30) {
            return false;
        } else {
            return getNodeService().fetchAddressByAlias(alias).isEmpty();
        }
    }

    private void sendTransaction() {
        final var alias = aliasTextField.getText().toLowerCase();
        final var privateKeyAccount = getPrivateKeyAccount();
        final var fee = feeSubject.getValue();
        final var tx = Transactions.makeAliasTx(privateKeyAccount, alias, privateKeyAccount.getChainId(), fee);

        rxBus.getTransaction().onNext(tx);
        final var parent = loadParent(FXMLView.CONFIRM_TRANSACTION, new ConfirmTransferController(rxBus));
        new DialogWindow(rxBus, getStage(createAliasButton), parent).show();

        aliasTextField.clear();
    }

}
