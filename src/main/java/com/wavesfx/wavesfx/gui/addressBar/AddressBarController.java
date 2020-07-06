package com.wavesfx.wavesfx.gui.addressBar;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.config.ConfigService;
import com.wavesfx.wavesfx.gui.MasterController;
import com.wavesfx.wavesfx.logic.Profile;
import com.wavesplatform.wavesj.PrivateKeyAccount;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class AddressBarController extends MasterController {

    private final BehaviorSubject<Profile> profileSubject;
    private final BehaviorSubject<ConfigService> configServiceSubject;
    private Clipboard clipboard;

    @FXML private ComboBox<PrivateKeyAccount> addressComboBox;
    @FXML private Button addAddressButton;
    @FXML private Button copyClipboardButton;

    public AddressBarController(final RxBus rxBus) {
        super(rxBus);
        configServiceSubject = rxBus.getConfigService();
        profileSubject = rxBus.getProfile();
    }

    @FXML
	public void initialize() {
        clipboard = Clipboard.getSystemClipboard();
        addressComboBox.setConverter(new PrivateKeyAccountConverterProfile());

        profileSubject
                .filter(profile -> !profile.loadPrivateKeyAccounts().equals(addressComboBox.getItems()))
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::initializeAddresses);

        JavaFxObservable.valuesOf(addressComboBox.valueProperty())
                .observeOn(Schedulers.io())
                .subscribe(this::selectAddress);

        JavaFxObservable.actionEventsOf(addAddressButton)
                .observeOn(Schedulers.io())
                .subscribe(actionEvent -> addAddress());
    }

    @FXML
    void copyAddressToClipboard(ActionEvent event) {
        final var cbContent = new ClipboardContent();
        cbContent.putString(addressComboBox.valueProperty().get().getAddress());
        clipboard.setContent(cbContent);
    }

    private void addAddress() {
        final var configService = configServiceSubject.getValue();
        final var profile = profileSubject.getValue().addAddress();
        configService.updateProfile(profile);
        profileSubject.onNext(profile);
    }

    private void initializeAddresses(final Profile profile) {
        addressComboBox.getItems().setAll(profile.loadPrivateKeyAccounts());

        if (profile.getLastNonce() <= addressComboBox.getItems().size())
            addressComboBox.getSelectionModel().select(profile.getLastNonce());
        else if (!addressComboBox.getItems().isEmpty())
            addressComboBox.getSelectionModel().select(0);

        if (profile.isPrivateKeyAccount()) {
            addressComboBox.setDisable(true);
            addAddressButton.setDisable(true);
        }
    }

    private void selectAddress(PrivateKeyAccount privateKeyAccount) {
        final var configService = configServiceSubject.getValue();
        final var profile = profileSubject.getValue();

        rxBus.getPrivateKeyAccount().onNext(privateKeyAccount);
        final var updatedProfile = profile.updateLastNonce(addressComboBox.getSelectionModel().getSelectedIndex());
        configService.updateProfile(updatedProfile);
        profileSubject.onNext(updatedProfile);
    }
}
