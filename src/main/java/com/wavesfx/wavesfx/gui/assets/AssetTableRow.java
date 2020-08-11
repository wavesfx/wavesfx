package com.wavesfx.wavesfx.gui.assets;

import com.wavesfx.wavesfx.bus.RxBus;
import com.wavesfx.wavesfx.gui.CustomFXMLLoader;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.MasterController;
import com.wavesfx.wavesfx.gui.dialog.AssetInfoController;
import com.wavesfx.wavesfx.gui.dialog.BurnController;
import com.wavesfx.wavesfx.gui.dialog.DialogWindow;
import com.wavesfx.wavesfx.gui.dialog.ReissueTokenController;
import com.wavesfx.wavesfx.logic.Asset;
import com.wavesfx.wavesfx.logic.Transferable;
import com.wavesplatform.wavesj.Node;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Optional;
import java.util.ResourceBundle;

public class AssetTableRow extends TableRow<Transferable> {
    private static final Logger log = LogManager.getLogger();

    private final BehaviorSubject<Node> nodeSubject;
    private final MenuItem assetInfoMenuItem;
    private final MenuItem burnTokenMenuItem;
    private final MenuItem reissueTokenMenuItem;
    private final MenuItem copyAssetIdMenuItem;
    private final ResourceBundle resourceBundle;
    private final RxBus rxBus;
    private final Stage stage;

    AssetTableRow(RxBus rxBus, Stage stage) {
        this.rxBus = rxBus;
        this.stage = stage;
        resourceBundle = rxBus.getResourceBundle().getValue();
        nodeSubject = rxBus.getNode();
        assetInfoMenuItem = new MenuItem(resourceBundle.getString("asset_info"));
        burnTokenMenuItem = new MenuItem(resourceBundle.getString("burn_asset"));
        reissueTokenMenuItem = new MenuItem(resourceBundle.getString("reissue_token"));
        copyAssetIdMenuItem = new MenuItem(resourceBundle.getString("copy_asset_id"));

        copyAssetIdMenuItem.setOnAction(event -> Optional.ofNullable(this.getItem())
                        .ifPresent(transferable -> {
                            final var clipboard = Clipboard.getSystemClipboard();
                            final var clipboardContent = new ClipboardContent();
                            final var assetId = transferable.getAssetId();
                            clipboardContent.putString(assetId);
                            clipboard.setContent(clipboardContent);
                        }));

        assetInfoMenuItem.setOnAction(event -> Optional.ofNullable(this.getItem())
                .ifPresent(transferable -> Observable.just(transferable.getAssetId()).observeOn(Schedulers.io())
                        .map(nodeSubject.getValue()::getAssetDetails)
                        .doOnNext(rxBus.getAssetDetails()::onNext)
                        .observeOn(JavaFxScheduler.platform())
                        .subscribe(assetDetails -> createDialog(FXMLView.ASSET_INFO_DIALOG, new AssetInfoController(rxBus)))));

        burnTokenMenuItem.setOnAction(event -> Optional.ofNullable(this.getTableView().getSelectionModel().getSelectedItem())
                .ifPresent(transferable -> Observable.just(transferable).observeOn(Schedulers.io())
                        .doOnNext(this::pushTxDataToBus)
                        .observeOn(JavaFxScheduler.platform())
                        .subscribe(transferable1 -> createDialog(FXMLView.BURN_TOKEN, new BurnController(rxBus)))));

        reissueTokenMenuItem.setOnAction(event -> Optional.ofNullable(this.getTableView().getSelectionModel().getSelectedItem())
                .ifPresent(transferable -> Observable.just(transferable).observeOn(Schedulers.io())
                        .doOnNext(this::pushTxDataToBus)
                        .observeOn(JavaFxScheduler.platform())
                        .subscribe(transferable1 -> createDialog(FXMLView.REISSUE_TOKEN, new ReissueTokenController(rxBus)))));
    }

    @Override
    protected void updateItem(Transferable item, boolean empty) {
        super.updateItem(item, empty);

        if (this.getItem() != null) {
            final var mainTokenAssetId = rxBus.getMainTokenDetails().getValue().getAssetId();
            if (this.getItem().getAssetId().equals(mainTokenAssetId)) {
                this.setContextMenu(new ContextMenu());
            } else {
                final var asset = (Asset) this.getItem();
                final var privateKeyAccount = rxBus.getPrivateKeyAccount().getValue();
                if (asset.isReissuable() && asset.getIssuer().equals(privateKeyAccount.getAddress())) {
                    this.setContextMenu(new ContextMenu(assetInfoMenuItem, copyAssetIdMenuItem, burnTokenMenuItem, reissueTokenMenuItem));
                } else {
                    this.setContextMenu(new ContextMenu(assetInfoMenuItem, copyAssetIdMenuItem, burnTokenMenuItem));
                }
            }
        }
    }

    private void pushTxDataToBus(final Transferable transferable1) {
        final var node = rxBus.getNode().getValue();
        try {
            final var assetDetails = node.getAssetDetails(transferable1.getAssetId());
            rxBus.getAssetDetails().onNext(assetDetails);
            rxBus.getTransferable().onNext(transferable1);
        } catch (IOException e) {
            log.error("Error fetching AssetDetails", e);
        }
    }


    private void createDialog(FXMLView fxmlView, MasterController fxmlController){
        final var parent = CustomFXMLLoader.loadParent(fxmlView, fxmlController, resourceBundle);
        new DialogWindow(rxBus, stage, parent).show();
    }
}
