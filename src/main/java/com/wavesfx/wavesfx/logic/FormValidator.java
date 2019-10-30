package com.wavesfx.wavesfx.logic;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FormValidator {
    public static final Pattern AMOUNT_PATTERN = Pattern.compile("^|\\d+[.]?\\d*(?!.)");
    public static final Pattern MASS_TX_PATTERN = Pattern.compile("^\\w+[,]\\d+[.]?\\d*$");
    public static final Pattern TOKEN_NAME_PATTERN = Pattern.compile("^[^-\\s][a-zA-Z0-9_\\s-]{3,}+$");
    public static final Pattern PASSWORD_PATTERN = Pattern.compile("\\A(?=\\S*?[0-9])(?=\\S*?[a-z])(?=\\S*?[A-Z])\\S{8,}\\z");

    public static boolean areValid (Boolean... booleans) {
        return Stream.of(booleans)
                .allMatch(Boolean::booleanValue);
    }

    public static boolean isWellFormed(String message, Pattern pattern) {
        return pattern.matcher(message).matches();
    }
}
