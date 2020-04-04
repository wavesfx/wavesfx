package com.wavesfx.wavesfx.gui.login;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.config.ConfigService;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.LocaleIcon;
import com.wavesfx.wavesfx.gui.MasterController;
import com.wavesfx.wavesfx.gui.WalletViewController;
import com.wavesfx.wavesfx.gui.accountCreator.AccountCreator;
import com.wavesfx.wavesfx.gui.accountCreator.NetworkController;
import com.wavesfx.wavesfx.logic.NodeService;
import com.wavesfx.wavesfx.logic.Profile;
import com.wavesplatform.wavesj.Node;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.wavesfx.wavesfx.utils.ApplicationSettings.*;

public class LoginController extends MasterController {
    private static final Logger log = LogManager.getLogger();

    private final ConfigService configService;

    @FXML private ComboBox<Profile> profileComboBox;
    @FXML private ComboBox<LocaleIcon> languageComboBox;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button deleteButton;
    @FXML private Button newAccountButton;
    @FXML private Button importAccountButton;
    @FXML private Label invalidPassphraseLabel;
    @FXML private RadioButton offlineModeRadioButton;

    public LoginController(final RxBus rxBus) {
        super(rxBus);
        configService = rxBus.getConfigService().getValue();
    }

    @FXML
    public void initialize() {
        initializeLanguageComboBox();
        initializeProfileComboBox();
        initializeLanguageSwitcher();
        initializeObservableLoginButtons();
        initializeProfileRemover();
        initializepasswordFieldDecorator();
        initializeOfflineModeBox();
    }

    @FXML
    void importAccount(ActionEvent event) {
//        if (!tosIsAgreed()) return;
        createNewProfile(true);
    }

    @FXML
    void newAccount(ActionEvent event) {
//        if (!tosIsAgreed()) return;
        createNewProfile(false);
    }

    private void initializepasswordFieldDecorator() {
        JavaFxObservable.valuesOf(passwordField.textProperty())
                .subscribe(this::decoratePasswordField);
    }

    private void initializeProfileRemover() {
        JavaFxObservable.actionEventsOf(deleteButton)
//                .doOnNext(actionEvent -> tosIsAgreed())
                .flatMapMaybe(actionEvent -> JavaFxObservable.fromDialog(buildWarningPopup()))
                .filter(response -> response.equals(ButtonType.OK))
                .subscribe(actionEvent -> deleteProfile(), Throwable::printStackTrace);

        JavaFxObservable.nullableValuesOf(profileComboBox.valueProperty())
                .subscribe(profile -> profile.ifPresentOrElse(p ->  deleteButton.setVisible(true), () -> deleteButton.setVisible(false)));
    }

    private void initializeObservableLoginButtons() {
        Observable.merge(
                JavaFxObservable.eventsOf(loginButton, ActionEvent.ACTION),
                JavaFxObservable.eventsOf(passwordField, KeyEvent.KEY_PRESSED).filter(e -> e.getCode().equals(KeyCode.ENTER)))
                .doOnNext(event -> disableControls(true))
                .observeOn(Schedulers.io())
                .map(event -> decryptAccount())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::logIntoWallet, Throwable::printStackTrace);
    }

    private void initializeLanguageSwitcher() {
        final var initialSelection = languageComboBox.getValue();
        JavaFxObservable.valuesOf(languageComboBox.valueProperty()).observeOn(Schedulers.io())
                .filter(localeIcon -> !localeIcon.equals(initialSelection))
                .map(LocaleIcon::getLocale)
                .doOnNext(configService::setLanguage)
                .map(locale -> ResourceBundle.getBundle(RESOURCE_BUNDLE_FILEPATH, locale))
                .doOnNext(rxBus.getResourceBundle()::onNext)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(resourceBundle -> reloadScene(), Throwable::printStackTrace);
    }

    private void reloadScene() {
        switchRootScene(FXMLView.LOGIN, new LoginController(rxBus));
    }

    private void disableControls(boolean setDisable) {
        passwordField.setDisable(setDisable);
        loginButton.setDisable(setDisable);
    }

    private void initializeLanguageComboBox() {
        final var localeIconList = getLocaleIcons();
        languageComboBox.setCellFactory(param -> new LanguageCell());
        languageComboBox.setButtonCell(new LanguageCell());
        languageComboBox.getItems().setAll(localeIconList);
        selectLanguage();
    }

