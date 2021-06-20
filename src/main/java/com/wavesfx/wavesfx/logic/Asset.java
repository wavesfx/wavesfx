package com.wavesfx.wavesfx.logic;

import com.wavesplatform.wavesj.AssetBalance;

import java.io.IOException;
import java.util.Objects;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;

public class Asset implements Transferable {
    private final String name;
    private final String assetId;
    private final String issuer;
    private final Long balance;
    private final Long minFee;
    private final Long sponsorBalance;
    private final int decimals;
    private final boolean reissuable;

    public Asset(final AssetBalance assetBalance, final NodeService nodeService) throws IOException {
        if (assetBalance.getIssueTransactionV2() != null) {
            this.decimals = assetBalance.getIssueTransactionV2().getDecimals();
            this.name = assetBalance.getIssueTransactionV2().getName();
            this.issuer = assetBalance.getIssueTransactionV2().getSenderPublicKey().getAddress();
        } else {
            var assetDetails = nodeService.getNode().getAssetDetails(assetBalance.getAssetId());
            this.decimals = assetDetails.getDecimals();
            this.name = assetDetails.getName();
            this.issuer = assetDetails.getIssuer();
        }
        this.balance = assetBalance.getBalance();
        this.assetId = assetBalance.getAssetId();
        this.minFee = assetBalance.getMinSponsoredAssetFee();
        this.sponsorBalance = assetBalance.getSponsorBalance();
        this.reissuable = assetBalance.getReissuable();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBalance() {
        return toReadable(balance, decimals);
    }

    @Override
    public String getAssetId() {
        return assetId;
    }

    @Override
    public int getDecimals() {
        return decimals;
    }

    @Override
    public String getMinFee() {
        return minFee != null ? toReadable(minFee, decimals) : null;
    }

    @Override
    public String getSponsorBalance() {
        return sponsorBalance != null ? toReadable(sponsorBalance, decimals) : null;
    }

    @Override
    public long balanceAsLong() {
        return balance;
    }

    @Override
    public long minFeeAsLong() {
        return minFee;
    }

    @Override
    public long sponsorBalanceAsLong() {
        return sponsorBalance;
    }

    public String getIssuer() {
        return issuer;
    }

    public boolean isReissuable() {
        return reissuable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset)) return false;
        Asset asset = (Asset) o;
        return getDecimals() == asset.getDecimals() &&
                isReissuable() == asset.isReissuable() &&
                Objects.equals(getName(), asset.getName()) &&
                Objects.equals(getAssetId(), asset.getAssetId()) &&
                Objects.equals(getIssuer(), asset.getIssuer()) &&
                Objects.equals(getBalance(), asset.getBalance()) &&
                Objects.equals(getMinFee(), asset.getMinFee()) &&
                Objects.equals(getSponsorBalance(), asset.getSponsorBalance());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getAssetId(), getIssuer(), getBalance(), getMinFee(), getSponsorBalance(), getDecimals(), isReissuable());
    }
}
