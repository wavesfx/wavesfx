package com.wavesfx.wavesfx.gui.dashboard;

import com.wavesfx.wavesfx.utils.ApplicationSettings;
import com.wavesplatform.wavesj.BlockHeader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ResourceBundle;

public class BlockInfoBox extends HBox {
    private final ImageView blockImageView;
    private final Label headlineLabel;
    private final Label sublineLabel;
    private final VBox vBox;

    BlockInfoBox(final BlockHeader blockHeader, final ResourceBundle messages, final int counter) {
        blockImageView = new ImageView();
        vBox = new VBox();
        headlineLabel = new Label();
        sublineLabel = new Label();
        blockImageView.setImage(new Image(getClass().getResourceAsStream("/img/waves-block-"+counter+".png")));

        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(5, 0, 0, 0));
        this.getChildren().addAll(blockImageView, vBox);
        vBox.getChildren().addAll(headlineLabel, sublineLabel);
        vBox.setPadding(new Insets(0, 0, 0, 5));
        headlineLabel.getStyleClass().addAll("block-cell-headline-label");
        sublineLabel.getStyleClass().add("block-cell-subline-label");

        headlineLabel.setText(MessageFormat.format(messages.getString("block_headline"),
                String.valueOf(blockHeader.getHeight()), blockHeader.getTransactionCount()));
        final var dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(blockHeader.getTimestamp()), ZoneId.systemDefault());
        sublineLabel.setText(ApplicationSettings.FORMATTER.format(dateTime));
    }
}
