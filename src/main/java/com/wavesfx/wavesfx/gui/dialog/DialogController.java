package com.wavesfx.wavesfx.gui.dialog;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.MasterController;
import javafx.fxml.FXML;

class DialogController extends MasterController {
    DialogController(final RxBus rxBus) {
        super(rxBus);
    }

    @FXML
	public void initialize() {
    }

    @FXML
    void closeDialog() {
        getStage().close();
    }
}
