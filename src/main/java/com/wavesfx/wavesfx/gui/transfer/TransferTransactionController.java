package com.wavesfx.wavesfx.gui.transfer;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.MasterController;
import com.wavesfx.wavesfx.logic.FormValidator;
import com.wavesfx.wavesfx.logic.Transferable;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toLong;

public class TransferTransactionController extends MasterController {

    private static final Logger log = LogManager.getLogger();

    private static final int MESSAGE_MAX_SIZE = 140;

    @FXML private Label messageLimitLabel;
    @FXML private ComboBox<Transferable> assetComboBox;

    public TransferTransactionController(final RxBus rxBus) {
        super(rxBus);
    }

    @FXML
	public void initialize() {

    }

    ComboBox<Transferable> reinitializeComboBox(ComboBox<Transferable> comboBox, List<Transferable> transferableList) {
        final var selectedItem = comboBox.getSelectionModel().getSelectedIndex();
        if (!transferableList.isEmpty()) {
            comboBox.getItems().clear();
            comboBox.getItems().setAll(transferableList);
            comboBox.getSelectionModel().select(selectedItem);
        } else {
            comboBox.getItems().clear();
        }

        return comboBox;
    }

    boolean isValidMessage(final String string) {
        final var byteLength = string.getBytes().length;
        final var template = "/" + MESSAGE_MAX_SIZE;
        Observable.just(byteLength + template).subscribeOn(JavaFxScheduler.platform())
                .subscribe(messageLimitLabel::setText);

        return byteLength <= MESSAGE_MAX_SIZE;
    }

    boolean isValidAmount(final String amount) {
        final var selectedAsset = Optional.ofNullable(assetComboBox.getSelectionModel().getSelectedItem());
        if (amount.isEmpty() || selectedAsset.isEmpty() || !FormValidator.isWellFormed(amount, FormValidator.AMOUNT_PATTERN))
            return false;
        final long decimalAmount;
        try {
            decimalAmount = toLong(amount, selectedAsset.get().getDecimals());
        } catch (Exception e) {
            log.error("Decimal conversion error");
            return false;
        }

        return decimalAmount <= selectedAsset.get().balanceAsLong() && decimalAmount >= 1;
    }

    boolean isValidAmount(final long amount) {
        final var selectedAsset = Optional.ofNullable(assetComboBox.getSelectionModel().getSelectedItem());
        return amount <= selectedAsset.get().balanceAsLong() && amount >= 1;
    }
}
