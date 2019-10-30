package com.wavesfx.wavesfx.encryption;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

abstract class EncrypterConfig {
    static final byte[] IV = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    static final int SALT_SIZE = 128;
    static final int ITERATION_COUNT = 65536;
    static final int KEY_SIZE = 256;
    static final String PDKDF_ALGO = "PBKDF2WithHmacSHA256";
    static final String KEY_ALGO = "AES";
    static final String ALGO = "AES/CBC/PKCS5Padding";
    static final String SEPERATOR = "__";
    static final Charset CHARSET = StandardCharsets.UTF_8;
}
