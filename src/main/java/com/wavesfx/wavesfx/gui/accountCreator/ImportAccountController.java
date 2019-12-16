package com.wavesfx.wavesfx.gui.accountCreator;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.login.LoginController;
import com.wavesplatform.wavesj.Base58;
import com.wavesplatform.wavesj.PrivateKeyAccount;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
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
    @FXML private Label invalidPkeyLabel;

    public ImportAccountController(final RxBus rxBus, final AccountCreator accountCreator) {
        super(rxBus, accountCreator);
    }

    @FXML
    	public void initialize() {
        Observable.merge(JavaFxObservable.valuesOf(seedTextArea.textProperty()),
                JavaFxObservable.valuesOf(toggleGroup.selectedToggleProperty()))
                .map(changedValue -> notValid(seedTextArea.getText()))
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

    private boolean notValid(String s) {
        if (!pkeyAccountRadioButton.isSelected())
            invalidPkeyLabel.setVisible(false);
        if (pkeyAccountRadioButton.isSelected() && isInvalidPrivateKey(s)) {
            invalidPkeyLabel.setVisible(true);
            return true;
        } else if (s.isEmpty()) {
            return true;
        } else {
            invalidPkeyLabel.setVisible(false);
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
}
