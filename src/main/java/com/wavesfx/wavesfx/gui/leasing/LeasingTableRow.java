package com.wavesfx.wavesfx.gui.leasing;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.CustomFXMLLoader;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.dialog.ConfirmTransferController;
import com.wavesfx.wavesfx.gui.dialog.DialogWindow;
import com.wavesfx.wavesfx.logic.TransactionDetails;
import com.wavesfx.wavesfx.logic.Waves;
import com.wavesplatform.wavesj.Transactions;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;

public class LeasingTableRow extends TableRow<TransactionDetails> {
    private final MenuItem cancelLeasingMenuItem;
    private final ContextMenu contextMenu;
    private final RxBus rxBus;
    private final ResourceBundle messages;
    private final Stage stage;

    LeasingTableRow(RxBus rxBus, Stage stage) {
        this.messages = rxBus.getResourceBundle().getValue();
        this.stage = stage;
        this.rxBus = rxBus;
        contextMenu = new ContextMenu();
        cancelLeasingMenuItem = new MenuItem(messages.getString("cancel_lease"));

        cancelLeasingMenuItem.setOnAction(event -> {
            final var privateKeyAccount = rxBus.getPrivateKeyAccount().getValue();
            Optional.ofNullable(this.getTableView().getSelectionModel().getSelectedItem())
                    .ifPresent(transferable -> Observable.just(transferable)
                            .map(transactionDetails -> Transactions.makeLeaseCancelTx(privateKeyAccount,
                                    privateKeyAccount.getChainId(), transactionDetails.getTransactionId(), Waves.FEE))
                            .doOnNext(rxBus.getTransaction()::onNext)
                            .observeOn(JavaFxScheduler.platform())
                            .subscribe(transferable1 -> createDialog()));
        });
    }

    @Override
    protected void updateItem(TransactionDetails item, boolean empty) {
        super.updateItem(item, empty);
        if (this.getItem() != null){
            final var privateKeyAccount = rxBus.getPrivateKeyAccount().getValue();
            final var senderPublicKey = this.getItem().getTransaction().getSenderPublicKey().getPublicKey();
            if (Arrays.equals(senderPublicKey, privateKeyAccount.getPublicKey())) {
                contextMenu.getItems().setAll(cancelLeasingMenuItem);
                this.setContextMenu(contextMenu);
            } else
                contextMenu.getItems().clear();
        }
    }

    private void createDialog() {
        final var controller = new ConfirmTransferController(rxBus);
        final var parent = CustomFXMLLoader.loadParent(FXMLView.CONFIRM_TRANSACTION, controller, messages);
        new DialogWindow(rxBus, stage, parent).show();
    }
}
