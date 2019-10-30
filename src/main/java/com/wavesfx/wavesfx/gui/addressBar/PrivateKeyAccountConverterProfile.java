package com.wavesfx.wavesfx.gui.addressBar;

import com.wavesplatform.wavesj.PrivateKeyAccount;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.Map;

public class PrivateKeyAccountConverterProfile extends StringConverter<PrivateKeyAccount> {

    private final Map<String, PrivateKeyAccount> mapPrivateKeyAccount = new HashMap<>();

    @Override
    public String toString(final PrivateKeyAccount privateKeyAccount) {
        if (privateKeyAccount == null) {
            return "";
        } else {
            mapPrivateKeyAccount.put(privateKeyAccount.getAddress(), privateKeyAccount);
            return privateKeyAccount.getAddress();
        }
    }

    @Override
    public PrivateKeyAccount fromString(final String string) {
        return mapPrivateKeyAccount.get(string);
    }
}
