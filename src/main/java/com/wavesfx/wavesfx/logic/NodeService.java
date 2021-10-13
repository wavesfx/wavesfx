package com.wavesfx.wavesfx.logic;

import com.wavesplatform.wavesj.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class NodeService {
    private static final Logger log = LogManager.getLogger();

    private final Node node;

    public NodeService(Node node) {
        this.node = node;
    }

    public Node getNode(){
        return node;
    }

    public Optional<Long> calculateFee(Transaction transaction){
        try {
            return Optional.of(node.calculateFee(transaction).getFeeAmount());
        } catch (IOException e) {
            log.error("Error calculating Fee", e);
            return Optional.empty();
        }
    }

    public Optional<CalculatedFee> calculateAssetFee(Transaction transaction){
        try {
            return Optional.of(node.calculateFee(transaction));
        } catch (IOException e) {
            log.error("Error calculating Fee", e);
            return Optional.empty();
        }
    }

    public Optional<BalanceDetails> fetchBalanceDetails(String address){
        try {
            return Optional.of(node.getBalanceDetails(address));
        } catch (IOException e) {
            log.error("Error fetching balance details", e);
            return Optional.empty();
        }
    }

    public Optional<String> fetchNodeversion(){
        try {
            return Optional.of(node.getVersion());
        } catch (IOException e) {
            log.error("Error fetching node version", e);
            return Optional.empty();
        }
    }

    public Optional<Integer> fetchHeight(){
        try {
            return Optional.of(node.getHeight());
        } catch (IOException e) {
            log.error("Error fetching height", e);
            return Optional.empty();
        }
    }

    public Optional<List<BlockHeader>> fetchBlockHeader(int from, int to){
        try {
            return Optional.of(node.getBlockHeaderSeq(from , to));
        } catch (IOException e) {
            log.error("Error fetching block header", e);
            return Optional.empty();
        }
    }

    public Optional<List<String>> fetchAliasByAddress(String address){
        try {
            return Optional.of(node.getAliasbyAddress(address));
        } catch (IOException e) {
            log.error("Error fetching aliases", e);
            return Optional.empty();
        }
    }

    public Optional<String> fetchAddressByAlias(String alias){
        try {
            return Optional.of(node.getAddrByAlias(alias));
        } catch (IOException e) {
            log.error("Error fetching address by alias");
            return Optional.empty();
        }
    }

    public Optional<List<AssetDetails>> fetchNFTs(String address, int limit) {
        try {
            return Optional.of(node.getNFTs(address, limit));
        } catch (IOException e) {
            log.error("Error fetching asset NFTs", e);
            return Optional.empty();
        }
    }

    public Optional<List<AssetBalance>> fetchAssetBalance(String address){
        try {
            return Optional.of(node.getAssetsBalance(address));
        } catch (IOException e) {
            log.error("Error fetching asset balance", e);
            return Optional.empty();
        }
    }

    public Optional<Long> fetchBalance(String address){
        try {
            return Optional.of(node.getBalance(address));
        } catch (IOException e) {
            log.error("Error fetching balance", e);
            return Optional.empty();
        }
    }

    public Optional<List<Transaction>> fetchAddressTransactions(String address, int amount){
        try {
            return Optional.of(node.getAddressTransactions(address, amount));
        } catch (IOException e) {
            log.error("Error fetching transactions of address", e);
            return Optional.empty();
        }
    }

    public Optional<String> compileScript(String script){
        try {
            return Optional.ofNullable(node.compileScript(script));
        } catch (IOException e) {
            log.error("Error compiling script", e);
            return Optional.empty();
        }
    }

    public Optional<List<LeaseInfo>> fetchActiveLeases(String address){
        try {
            return Optional.of(node.getActiveLeases(address));
        } catch (IOException e) {
            log.error("Error fetching active leases", e);
            return Optional.empty();
        }
    }

    public Optional<String> sendTransaction(Transaction transaction){
        try {
            return Optional.of(node.send(transaction));
        } catch (IOException e) {
            log.error("Error sending transaction", e);
            return Optional.empty();
        }
    }

    public Optional<Transaction> getTransaction(String transactionId) {
        try {
            return Optional.of(node.getTransaction(transactionId));
        } catch (IOException e) {
            log.error("Error fetching transaction");
            return Optional.empty();
        }
    }

    private Optional<AssetDetails> fetchAssetDetails(String assetId) {
        try {
            return Optional.of(node.getAssetDetails(assetId));
        } catch (IOException e) {
            log.error("Fetching asset details failed", e);
            return Optional.empty();
        }
    }

}
