package com.wavesfx.wavesfx.gui.login;

import com.wavesfx.wavesfx.gui.LocaleIcon;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class LanguageCell extends ListCell<LocaleIcon> {
    private final HBox hBox;
    private final Label localeName;
    private final ImageView flag;

    public LanguageCell() {
        hBox = new HBox();
        flag = new ImageView();
        localeName = new Label();
        localeName.getStyleClass().add("language-label");
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.getChildren().addAll(flag, localeName);
    }

    @Override
    protected void updateItem(LocaleIcon item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
            setGraphic(null);
        } else {
            flag.setImage(item.getIcon());
            localeName.setText(item.getLocale().getLanguage());
            setGraphic(hBox);
        }
    }
}
