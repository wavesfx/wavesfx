package com.wavesfx.wavesfx.logic;

import com.wavesplatform.wavesj.AssetDetails;

import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toLong;
import static com.wavesfx.wavesfx.logic.AssetNumeralFormatter.toReadable;

public class Waves implements Transferable {

    public static final long FEE = 100000;
    public static final String NAME = "Waves";
    public static final String ASSET_ID = "WAVES";
    public static final String MIN_FEE = "0.001";
    public static final String SPONSOR_BALANCE = "0.001";
    public static final int DECIMALS = 8;
    private final String balance;


    public Waves(Long balance) {
        this.balance = toReadable(balance, DECIMALS);
    }

    public Waves(String balance) {
        this.balance = balance;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getBalance() {
        return balance;
    }

    @Override
    public String getAssetId() {
        return ASSET_ID;
    }

    @Override
    public int getDecimals() {
        return DECIMALS;
    }

    @Override
    public String getMinFee() {
        return MIN_FEE;
    }

    @Override
    public String getSponsorBalance() {
        return SPONSOR_BALANCE;
    }

    @Override
    public long balanceAsLong() {
        return toLong(balance, DECIMALS);
    }

    @Override
    public long minFeeAsLong() {
        return toLong(MIN_FEE, DECIMALS);
    }

    @Override
    public long sponsorBalanceAsLong() {
        return toLong(SPONSOR_BALANCE, DECIMALS);
    }

    public static AssetDetails getAssetDetails() {
        return new AssetDetails(ASSET_ID, 0L, 0L, "", NAME, "", 8, false, 10000000000000000L, false, null);
    }

}
