package com.wavesfx.wavesfx.logic;

import com.wavesplatform.wavesj.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class NodeAddressValidator {
    private static final Logger log = LogManager.getLogger();

    public static boolean isValidAddress(final String address, final Node node) {
        if (address.isEmpty()) {
            return false;
        } else if (AddressValidator.validateAddress(address, node.getChainId())){
            return true;
        } else {
            try {
                node.getAddrByAlias(address);
                return true;
            } catch (IOException e) {
                log.error("Error fetching address");
                return false;
            }
        }
    }
}
