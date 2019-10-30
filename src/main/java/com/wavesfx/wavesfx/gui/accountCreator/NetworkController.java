package com.wavesfx.wavesfx.gui.accountCreator;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.login.LoginController;
import com.wavesfx.wavesfx.logic.Net;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

public class NetworkController extends AccountCreatorController  {

    @FXML private ComboBox<Net> networkComboBox;
    @FXML private AnchorPane networkPane;
    @FXML private TextField nodeUrlTextField;
    @FXML private TextField networkIdTextField;

    public NetworkController(final RxBus rxBus, final AccountCreator accountCreator) {
        super(rxBus, accountCreator);
    }

    @FXML
	public void initialize() {
        initializeComboBox();

        final var nodeUrlValueObservable = JavaFxObservable.valuesOf(nodeUrlTextField.textProperty());
        final var networkIdValueObservable = JavaFxObservable.valuesOf(networkIdTextField.textProperty());
        final var networkChoiceValueObservable = JavaFxObservable.valuesOf(networkComboBox.valueProperty());

        networkChoiceValueObservable
                .subscribe(this::initializeInputs);

        networkIdValueObservable.filter(s -> s.length() > 1)
                .subscribe(s -> networkIdTextField.setText(s.substring(0, 1)));

        ConnectableObservable.merge(networkIdValueObservable.map(String::isEmpty), nodeUrlValueObservable.map(String::isEmpty))
                .map(Boolean::booleanValue)
                .subscribe(nextButton::setDisable);

        networkChoiceValueObservable.map(this::hasInvalidNetworkParameters).subscribe(nextButton::setDisable);
    }

    @FXML
    void back(ActionEvent event) {
        switchRootScene(FXMLView.LOGIN, new LoginController(rxBus));
    }

    @FXML
    void next(ActionEvent event) throws Exception {
        importOrCreateSeed();
    }

    private void initializeComboBox() {
        networkComboBox.getItems().setAll(Net.class.getEnumConstants());
        networkComboBox.getSelectionModel().select(0);
    }

    private void initializeInputs(Net net) {
        nodeUrlTextField.setText(net.getNode());
        networkIdTextField.setText(String.valueOf(net.getNetworkId()));

        if (net == Net.Custom){
            networkIdTextField.setDisable(false);
        } else {
            networkIdTextField.setDisable(true);
        }
    }

    private void importOrCreateSeed() {
        setNetworkSettings();
        if (accountCreator.isImported()) {
            switchRootScene(FXMLView.IMPORT_ACCOUNT, new ImportAccountController(rxBus, accountCreator));
        } else {
            switchRootScene(FXMLView.GET_SEED, new GetSeedController(rxBus, accountCreator));
        }
    }

    private void setNetworkSettings() {
        accountCreator.setNode(nodeUrlTextField.getText());
        accountCreator.setNetworkId(networkIdTextField.getText().charAt(0));
    }

    private boolean hasInvalidNetworkParameters(Net net) {
        return net == Net.Custom && (nodeUrlTextField.getText().isEmpty() || networkIdTextField.getText().isEmpty());
    }
}
