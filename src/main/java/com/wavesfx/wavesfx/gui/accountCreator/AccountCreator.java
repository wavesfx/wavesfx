package com.wavesfx.wavesfx.gui.accountCreator;

public class AccountCreator {
    private String name;
    private String seed;
    private String node;
    private char networkId;
    private boolean isPrivateKeyAccount;
    private boolean isImported;

    public AccountCreator() {
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(final String seed) {
        this.seed = seed;
    }

    public String getNode() {
        return node;
    }

    public void setNode(final String node) {
        this.node = node;
    }

    public char getNetworkId() {
        return networkId;
    }

    public void setNetworkId(final char networkId) {
        this.networkId = networkId;
    }

    public boolean isPrivateKeyAccount() {
        return isPrivateKeyAccount;
    }

    public void setPrivateKeyAccount(final boolean privateKeyAccount) {
        isPrivateKeyAccount = privateKeyAccount;
    }

    public boolean isImported() {
        return isImported;
    }

    public void setImported(final boolean imported) {
        isImported = imported;
    }

    @Override
    public String toString() {
        return "AccountCreator{" +
                "name='" + name + '\'' +
                ", seed='" + seed + '\'' +
                ", node='" + node + '\'' +
                ", networkId=" + networkId +
                '}';
    }
}
