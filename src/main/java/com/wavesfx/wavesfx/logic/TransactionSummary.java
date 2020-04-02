package com.wavesfx.wavesfx.logic;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionSummary)) return false;
        TransactionSummary that = (TransactionSummary) o;
        return Objects.equals(getHeadline(), that.getHeadline()) &&
                Objects.equals(getInfo(), that.getInfo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHeadline(), getInfo());
    }
}
