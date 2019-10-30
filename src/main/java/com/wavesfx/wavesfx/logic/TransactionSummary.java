package com.wavesfx.wavesfx.logic;

public class TransactionSummary {
    private final String headline;
    private final String info;

    public TransactionSummary(final String headline, final String info) {
        this.headline = headline;
        this.info = info;
    }

    public String getHeadline() {
        return headline;
    }

    public String getInfo() {
        return info;
    }
}
