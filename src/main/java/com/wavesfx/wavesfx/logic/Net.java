package com.wavesfx.wavesfx.logic;

import com.wavesfx.wavesfx.utils.ApplicationSettings;

public enum Net {
    Mainnet(ApplicationSettings.MAINNET_NODE, ApplicationSettings.MAINNET_ID),
    Testnet(ApplicationSettings.TESTNET_NODE, ApplicationSettings.TESTNET_ID),
    Stagenet(ApplicationSettings.STAGENET_NODE, ApplicationSettings.STAGENET_ID),
    Custom(ApplicationSettings.CUSTOM_NODE, ApplicationSettings.CUSTOM_ID);

    private final String node;
    private final char networkId;

    Net(final String node, final char networkId) {
        this.node = node;
        this.networkId = networkId;
    }

    public String getNode() {
        return node;
    }

    public char getNetworkId() {
        return networkId;
    }

    public static Net getFullNetworkName(char networkId) {
        if (networkId == Mainnet.getNetworkId())
            return Mainnet;
        else if (networkId == Testnet.getNetworkId())
            return Testnet;
        else if (networkId == Stagenet.getNetworkId())
            return Stagenet;
        else
            return Custom;
    }
}
