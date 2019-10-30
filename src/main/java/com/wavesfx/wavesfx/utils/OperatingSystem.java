package com.wavesfx.wavesfx.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum OperatingSystem {
    WIN(System.getenv("APPDATA")),
    MAC(System.getProperty("user.home") + "/Library"),
    LINUX(System.getProperty("user.home") + "/.config"),
    UNKNOWN(System.getProperty("user.home"));

    private final Path dataFolder;

    OperatingSystem(String dataFolder) {
        this.dataFolder = dataFolder == null ? null : Paths.get(dataFolder);
    }

    public Path getAppDataFolder() {
        return dataFolder;
    }

    public static OperatingSystem getCurrentOS() {
        final var os = System.getProperty("os.name").toUpperCase();

        if (os.contains("WIN")) {
            return WIN;
        } else if (os.contains("MAC")) {
            return MAC;
        } else if (os.contains("NUX")) {
            return LINUX;
        } else {
            return UNKNOWN;
        }
    }

}
