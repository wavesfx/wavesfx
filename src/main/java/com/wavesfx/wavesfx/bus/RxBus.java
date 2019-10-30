package com.wavesfx.wavesfx.bus;

import com.wavesfx.wavesfx.config.ConfigService;
import com.wavesfx.wavesfx.gui.FXMLView;
import com.wavesfx.wavesfx.gui.accountCreator.AccountCreator;
import com.wavesfx.wavesfx.logic.*;
import com.wavesplatform.wavesj.*;
import com.wavesplatform.wavesj.transactions.TransferTransaction;
import io.reactivex.subjects.BehaviorSubject;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import java.util.*;

public class RxBus {
    private final Collection<BehaviorSubject<?>> behaviorSubjects = new ArrayList<>();
    private final BehaviorSubject<AccountCreator> accountCreatorBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<AssetDetails> assetDetailsBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<AssetDetails> mainTokenDetailsBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<AssetDetailsService> assetDetailsServiceBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<BalanceDetails> balanceDetailsBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<ConfigService> configServiceBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<HashMap<FXMLView, FXMLLoader>> fxmlLoaderHashMap = createBehaviorSubject();
    private final BehaviorSubject<HashMap<String, AssetDetails>> assetDetailsHashMapBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<List<TransactionDetails>> transactionDetailsListBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<List<Transferable>> assetListBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<List<TransferTransaction>> transactionsBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<Long> emitterBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<Node> nodeBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<NodeService> nodeService2BehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<PrivateKeyAccount> privateKeyAccountBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<Profile> profileBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<ResourceBundle> resourceBundleBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<Stage> stageBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<Transaction> transactionBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<TransactionDetails> transactionDetailsBehaviorSubject = createBehaviorSubject();
    private final BehaviorSubject<Transferable> transferableBehaviorSubject = createBehaviorSubject();

    public RxBus() {
    }

    private <T> BehaviorSubject<T> createBehaviorSubject() {
        final BehaviorSubject<T> behaviorSubject = BehaviorSubject.create();
        behaviorSubjects.add(behaviorSubject);
        return behaviorSubject;
    }

    public BehaviorSubject<ResourceBundle> getResourceBundle() {
        return resourceBundleBehaviorSubject;
    }

    public BehaviorSubject<ConfigService> getConfigService() {
        return configServiceBehaviorSubject;
    }

    public BehaviorSubject<Profile> getProfile() {
        return profileBehaviorSubject;
    }

    public BehaviorSubject<PrivateKeyAccount> getPrivateKeyAccount() {
        return privateKeyAccountBehaviorSubject;
    }

    public BehaviorSubject<List<Transferable>> getAssetList() {
        return assetListBehaviorSubject;
    }

    public BehaviorSubject<Node> getNode() {
        return nodeBehaviorSubject;
    }

    public BehaviorSubject<List<TransactionDetails>> getTransactionDetailsList() {
        return transactionDetailsListBehaviorSubject;
    }

    public BehaviorSubject<AssetDetails> getAssetDetails() {
        return assetDetailsBehaviorSubject;
    }

    public BehaviorSubject<AssetDetails> getMainTokenDetails() {
        return mainTokenDetailsBehaviorSubject;
    }

    public BehaviorSubject<Transaction> getTransaction() {
        return transactionBehaviorSubject;
    }

    public BehaviorSubject<Transferable> getTransferable() {
        return transferableBehaviorSubject;
    }

    public BehaviorSubject<AccountCreator> getAccountCreator() {
        return accountCreatorBehaviorSubject;
    }

    public BehaviorSubject<Long> getEmitter() {
        return emitterBehaviorSubject;
    }

    public BehaviorSubject<BalanceDetails> getBalanceDetails() {
        return balanceDetailsBehaviorSubject;
    }

    public BehaviorSubject<List<TransferTransaction>> getTransactions() {
        return transactionsBehaviorSubject;
    }

    public BehaviorSubject<HashMap<String, AssetDetails>> getAssetDetailsHashMap() {
        return assetDetailsHashMapBehaviorSubject;
    }

    public BehaviorSubject<AssetDetailsService> getAssetDetailsService() {
        return assetDetailsServiceBehaviorSubject;
    }

    public BehaviorSubject<TransactionDetails> getTransactionDetails() {
        return transactionDetailsBehaviorSubject;
    }

    public BehaviorSubject<Stage> getStageBehaviorSubject(){
        return stageBehaviorSubject;
    }

    public BehaviorSubject<NodeService> getNodeService() {
        return nodeService2BehaviorSubject;
    }
}
