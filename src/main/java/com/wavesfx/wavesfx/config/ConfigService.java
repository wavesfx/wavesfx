package com.wavesfx.wavesfx.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.wavesfx.wavesfx.logic.Profile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfigService {

    private final static String LAST_USED = "last_user";
    private final static String TOS_AGREED = "tos_agreed";
    private final static String OFFLINE_MODE = "offline_mode";
    private final static String LANG = "lang";
    private final static String ROOT = "Accounts";
    private final static String SEED = ".seed";
    private final static String NONCE = ".nonce";
    private final static String LAST_NONCE = ".last_nonce";
    private final static String NODE = ".node";
    private final static String NETWORK_ID = ".networkID";
    private final static String IS_PRIVATEKEYACCOUNT = ".isPrivateKeyAccount";

    private final FileConfig fileConfig;

    private ConfigService(FileConfig fileConfig) {
        this.fileConfig = fileConfig;
        fileConfig.load();
    }

    public FileConfig getFileConfig() {
        return fileConfig;
    }

    public static ConfigService build(FileConfig fileConfig) {
        return new ConfigService(fileConfig);
    }

    public Optional<List<Profile>> getProfiles() {
        final Optional<Config> accounts = fileConfig.getOptional(ROOT);
        return accounts.map(config -> config.valueMap().entrySet().stream()
                .map(m -> getProfile(config, m))
                .collect(Collectors.toUnmodifiableList()));
    }

    public Profile getProfile(final Config accounts, final Map.Entry<String, Object> m) {
        final var name = m.getKey();
        try {
            final String seed = accounts.get(name + SEED);
            final Integer nonce = accounts.get(name + NONCE);
            final Integer last_nonce = accounts.get(name + LAST_NONCE);
            final String node = accounts.get(name + NODE);
            final char networkId = accounts.get(name + NETWORK_ID).toString().charAt(0);
            final boolean isPrivateKeyAccount = accounts.get(name + IS_PRIVATEKEYACCOUNT);
            return new Profile(name, seed, node, isPrivateKeyAccount, nonce, last_nonce, networkId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveProfile(Profile profile) {
        var path = getKeyPath(profile.getName());
        fileConfig.set(path + SEED, profile.getSeed());
        fileConfig.set(path + NODE, profile.getNode());
        fileConfig.set(path + NETWORK_ID, Character.toString(profile.getNetworkId()));
        fileConfig.set(path + NONCE, profile.getNonce());
        fileConfig.set(path + LAST_NONCE, profile.getLastNonce());
        fileConfig.set(path + IS_PRIVATEKEYACCOUNT, profile.isPrivateKeyAccount());
        fileConfig.save();
    }

    public void updateProfile(Profile profile) {
        var path = getKeyPath(profile.getName());
        fileConfig.set(path + NODE, profile.getNode());
        fileConfig.set(path + NETWORK_ID, Character.toString(profile.getNetworkId()));
        fileConfig.set(path + NONCE, profile.getNonce());
        fileConfig.set(path + LAST_NONCE, profile.getLastNonce());
        fileConfig.save();
    }

    public void removeProfile(String profile) {
        fileConfig.remove(getKeyPath(profile));
        fileConfig.save();
    }

    public boolean profileExists(String profile) {
        return fileConfig.getOptional(getKeyPath(profile)).isPresent();
    }

    private String getKeyPath(String child) {
        return ROOT + "." + child;
    }

    public void agreeTos() {
        fileConfig.set(TOS_AGREED, true);
        fileConfig.save();
    }

    public boolean tosIsAgreed() {
        return fileConfig.get(TOS_AGREED);
    }

    public void setLanguage(Locale locale){
        fileConfig.set(LANG, locale.getLanguage());
        fileConfig.save();
    }

    public Optional<String> getLanguage() {
        return fileConfig.getOptional(LANG);
    }

    public void setLastUser(String profileName) {
        fileConfig.set(LAST_USED, profileName);
        fileConfig.save();
    }

    public Optional<String> getLastUser() {
        return fileConfig.getOptional(LAST_USED);
    }

    public void setOfflineMode(boolean bool) {
        fileConfig.set(OFFLINE_MODE, bool);
        fileConfig.save();
    }

    public Optional<Boolean> offlineModeIsEnabled() {
        return fileConfig.getOptional(OFFLINE_MODE);
    }
}
