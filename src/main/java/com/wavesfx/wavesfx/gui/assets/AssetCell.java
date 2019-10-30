package com.wavesfx.wavesfx.gui.assets;

import com.wavesfx.wavesfx.logic.Transferable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class AssetCell extends ListCell<Transferable> {
    private final VBox vBox;
    private final Label assetName;
    private final Label assetId;

    public AssetCell() {
        vBox = new VBox();
        assetName = new Label();
        assetId = new Label();
        vBox.setAlignment(Pos.CENTER_LEFT);
        assetName.getStyleClass().add("custom-cell-headline-label");
        assetId.getStyleClass().add("custom-cell-subline-label");
        vBox.getStyleClass().add("custom-cell-vbox");
        vBox.getChildren().addAll(assetName, assetId);
    }

    @Override
    protected void updateItem(Transferable item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
            setGraphic(null);
        } else {
            assetName.setText(String.format("%s [%s]", item.getName(), item.getBalance()));
            assetId.setText(item.getAssetId());
            setGraphic(vBox);
        }
    }
}
