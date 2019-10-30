package com.wavesfx.wavesfx.logic;

import com.wavesplatform.wavesj.AssetDetails;
import com.wavesplatform.wavesj.Transaction;
import com.wavesplatform.wavesj.Transfer;
import com.wavesplatform.wavesj.transactions.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.*;
import static com.wavesplatform.wavesj.transactions.AliasTransaction.ALIAS;
import static com.wavesplatform.wavesj.transactions.BurnTransaction.BURN;
import static com.wavesplatform.wavesj.transactions.DataTransaction.DATA;
import static com.wavesplatform.wavesj.transactions.ExchangeTransaction.EXCHANGE;
import static com.wavesplatform.wavesj.transactions.InvokeScriptTransaction.CONTRACT_INVOKE;
import static com.wavesplatform.wavesj.transactions.IssueTransaction.ISSUE;
import static com.wavesplatform.wavesj.transactions.LeaseCancelTransaction.LEASE_CANCEL;
import static com.wavesplatform.wavesj.transactions.LeaseTransaction.LEASE;
import static com.wavesplatform.wavesj.transactions.MassTransferTransaction.MASS_TRANSFER;
import static com.wavesplatform.wavesj.transactions.ReissueTransaction.REISSUE;
import static com.wavesplatform.wavesj.transactions.SetAssetScriptTransaction.SET_ASSET_SCRIPT;
import static com.wavesplatform.wavesj.transactions.SponsorTransaction.SPONSOR;
import static com.wavesplatform.wavesj.transactions.TransferTransaction.TRANSFER;
import static java.text.MessageFormat.format;

public class TransactionDetails {
    private static final Logger log = LogManager.getLogger();

    static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Transaction transaction;
    private final AssetDetailsService assetDetailsService;
    private final ResourceBundle messages;
    private final String address;
    private final long dateTime;
    private final String transactionId;
    private final Byte transactionType;
    private final TransactionSummary transactionSummary;

    public TransactionDetails(AssetDetailsService assetDetailsService, Transaction transaction, String address,
                              ResourceBundle messages) {
        this.assetDetailsService = assetDetailsService;
        this.messages = messages;
        this.address = address;
        this.transaction = transaction;
        this.dateTime = transaction.getTimestamp();
        this.transactionId = transaction.getId().getBase58String();
        this.transactionSummary = setTransactionSummary(transaction);
        this.transactionType = transaction.getType();
    }

    private String formatDateTime(long timestamp) {
        final var dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return FORMATTER.format(dateTime);
    }

