package com.wavesfx.wavesfx.logic;

import io.reactivex.disposables.Disposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.sources.Change;
import javafx.scene.control.TextField;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FormValidator {
    public static final Pattern AMOUNT_PATTERN = Pattern.compile("^|\\d+[.]?\\d*(?!.)");
    public static final Pattern MASS_TX_PATTERN = Pattern.compile("^\\w+[,]\\d+[.]?\\d*$");
    public static final Pattern TOKEN_NAME_PATTERN = Pattern.compile("^[^-\\s][\\p{InBASIC_LATIN}\\s-]{3,16}+$");
    public static final Pattern ALIAS_PATTERN = Pattern.compile("^[^-\\s][\\p{InBASIC_LATIN}\\s-]{3,30}+$");
    public static final Pattern PASSWORD_PATTERN = Pattern.compile("\\A(?=\\S*?\\p{Digit})(?=\\S*?\\p{IsLower})(?=\\S*?\\p{IsUpper})\\S{8,}\\z");

    public static boolean areValid (Boolean... booleans) {
        return Stream.of(booleans)
                .allMatch(Boolean::booleanValue);
    }

    public static boolean isWellFormed(String message, Pattern pattern) {
        return pattern.matcher(message).matches();
    }

    public static Disposable inputCorrectorObservable(TextField textField, Pattern pattern) {
        return JavaFxObservable.changesOf(textField.textProperty())
                .filter(stringChange -> !isWellFormed(stringChange.getNewVal(), pattern))
                .map(Change::getOldVal)
                .subscribe(textField::setText);
    }
}
