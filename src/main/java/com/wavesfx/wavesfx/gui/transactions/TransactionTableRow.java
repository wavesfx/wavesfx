package com.wavesfx.wavesfx.gui.transactions;

import com.wavesfx.wavesfx.logic.TransactionDetails;
import com.wavesplatform.wavesj.transactions.WithRecipient;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;

public class TransactionTableRow extends TableRow<TransactionDetails> {
    private final MenuItem copyTxIdItem;
    private final MenuItem copyRecipientItem;
    private final MenuItem copySenderItem;
    private final MenuItem copyTxInfo;
    private final ContextMenu contextMenu;
    private final Clipboard clipboard;

    TransactionTableRow(ResourceBundle messages, Clipboard clipboard) {
        copyTxIdItem = new MenuItem(messages.getString("copy_tx_id"));
        copyRecipientItem = new MenuItem(messages.getString("copy_recipient_address"));
        copySenderItem = new MenuItem(messages.getString("copy_sender_address"));
        copyTxInfo = new MenuItem(messages.getString("copy_tx_info"));
        contextMenu = new ContextMenu();
        this.clipboard = clipboard;

        copyTxInfo.setOnAction(event -> Optional.ofNullable(this.getItem())
                    .ifPresent(txDetails -> {
                        final var txSummary = txDetails.getTransactionSummary();
                        final var info = createSummary(txSummary.getHeadline(), txSummary.getInfo(),
                                txDetails.getTransactionId(), txDetails.getDateTime());
                        final var clipboardContent = new ClipboardContent();
                        clipboardContent.putString(info);
                        clipboard.setContent(clipboardContent);
                    })
        );

        copyTxIdItem.setOnAction(event -> Optional.ofNullable(getItem().getTransactionId())
                    .ifPresent(tx -> {
                        final var cbContent = new ClipboardContent();
                        cbContent.putString(tx);
                        clipboard.setContent(cbContent);
            }));

        copyRecipientItem.setOnAction(event -> Optional.ofNullable(getItem().getTransaction())
                    .ifPresent(tx -> {
                        final var cbContent = new ClipboardContent();
                        final var txWithRecipient = (WithRecipient) tx;
                        cbContent.putString(txWithRecipient.getRecipient());
                        clipboard.setContent(cbContent);
            }));

        copySenderItem.setOnAction(event -> Optional.ofNullable(getItem().getTransaction())
                .ifPresent(tx -> {
                    final var cbContent = new ClipboardContent();
                    cbContent.putString(tx.getSenderPublicKey().getAddress());
                    clipboard.setContent(cbContent);
        }));
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
                    contextMenu.getItems().setAll(copyTxIdItem, copyRecipientItem, copyTxInfo);
                else
                    contextMenu.getItems().setAll(copyTxIdItem, copySenderItem, copyTxInfo);
            } else {
                contextMenu.getItems().setAll(copyTxIdItem, copyTxInfo);
            }
            this.setContextMenu(contextMenu);
        }
    }

    private String createSummary(String... strings) {
        return Arrays.stream(strings).reduce((s1 , s2) -> s1 + "\n" + s2).orElse("");
    }
}
