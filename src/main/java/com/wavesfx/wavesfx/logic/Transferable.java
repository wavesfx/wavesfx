package com.wavesfx.wavesfx.logic;

public interface Transferable {
    String getName();

    String getBalance();

    String getAssetId();

    String getMinFee();

    String getSponsorBalance();

    int getDecimals();

    long balanceAsLong();

    long minFeeAsLong();

    long sponsorBalanceAsLong();
}
