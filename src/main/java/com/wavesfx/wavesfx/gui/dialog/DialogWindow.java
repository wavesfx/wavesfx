package com.wavesfx.wavesfx.gui.dialog;

import com.wavesfx.wavesfx.bus.RxBus;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ResourceBundle;

public class DialogWindow {
    private final RxBus rxBus;
    private final ResourceBundle messages;
    private final Stage stage;
    private final Parent parent;

    public DialogWindow(RxBus rxBus, Stage mainStage, Parent parent) {
        this.rxBus = rxBus;
        this.messages = rxBus.getResourceBundle().getValue();
        this.stage = mainStage;
        this.parent = parent;
    }

    private Stage initializeStage(){
        final var newStage = new Stage();
        final var icons = stage.getIcons();
        newStage.setScene(new Scene(parent));
        newStage.getIcons().setAll(icons);
        newStage.resizableProperty().set(false);
        newStage.requestFocus();
        newStage.initOwner(stage);
        newStage.initStyle(StageStyle.UNDECORATED);
        newStage.initModality(Modality.WINDOW_MODAL);
        rxBus.getStageBehaviorSubject().onNext(newStage);
        return newStage;
    }

    public void show() {
        initializeStage().show();
    }

    public void showAndWait() {
        initializeStage().showAndWait();
    }

}
