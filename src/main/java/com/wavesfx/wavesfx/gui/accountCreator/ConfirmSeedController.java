package com.wavesfx.wavesfx.gui.accountCreator;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class ConfirmSeedController extends AccountCreatorController  {

    @FXML private TextArea seedTextArea;

    public ConfirmSeedController(final RxBus rxBus, final AccountCreator accountCreator) {
        super(rxBus, accountCreator);
    }

    @FXML
	public void initialize() {
        JavaFxObservable.valuesOf(seedTextArea.textProperty())
                .map(seed -> !seed.equals(accountCreator.getSeed()))
                .subscribe(seedNotCorrect -> nextButton.setDisable(seedNotCorrect));
    }

    @FXML
    void back(ActionEvent event) {
        switchRootScene(FXMLView.GET_SEED, new GetSeedController(rxBus, accountCreator));
    }

    @FXML
    void next(ActionEvent event) {
        accountCreator.setSeed(seedTextArea.getText());
        switchRootScene(FXMLView.LOGIN_INFO, new LoginInfoController(rxBus, accountCreator));
    }
}
