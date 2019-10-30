package com.wavesfx.wavesfx.gui.dashboard;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.ResourceBundle;

public class AliasCell extends ListCell<String> {
    private final Label aliasLabel;
    private final Clipboard clipboard;
    private final ContextMenu contextMenu;
    private final ResourceBundle messages;

    AliasCell(final ResourceBundle resourceBundle) {
        this.aliasLabel = new Label();
        this.clipboard = Clipboard.getSystemClipboard();
        this.contextMenu = new ContextMenu();
        this.messages = resourceBundle;
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
            setGraphic(null);
        } else {
            final var menuItem = new MenuItem(messages.getString("copy_to_clipboard"));
            final var clipBoardContent = new ClipboardContent();
            clipBoardContent.putString(item);
            menuItem.setOnAction(event -> clipboard.setContent(clipBoardContent));
            contextMenu.getItems().setAll(menuItem);
            this.setContextMenu(contextMenu);
            aliasLabel.setText(item);
            setGraphic(aliasLabel);
        }
    }
}
