package com.wavesfx.wavesfx.gui.assets;

import com.wavesfx.wavesfx.logic.Transferable;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.Map;

public class AssetConverterProfile extends StringConverter<Transferable> {

    private final Map<String, Transferable> mapAsset = new HashMap<>();

    @Override
    public String toString(final Transferable asset) {
        if (asset == null) {
            return "";
        } else {
            final var assetWithBalance = asset.getName() + " [Balance: " + asset.getBalance() + "]";
            mapAsset.put(assetWithBalance, asset);
            return assetWithBalance;
        }
    }

    @Override
    public Transferable fromString(final String string) {
        return mapAsset.get(string);
    }
}
