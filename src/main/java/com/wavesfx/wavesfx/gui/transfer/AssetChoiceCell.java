package com.wavesfx.wavesfx.gui.transfer;

import com.wavesfx.wavesfx.logic.Transferable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AssetChoiceCell extends ListCell<Transferable> {
    private final VBox vBox;
    private final HBox hBox;
    private final Label assetName;
    private final Label assetId;

    public AssetChoiceCell(final ListView<?> listView) {
        vBox = new VBox();
        hBox = new HBox();
        assetName = new Label();
        assetId = new Label();
        vBox.setAlignment(Pos.CENTER_LEFT);
        assetName.getStyleClass().add("custom-cell-headline-label");
        assetId.getStyleClass().add("custom-cell-subline-label");
        vBox.getStyleClass().add("custom-cell-vbox");
        vBox.getChildren().addAll(assetName, assetId);
        hBox.getChildren().addAll(vBox);


        this.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (this.isEmpty()) {
                return ;
            }

            final var index = this.getIndex() ;
            if (listView.getSelectionModel().getSelectedIndices().contains(index)) {
                listView.getSelectionModel().clearSelection(index);
            } else {
                listView.getSelectionModel().select(index);
            }

            listView.requestFocus();

            e.consume();
        });
    }

    @Override
    protected void updateItem(Transferable item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
            setText(null);
            setGraphic(null);
        } else {
            assetName.setText(String.format("%s [%s]", item.getName(), item.getBalance()));
            assetId.setText(item.getAssetId());
            setGraphic(vBox);
        }
    }
}
