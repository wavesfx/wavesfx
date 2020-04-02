package com.wavesfx.wavesfx.logic;

import com.wavesplatform.wavesj.AssetDetails;
import com.wavesplatform.wavesj.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class AssetDetailsService {
    private static final Logger log = LogManager.getLogger();

    private final HashMap<String, AssetDetails> assetDetailsHashMap;
    private final AssetDetails mainToken;
    private final Node node;

    public AssetDetailsService(HashMap<String, AssetDetails> assetDetailsHashMap, AssetDetails mainToken, Node node) {
        this.assetDetailsHashMap = assetDetailsHashMap;
        this.mainToken = mainToken;
        this.node = node;
    }

    public AssetDetails fetchAssetDetails (final String assetId) {
        if (assetDetailsHashMap.containsKey(assetId)){
            return assetDetailsHashMap.get(assetId);
        } else {
            try {
                final var assetDetail =
                        (assetId == null || assetId.equals(mainToken.getAssetId()) ? mainToken : node.getAssetDetails(assetId));
                assetDetailsHashMap.put(assetId, assetDetail);
                return assetDetail;
            } catch (IOException e) {
                log.error("Fetching asset details failed");
            }
        }
        return null;
    }

    public Node getNode() {
        return node;
    }

    public AssetDetails getMainToken() {
        return mainToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetDetailsService)) return false;
        AssetDetailsService that = (AssetDetailsService) o;
        return Objects.equals(assetDetailsHashMap, that.assetDetailsHashMap) &&
                Objects.equals(getMainToken(), that.getMainToken()) &&
                Objects.equals(getNode(), that.getNode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetDetailsHashMap, getMainToken(), getNode());
    }
}
