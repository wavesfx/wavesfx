package com.wavesfx.wavesfx.gui;

import javafx.scene.image.Image;

import java.util.Locale;

public class LocaleIcon {
    private final Image icon;
    private final Locale locale;

    public LocaleIcon(final Image icon, final Locale locale) {
        this.icon = icon;
        this.locale = locale;
    }

    public Image getIcon() {
        return icon;
    }

    public Locale getLocale() {
        return locale;
    }

    @Override
    public String toString() {
        return locale.toString();
    }
}