    public String getDateTime() {
        return formatDateTime(dateTime);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public String getAddress() {
        return address;
    }

    public AssetDetailsService getAssetDetailsService() {
        return assetDetailsService;
    }

    public TransactionSummary getTransactionSummary() {
        return transactionSummary;
    }

    public boolean isOfTypeFilter(TxFilter txFilter) {
        final var txType = transaction.getType();
        switch (txFilter) {
            case Exchanged:
                return txType == EXCHANGE;
            case Received:
                return !transaction.getSenderPublicKey().getAddress().equals(address) && (txType == TRANSFER || txType == MASS_TRANSFER);
            case Leased:
                return txType == LEASE || txType == LEASE_CANCEL;
            case Issued:
                return txType == ISSUE || txType == BURN;
            case Sent:
                return transaction.getSenderPublicKey().getAddress().equals(address) && (txType == TRANSFER || txType == MASS_TRANSFER);
            case All:
                return true;
        }
        return false;
    }

    public boolean isTransferTransaction() {
        return transaction instanceof TransferTransaction;
    }

    public long getEpochDateTime() {
        return dateTime;
    }

    private AssetDetails fetchAssetDetails(String assetId) {
        return assetDetailsService.fetchAssetDetails(assetId);
    }

    private TransactionSummary setTransactionSummary(final Transaction transaction) {
        switch (transaction.getType()) {
            case TRANSFER:
                return generateTransferTransactionInfo((TransferTransaction) transaction);
            case DATA:
                return generateDataTransferInfo();
            case EXCHANGE:
                return generateExchangeTransactionInfo((ExchangeTransaction) transaction);
            case LEASE_CANCEL:
                return generateLeaseCancelTransactionInfo();
            case LEASE:
                return generateLeaseTransactionInfo((LeaseTransaction) transaction);
            case MASS_TRANSFER:
                return generateMassTransferTransactionInfo((MassTransferTransaction) transaction);
            case ISSUE:
                return generateIssueTransactionInfo((IssueTransaction) transaction);
            case REISSUE:
                return generateReissueTransactionInfo((ReissueTransaction) transaction);
            case BURN:
                return generateBurnTransactionInfo((BurnTransaction) transaction);
            case SPONSOR:
                return generateSponsorTransactionInfo((SponsorTransaction) transaction);
            case ALIAS:
                return generateAliasTransactionInfo((AliasTransaction) transaction);
            case CONTRACT_INVOKE:
                return generateInvokeScriptTransactionInfo((InvokeScriptTransaction) transaction);
            case SET_ASSET_SCRIPT:
                return generateSetScriptTransactionInfo((SetScriptTransaction) transaction);
            default:
                return messageToTransactionSummary(format("unknown_transaction", transaction.getId()));
        }
    }

    private TransactionSummary generateTransferTransactionInfo(final TransferTransaction transferTransaction) {
        final var assetDetails = fetchAssetDetails(transferTransaction.getAssetId());
        final var assetName = assetDetails.getName();
        final var amount = toReadable(transferTransaction.getAmount(), assetDetails.getDecimals());
        final var senderAddress = transferTransaction.getSenderPublicKey().getAddress();
        final var recipientAddress = transferTransaction.getRecipient();

        if (senderAddress.equals(address)) {
            return messageToTransactionSummary(format(messages.getString("sent"), amount, assetName, recipientAddress));
        } else {
            return messageToTransactionSummary(format(messages.getString("received"), amount, assetName, senderAddress));
        }
    }

    private TransactionSummary generateMassTransferTransactionInfo(final MassTransferTransaction massTransferTransaction) {
        final var assetDetails = fetchAssetDetails(massTransferTransaction.getAssetId());
        final var assetName = assetDetails.getName();
        final var senderAddress = massTransferTransaction.getSenderPublicKey().getAddress();
        final var transfer = massTransferTransaction.getTransfers().stream().parallel();

        if (senderAddress.equals(address)) {
            final var totalAmount = transfer.mapToLong(Transfer::getAmount).sum();
            final var amount = toReadable(totalAmount, assetDetails.getDecimals());
            return messageToTransactionSummary(format(messages.getString("sent_masstransfer"), amount, assetName));
        } else {
            final var receviedAmount = transfer
                    .filter(transfer1 -> transfer1.getRecipient().equals(address))
                    .mapToLong(Transfer::getAmount)
                    .findFirst();
            final var amount = toReadable(receviedAmount.orElse(0), assetDetails.getDecimals());
            return messageToTransactionSummary(format(messages.getString("received_masstransfer"), amount, assetName));
        }
    }

    private TransactionSummary generateDataTransferInfo() {
        return messageToTransactionSummary(messages.getString("data_transaction"));
    }

    private TransactionSummary generateExchangeTransactionInfo(final ExchangeTransaction exchangeTransaction) {
        final var buyOrder = exchangeTransaction.getOrder1();
        final var sellOrder = exchangeTransaction.getOrder2();
        final var buyer = buyOrder.getSenderPublicKey().getAddress();
        final var seller = sellOrder.getSenderPublicKey().getAddress();
        final var assetPair = buyOrder.getAssetPair();
        final var amountAssetDetails = fetchAssetDetails(assetPair.getAmountAsset());
        final var priceAssetDetails = fetchAssetDetails(assetPair.getPriceAsset());
        final BigDecimal price = toBigDecimal(exchangeTransaction.getPrice(), 8 + priceAssetDetails.getDecimals() - amountAssetDetails.getDecimals());
        final BigDecimal soldAmount = toBigDecimal(exchangeTransaction.getAmount(), amountAssetDetails.getDecimals()).multiply(price);
        final var boughtAmount = toReadable(exchangeTransaction.getAmount(), amountAssetDetails.getDecimals());
        final var readableSoldAmount = fromBigDecimalToReadable(soldAmount, priceAssetDetails.getDecimals());

        if (buyer.equals(address)) {
            return messageToTransactionSummary(format(messages.getString("bought"), boughtAmount, amountAssetDetails.getName(),
                    readableSoldAmount, priceAssetDetails.getName()));
        } else {
            return messageToTransactionSummary(format(messages.getString("sold"), boughtAmount, amountAssetDetails.getName()
                    , readableSoldAmount, priceAssetDetails.getName()));
        }
    }

    private TransactionSummary generateLeaseTransactionInfo(final LeaseTransaction leaseTransaction) {
        final var mainToken = assetDetailsService.getMainToken();
        final var amount = toReadable(leaseTransaction.getAmount(), mainToken.getDecimals());
        final var address = leaseTransaction.getRecipient();
        final var recipient = address.startsWith("alias:W:") ? address.replace("alias:W:", "")
                : address;
        final var sender = leaseTransaction.getSenderPublicKey().getAddress();
        if (!recipient.equals(this.address)) {
            return messageToTransactionSummary(format(messages.getString("started_leasing"), amount, mainToken.getName(), recipient));
        } else {
            return messageToTransactionSummary(format(messages.getString("received_lease"), amount, mainToken.getName(), sender));
        }
    }

    private TransactionSummary generateLeaseCancelTransactionInfo() {
        return messageToTransactionSummary(messages.getString("canceled_leasing"));
    }

    private TransactionSummary generateIssueTransactionInfo(IssueTransaction issueTransaction) {
        final var amount = toReadable(issueTransaction.getQuantity(), issueTransaction.getDecimals());
        return messageToTransactionSummary(format(messages.getString("issued_token"), issueTransaction.getName(), amount));
    }

    private TransactionSummary generateReissueTransactionInfo(ReissueTransaction reissueTransaction) {
        final var assetInfo = fetchAssetDetails(reissueTransaction.getAssetId());
        final var amount = toReadable(reissueTransaction.getQuantity(), assetInfo.getDecimals());
        return messageToTransactionSummary(format(messages.getString("reissued_token"), assetInfo.getName(), amount));
    }

    private TransactionSummary generateBurnTransactionInfo(BurnTransaction burnTransaction) {
        final var assetInfo = fetchAssetDetails(burnTransaction.getAssetId());
        final var amount = toReadable(burnTransaction.getAmount(), assetInfo.getDecimals());
        return messageToTransactionSummary(format(messages.getString("burned_token"), amount, assetInfo.getName()));
    }

    private TransactionSummary generateSponsorTransactionInfo(SponsorTransaction sponsorTransaction) {
        final var name = fetchAssetDetails(sponsorTransaction.getAssetId()).getName();
        return messageToTransactionSummary(format(messages.getString("sponsored_transaction"), name));
    }

    private TransactionSummary generateAliasTransactionInfo(AliasTransaction aliasTransaction) {
        return messageToTransactionSummary(format(messages.getString("alias_transaction"), aliasTransaction.getAlias()));
    }

    private TransactionSummary generateInvokeScriptTransactionInfo(InvokeScriptTransaction invokeScriptTransaction) {
        return messageToTransactionSummary(format(messages.getString("invoked_script"), invokeScriptTransaction.getdApp()));
    }

    private TransactionSummary generateSetScriptTransactionInfo(SetScriptTransaction setScriptTransaction) {
        return messageToTransactionSummary(messages.getString("set_script"));
    }

    private TransactionSummary messageToTransactionSummary(final String message) {
        final var regex = "\r";
        if (message.contains(regex)) {
            final var messages = message.split(regex);
            return new TransactionSummary(messages[0], messages[1]);
        }

        return new TransactionSummary(message, "");
    }
}
