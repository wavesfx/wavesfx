package com.wavesfx.wavesfx.gui.login;

import com.wavesfx.wavesfx.logic.Net;
import com.wavesfx.wavesfx.logic.Profile;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class LoginCell extends ListCell<Profile> {
    private final VBox vBox;
    private final Label profileNameLabel;
    private final Label networkLabel;

    LoginCell() {
        vBox = new VBox();
        profileNameLabel = new Label();
        networkLabel = new Label();
        vBox.setAlignment(Pos.CENTER_LEFT);
        profileNameLabel.getStyleClass().add("login-cell-headline-label");
        networkLabel.getStyleClass().add("custom-cell-subline-label");
        vBox.getChildren().addAll(profileNameLabel, networkLabel);
    }

    @Override
    protected void updateItem(Profile item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
            setGraphic(null);
        } else {
            profileNameLabel.setText(item.getName());
            networkLabel.setText(Net.getFullNetworkName(item.getNetworkId()).toString());
            setGraphic(vBox);
        }
    }
}
