package com.wavesfx.wavesfx.encryption;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Encrypter {

    public static String encrypt(String message, String passphrase) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        final var messageAsChar = message.getBytes(EncrypterConfig.CHARSET);
        final var passphraseAsChar = passphrase.toCharArray();
        final var salt = getSalt();

        final var encryptedMessage = getEncryptedMessage(passphraseAsChar, messageAsChar, salt);

        return (concatenateMessage(salt, encryptedMessage));
    }

    public static String decrypt(String message, String passphrase) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {

        final var splitMessage = message.split(EncrypterConfig.SEPERATOR);
        final var salt = Base64.getDecoder().decode(splitMessage[0]);
        final var encryptedMessage = Base64.getDecoder().decode(splitMessage[1]);
        final var passphraseAsChar = passphrase.toCharArray();

        final var secretKeySpec = getSecretKeySpec(passphraseAsChar, salt);
        final var decryptedMessage = getDecryptedMessage(secretKeySpec, encryptedMessage);

        return new String(decryptedMessage);
    }

    private static byte[] getSalt() {
        final var salt = new byte[EncrypterConfig.SALT_SIZE];
        final var secureRandom = new SecureRandom();
        secureRandom.nextBytes(salt);
        return salt;
    }

    private static SecretKeySpec getSecretKeySpec(char[] passphrase, byte[] salt) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        final var secretKeyFactory = SecretKeyFactory.getInstance(EncrypterConfig.PDKDF_ALGO);
        final var pbeKeySpec = new PBEKeySpec(passphrase, salt, EncrypterConfig.ITERATION_COUNT, EncrypterConfig.KEY_SIZE);
        final var secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
        return new SecretKeySpec(secretKey.getEncoded(), EncrypterConfig.KEY_ALGO);
    }

    private static Cipher getCipher(SecretKeySpec secretKeySpec) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        final var ivSpec = new IvParameterSpec(EncrypterConfig.IV);
        final var cipher = Cipher.getInstance(EncrypterConfig.ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
        return cipher;
    }

    private static byte[] getEncryptedMessage(char[] passphrase, byte[] message, byte[] salt)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        final var secret = getSecretKeySpec(passphrase, salt);
        final var cipher = getCipher(secret);
        return cipher.doFinal(message);
    }

    private static byte[] getDecryptedMessage(SecretKeySpec secretKeySpec, byte[] encryptedMessage)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException {
        final var ivSpec = new IvParameterSpec(EncrypterConfig.IV);
        final var cipher = Cipher.getInstance(EncrypterConfig.ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
        try {
            return cipher.doFinal(encryptedMessage);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String concatenateMessage(byte[] salt, byte[] message) {
        return Stream.of(salt, message)
                .map(Base64.getEncoder()::encode)
                .map(m -> new String(m, StandardCharsets.UTF_8))
                .collect(Collectors.joining(EncrypterConfig.SEPERATOR));
    }

}
