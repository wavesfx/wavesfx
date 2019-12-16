package com.wavesfx.wavesfx.gui.settings;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.config.ConfigService;
import com.wavesfx.wavesfx.gui.MasterController;
import com.wavesfx.wavesfx.gui.style.StyleHandler;
import com.wavesfx.wavesfx.logic.NodeService;
import com.wavesfx.wavesfx.logic.Profile;
import com.wavesplatform.wavesj.Base58;
import com.wavesplatform.wavesj.Node;
import com.wavesplatform.wavesj.PrivateKeyAccount;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SettingsController extends MasterController {
    private ConfigService configService;
    private Profile profile;
    private final Observable<PrivateKeyAccount> observablePrivateKeyAccount;
    private static final String MASK = "***************************************************";
    private static final int MASK_COUNTER = 15;

    @FXML private AnchorPane rootPane;
    @FXML private Label messageLimitLabel;
    @FXML private Label assetLimitLabel;
    @FXML private Button saveButton;
    @FXML private Button showEncodedSeedButton;
    @FXML private Button showSeedButton;
    @FXML private Button showPkeyButton;
    @FXML private TextField encodedSeedTextField;
    @FXML private TextField nodeAddressTextField;
    @FXML private TextField seedTextField;
    @FXML private TextField privateKeyTextField;
    @FXML private TextField publicKeyTextField;

    public SettingsController(final RxBus rxBus) {
        super(rxBus);
        observablePrivateKeyAccount = rxBus.getPrivateKeyAccount();
        rxBus.getConfigService().subscribe(configService -> this.configService = configService);
        rxBus.getProfile().subscribe(profile -> this.profile = profile);
    }

    @FXML
	public void initialize() {
        nodeAddressTextField.setText(profile.getNode());

        observablePrivateKeyAccount.subscribe(privateKeyAccount -> hideTextFields());
        observablePrivateKeyAccount.map(privateKeyAccount -> Base58.encode(privateKeyAccount.getPublicKey()))
                .subscribe(publicKeyTextField::setText);

        if (profile.isPrivateKeyAccount()) {
            Stream.of(showSeedButton, seedTextField, showEncodedSeedButton, encodedSeedTextField)
                    .forEach(node -> node.setDisable(true));
        }

        final var observableIsValidNode = JavaFxObservable.valuesOf(nodeAddressTextField.textProperty())
                .map(this::isValidNode);

        StyleHandler.setBorderDisposable(observableIsValidNode, nodeAddressTextField);

        observableIsValidNode
                .subscribe(b -> saveButton.setDisable(!b));

        JavaFxObservable.actionEventsOf(saveButton)
                .subscribe(actionEvent -> saveChanges());

        temporarilyShowStringObservable(showSeedButton, seedTextField, () -> profile.getSeed());
        temporarilyShowStringObservable(showEncodedSeedButton, encodedSeedTextField, () -> Base58.encode(profile.getSeed().getBytes()));
        temporarilyShowStringObservable(showPkeyButton, privateKeyTextField, () -> Base58.encode(getPrivateKeyAccount().getPrivateKey()));

    }

    private void hideTextFields() {
        Stream.of(privateKeyTextField, seedTextField, encodedSeedTextField)
                .forEach(textField -> textField.setText(MASK));
    }

    private void saveChanges() throws URISyntaxException {
        final var nodeAddress = nodeAddressTextField.getText();
        final var updatedProfile = profile.changeNode(nodeAddress);
        final var privateKeyAccount = getPrivateKeyAccount();
        final var node = new Node(nodeAddress, privateKeyAccount.getChainId());
        final var nodeService = new NodeService(node);
        rxBus.getProfile().onNext(updatedProfile);
        rxBus.getNode().onNext(node);
        rxBus.getNodeService().onNext(nodeService);
        configService.updateProfile(updatedProfile);
    }

    private boolean isValidNode(final String node) {
        final var nodeLowerCase = node.toLowerCase();
        return (nodeLowerCase.startsWith("http://") || nodeLowerCase.startsWith("https://"));
    }

    private void temporarilyShowStringObservable(Button button, TextField textField, Supplier<String> secretString) {
        JavaFxObservable.actionEventsOf(button)
                .map(ae -> secretString.get())
                .doOnNext(textField::setText)
                .delay(MASK_COUNTER, TimeUnit.SECONDS, Schedulers.io())
                .subscribe(actionEvent -> textField.setText(MASK));
    }
}