//    private boolean tosIsAgreed(){
//        if (!configService.tosIsAgreed()){
//            final var parent = CustomFXMLLoader.loadParent(FXMLView.TOS, new TosController(rxBus), getMessages());
//            createDialog(parent);
//            return false;
//        }
//        return true;
//    }

    private void createNewProfile(final boolean isImportedProfile) {
        final var accountCreator = new AccountCreator();
        accountCreator.setImported(isImportedProfile);
        switchRootScene(FXMLView.NETWORK, new NetworkController(rxBus, accountCreator));
    }

    private void deleteProfile() {
        final var selectedAccount = profileComboBox.getSelectionModel().getSelectedItem().getName();
        profileComboBox.getItems().remove(profileComboBox.getSelectionModel().getSelectedItem());
        configService.removeProfile(selectedAccount);
        profileComboBox.getSelectionModel().select(0);
    }

    private void publishSelectedUserSettings() {
        final var profile = profileComboBox.getSelectionModel().getSelectedItem();
        try {
            final var node = new Node(profile.getNode(), profile.getNetworkId());
            rxBus.getProfile().onNext(profile);
            rxBus.getNode().onNext(node);
            rxBus.getNodeService().onNext(new NodeService(node));
        } catch (URISyntaxException e) {
            log.error("Node could not be created", e);
        }
    }

    private boolean decryptAccount() {
        final var password = passwordField.getText();
        final var selectedAccount = profileComboBox.getSelectionModel().getSelectedItem();

        try {
            final var decryptedAccount = selectedAccount.decrypt(password);
            publishSelectedUserSettings();
            rxBus.getProfile().onNext(decryptedAccount);
            rxBus.getPrivateKeyAccount().onNext(decryptedAccount.loadPrivateKeyAccounts().get(0));
            rxBus.getMainTokenDetails().onNext(MAIN_TOKEN);
            configService.setLastUser(selectedAccount.getName());
            return true;
        } catch (Exception e) {
            log.error("Error logging into Wallet", e);
            return false;
        }
    }

    private void logIntoWallet(boolean decrypted) {
//        if (!tosIsAgreed()) return;
        disableControls(false);
        if (decrypted) {
            switchRootScene(FXMLView.WALLET, new WalletViewController(rxBus, offlineModeRadioButton.isSelected()));
        } else {
            Observable.just(0).doOnNext(integer -> invalidPassphraseLabel.setVisible(true))
                    .delay(10, TimeUnit.SECONDS)
                    .subscribe(integer -> invalidPassphraseLabel.setVisible(false));
        }
    }

    private void decoratePasswordField(String password) {
        if (password.isEmpty())
            passwordField.getStyleClass().set(1, "alert");
        else
            passwordField.getStyleClass().set(1, "");
    }

    private Alert buildWarningPopup() {
        final var alert = new Alert(Alert.AlertType.CONFIRMATION);
        final var stage = getStage();
        final var alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alert.initOwner(stage);
        alert.setGraphic(null);
        alert.initStyle(StageStyle.UNDECORATED);
        alert.setHeaderText(getMessages().getString("delete_header"));
        alert.setContentText(getMessages().getString("delete_message"));
        final var cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        final var okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        cancelButton.setText(getMessages().getString("cancel"));
        okButton.setText(getMessages().getString("ok"));
        alertStage.getIcons().setAll(stage.getIcons());
        return alert;
    }

    private void selectLanguage(){
        final var configLanguage = configService.getLanguage().orElse(getMessages().getLocale().getLanguage());
        languageComboBox.getItems().stream()
                .filter(localeIcon -> localeIcon.getLocale().getLanguage().equals(configLanguage))
                .findAny()
                .ifPresent(languageComboBox.getSelectionModel()::select);
        languageComboBox.setFocusTraversable(false);
    }

    private List<LocaleIcon> getLocaleIcons() {
        return Arrays.stream(LANGUAGES)
                .map(this::fetchLocaleIcon)
                .collect(Collectors.toUnmodifiableList());
    }

    private void initializeProfileComboBox() {
        profileComboBox.setCellFactory(param -> new LoginCell());
        profileComboBox.setButtonCell(new LoginCell());

        configService.getProfiles().ifPresent(profileComboBox.getItems()::setAll);
        profileComboBox.getItems().stream()
                .filter(profile -> profile.getName().equals(configService.getLastUser().orElse("")))
                .findAny()
                .ifPresentOrElse(profileComboBox.getSelectionModel()::select, () -> profileComboBox.getSelectionModel().select(0));
    }

    private void initializeOfflineModeBox() {
        configService.offlineModeIsEnabled()
                .ifPresent(offlineModeRadioButton.selectedProperty()::setValue);

        JavaFxObservable.valuesOf(offlineModeRadioButton.selectedProperty())
                .subscribeOn(Schedulers.io())
                .subscribe(configService::setOfflineMode);
    }

    private LocaleIcon fetchLocaleIcon(String localeName){
        final var image = new Image(getClass().getResourceAsStream("/icons/"+localeName+".png"));
        final var locale = new Locale(localeName);
        return new LocaleIcon(image, locale);
    }

}
