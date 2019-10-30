package com.wavesfx.wavesfx.gui.transactions;

import com.wavesfx.wavesfx.logic.TransactionDetails;
import com.wavesplatform.wavesj.transactions.WithRecipient;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.Optional;
import java.util.ResourceBundle;

public class TransactionTableRow extends TableRow<TransactionDetails> {
    private final MenuItem copyTxIdItem;
    private final MenuItem copyRecipientItem;
    private final MenuItem copySenderItem;
    private final ContextMenu contextMenu;
    private final Clipboard clipboard;

    TransactionTableRow(ResourceBundle messages, Clipboard clipboard) {
        copyTxIdItem = new MenuItem(messages.getString("copy_tx_id"));
        copyRecipientItem = new MenuItem(messages.getString("copy_recipient_address"));
        copySenderItem = new MenuItem(messages.getString("copy_sender_address"));
        contextMenu = new ContextMenu();
        this.clipboard = clipboard;

        copyTxIdItem.setOnAction(event -> {
            Optional.ofNullable(getItem().getTransactionId())
                    .ifPresent(tx -> {
                        final var cbContent = new ClipboardContent();
                        cbContent.putString(tx);
                        clipboard.setContent(cbContent);
            });
        });

        copyRecipientItem.setOnAction(event -> {
            Optional.ofNullable(getItem().getTransaction())
                    .ifPresent(tx -> {
                        final var cbContent = new ClipboardContent();
                        final var txWithRecipient = (WithRecipient) tx;
                        cbContent.putString(txWithRecipient.getRecipient());
                        clipboard.setContent(cbContent);
            });
        });

        copySenderItem.setOnAction(event -> {
            Optional.ofNullable(getItem().getTransaction())
                    .ifPresent(tx -> {
                        final var cbContent = new ClipboardContent();
                        cbContent.putString(tx.getSenderPublicKey().getAddress());
                        clipboard.setContent(cbContent);
            });
        });
    }

    @Override
    protected void updateItem(TransactionDetails item, boolean empty) {
        super.updateItem(item, empty);
        if (this.getItem() != null){
            if (item.getTransaction() instanceof WithRecipient){
                final var tx = item.getTransaction();
                final var address = item.getAddress();
                final var senderAddress = item.getTransaction().getSenderPublicKey().getAddress();
                if (address.equals(senderAddress))
                    contextMenu.getItems().setAll(copyTxIdItem, copyRecipientItem);
                else
                    contextMenu.getItems().setAll(copyTxIdItem, copySenderItem);
            } else {
                contextMenu.getItems().setAll(copyTxIdItem);
            }
            this.setContextMenu(contextMenu);
        }
    }
}
