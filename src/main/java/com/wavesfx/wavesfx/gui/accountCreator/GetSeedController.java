package com.wavesfx.wavesfx.gui.accountCreator;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.login.LoginController;
import com.wavesplatform.wavesj.PrivateKeyAccount;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class GetSeedController extends AccountCreatorController  {

    @FXML private TextArea seedTextArea;

    public GetSeedController(final RxBus rxBus, final AccountCreator accountCreator) {
        super(rxBus, accountCreator);
    }

    @FXML
	public void initialize() {
        Observable.just(0).subscribeOn(Schedulers.computation())
                .map(integer -> PrivateKeyAccount.generateSeed())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(seedTextArea::setText);
    }

    @FXML
    void back(ActionEvent event) {
        switchRootScene(FXMLView.LOGIN, new LoginController(rxBus));
    }

    @FXML
    void next(ActionEvent event) {
        accountCreator.setSeed(seedTextArea.getText());
        switchRootScene(FXMLView.CONFIRM_SEED, new ConfirmSeedController(rxBus, accountCreator));
    }
}
