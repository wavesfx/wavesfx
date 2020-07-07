package com.wavesfx.wavesfx;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.config.ConfigLoader;
import com.wavesfx.wavesfx.config.ConfigService;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.login.LoginController;
import com.wavesfx.wavesfx.utils.OperatingSystem;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import static com.wavesfx.wavesfx.utils.ApplicationSettings.RESOURCE_BUNDLE_FILEPATH;
import static com.wavesfx.wavesfx.utils.ApplicationSettings.getAppPath;

public class WavesFX extends Application {

    private static final Logger log = LogManager.getLogger();

    @Override
    public void start(Stage stage) throws IOException {
        System.setProperty("javafx.sg.warn", "false");
        final var appPath = getAppPath(OperatingSystem.getCurrentOS());
        log.info("Loading configuration file.");
        final var configLoader = new ConfigLoader(appPath);
        final var configService = ConfigService.build(configLoader.getConfigFile());
        final var locale = configService.getLanguage().isPresent() ? new Locale(configService.getLanguage().get()) : new Locale("en");
        final var messages = ResourceBundle.getBundle(RESOURCE_BUNDLE_FILEPATH, locale);
        final var rxBus = new RxBus();
        rxBus.getConfigService().onNext(configService);
        rxBus.getResourceBundle().onNext(messages);

        FXMLLoader loader = new FXMLLoader(getClass().getResource(FXMLView.LOGIN.toString()), messages);
        final var loginController = new LoginController(rxBus);
        loader.setController(loginController);
        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/wavesfx_icon.png")));
        scene.getStylesheets().add(getClass().getResource("/mainView/mainView.css").toExternalForm());
        stage.setTitle("WavesFX Wallet");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
        final var splashScreen = SplashScreen.getSplashScreen();
        if (splashScreen!=null) splashScreen.close();

//        if (!configService.tosIsAgreed()){
//            final var fxmlLoader = new FXMLLoader(getClass().getResource(FxmlView.TOS.toString()), messages);
//            fxmlLoader.setController(new TosController(rxBus));
//            final var parent = (Parent) fxmlLoader.load();
//            new DialogWindow(rxBus, stage, parent, DialogWindow.Type.TOS).show();
//        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
