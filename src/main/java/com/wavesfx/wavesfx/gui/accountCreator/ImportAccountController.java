package com.wavesfx.wavesfx.gui.accountCreator;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.login.LoginController;
import com.wavesplatform.wavesj.Base58;
import com.wavesplatform.wavesj.PrivateKeyAccount;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ImportAccountController extends AccountCreatorController  {

    private static final Logger log = LogManager.getLogger();

    @FXML private ToggleGroup toggleGroup;
    @FXML private RadioButton encodedSeedAccountRadioButton;
    @FXML private RadioButton seedAccountRadioButton;
    @FXML private RadioButton pkeyAccountRadioButton;
    @FXML private TextArea seedTextArea;
    @FXML private Label warningLabel;

    public ImportAccountController(final RxBus rxBus, final AccountCreator accountCreator) {
        super(rxBus, accountCreator);
    }

    @FXML
    public void initialize() {
        Observable.merge(JavaFxObservable.valuesOf(seedTextArea.textProperty()),
                JavaFxObservable.valuesOf(toggleGroup.selectedToggleProperty()))
                .observeOn(Schedulers.computation())
                .map(changedValue -> notValidBackup(seedTextArea.getText()))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(nextButton::setDisable, Throwable::printStackTrace);

        JavaFxObservable.valuesOf(pkeyAccountRadioButton.selectedProperty())
                .map(Boolean::booleanValue)
                .subscribe(accountCreator::setPrivateKeyAccount);
    }

    @FXML
    void back(ActionEvent event) {
        switchRootScene(FXMLView.LOGIN, new LoginController(rxBus));
    }

    @FXML
    void next(ActionEvent event) {
        if (encodedSeedAccountRadioButton.isSelected())
            accountCreator.setSeed(new String(Base58.decode(seedTextArea.getText())));
        else {
            accountCreator.setSeed(seedTextArea.getText());
        }
        switchRootScene(FXMLView.LOGIN_INFO, new LoginInfoController(rxBus, accountCreator));
    }

    private boolean notValidBackup(String s) {
        final var pKeyButtonIsSelected = pkeyAccountRadioButton.isSelected();
        final var encodedSeedButtonIsSelected = encodedSeedAccountRadioButton.isSelected();

        if (pKeyButtonIsSelected && isInvalidPrivateKey(s)) {
            setWarningLabelTranslated("invalid_pkey");
            return true;
        } else if (encodedSeedButtonIsSelected && isInvalidEncodedSeed(s)) {
            setWarningLabelTranslated("invalid_encoded_seed");
            return true;
        } else if (s.isEmpty()) {
            Platform.runLater(() -> warningLabel.setText(""));
            return true;
        } else if (startsOrEndsWithSpace(s)) {
            setWarningLabelTranslated("space_warning");
            return false;
        } else {
            Platform.runLater(() -> warningLabel.setText(""));
            return false;
        }
    }

    private boolean isInvalidPrivateKey(String s) {
        try {
            PrivateKeyAccount.fromPrivateKey(s, (byte) accountCreator.getNetworkId());
            return false;
        } catch (Exception e) {
            log.error("privatekey account could not be created");
            return true;
        }
    }

    private boolean isInvalidEncodedSeed(String s) {
        try {
            Base58.decode(s);
            return false;
        } catch (Exception e) {
            log.error("string could not be Base58-decoded");
            return true;
        }
    }

    private boolean startsOrEndsWithSpace(String s) {
        return s.startsWith(" ") || s.endsWith(" ");
    }

    private void setWarningLabelTranslated(String messageProperty) {
        Platform.runLater(() -> warningLabel.setText(getMessages().getString(messageProperty)));
    }
}
