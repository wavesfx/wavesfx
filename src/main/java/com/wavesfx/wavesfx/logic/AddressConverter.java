package com.wavesfx.wavesfx.logic;

import com.wavesplatform.wavesj.Alias;

public class AddressConverter {
    public static String toRawString (String recipient, byte chainId){
        return recipient.length()<=30 ? Alias.fromRawString(recipient, chainId).toRawString() : recipient;
    }
}
