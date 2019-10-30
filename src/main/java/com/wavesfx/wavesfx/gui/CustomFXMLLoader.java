package com.wavesfx.wavesfx.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ResourceBundle;

public class CustomFXMLLoader {

    public static Parent loadParent(FXMLView fxmlView, MasterController fxmlController, ResourceBundle resourceBundle) {
        final var loaderClass = MethodHandles.lookup().lookupClass();
        final var fxmlLoader = new FXMLLoader(loaderClass.getResource(fxmlView.toString()), resourceBundle);;
        fxmlLoader.setController(fxmlController);
        try {
            return fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
