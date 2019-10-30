package com.wavesfx.wavesfx.config;

import com.electronwill.nightconfig.core.file.FileConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader {

    private final static String FILE_NAME = "config.toml";
    private final static String DEFAULT_CONFIG = "/config/defaultConfig.toml";
    private final Path path;

    public ConfigLoader(Path configPath) {
        this.path = configPath;
    }

    public FileConfig getConfigFile() throws IOException {
        final var filePath = Path.of(this.path.toString(), FILE_NAME);
        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return FileConfig.builder(filePath)
                .defaultData(getClass().getResource(DEFAULT_CONFIG))
                .build();
    }
}
