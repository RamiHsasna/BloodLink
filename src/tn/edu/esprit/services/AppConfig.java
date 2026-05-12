package tn.edu.esprit.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Properties;

public final class AppConfig {

    private static final Properties FILE_CONFIG = loadFileConfig();

    private AppConfig() {
    }

    public static String get(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (isPresent(value)) {
                return value.trim();
            }
        }

        for (String name : names) {
            String value = FILE_CONFIG.getProperty(name);
            if (isPresent(value)) {
                return value.trim();
            }
        }

        return null;
    }

    public static String getOrDefault(String name, String fallback) {
        String value = get(name);
        return value != null ? value : fallback;
    }

    private static Properties loadFileConfig() {
        Properties properties = new Properties();
        String workingDir = System.getProperty("user.dir");
        String userHome = System.getProperty("user.home");

        loadPropertiesFile(properties, new File(userHome, "bloodlink_config.properties"));
        loadPropertiesFile(properties, new File(workingDir, "config.properties"));
        loadDotEnvFile(properties, new File(workingDir, ".env"));
        return properties;
    }

    private static void loadPropertiesFile(Properties properties, File file) {
        if (!file.exists() || !file.canRead()) {
            return;
        }
        try (FileInputStream input = new FileInputStream(file)) {
            Properties loaded = new Properties();
            loaded.load(input);
            properties.putAll(loaded);
        } catch (Exception exception) {
            System.err.println("config: unable to read " + file.getAbsolutePath() + " -> " + exception.getMessage());
        }
    }

    private static void loadDotEnvFile(Properties properties, File file) {
        if (!file.exists() || !file.canRead()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                String key = trimmed.substring(0, separator).trim();
                String value = stripQuotes(trimmed.substring(separator + 1).trim());
                if (!key.isEmpty() && isPresent(value)) {
                    properties.setProperty(key, value);
                }
            }
        } catch (Exception exception) {
            System.err.println("config: unable to read " + file.getAbsolutePath() + " -> " + exception.getMessage());
        }
    }

    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
