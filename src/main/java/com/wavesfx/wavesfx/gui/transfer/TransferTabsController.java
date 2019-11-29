package com.wavesfx.wavesfx.gui.transfer;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.MasterController;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class TransferTabsController extends MasterController {

    private final BurnAssetsController burnAssetsController;
    private final MassTransferController massTransferController;
    private final MoveAssetsController moveAssetsController;
    private final SendController sendController;
    private final SetScriptController setScriptController;

    @FXML private Tab sendTab;
    @FXML private Tab massTransferTab;
    @FXML private Tab setScriptTab;
    @FXML private Tab moveAssetsTab;
    @FXML private Tab burnAssetsTab;
    @FXML private TabPane tabPane;

    public TransferTabsController(final RxBus rxBus) {
        super(rxBus);
        sendController = new SendController(rxBus);
        massTransferController = new MassTransferController(rxBus);
        setScriptController = new SetScriptController(rxBus);
        moveAssetsController = new MoveAssetsController(rxBus);
        burnAssetsController = new BurnAssetsController(rxBus);
    }

    @FXML
	public void initialize() {
        sendTab.setContent(loadParent(FXMLView.SEND, sendController));
        massTransferTab.setContent(loadParent(FXMLView.MASS_TRANSFER, massTransferController));
        setScriptTab.setContent(loadParent(FXMLView.SET_SCRIPT, setScriptController));
        moveAssetsTab.setContent(loadParent(FXMLView.MOVE_ASSETS, moveAssetsController));
        burnAssetsTab.setContent(loadParent(FXMLView.BURN_ASSETS, burnAssetsController));
    }
}
