package com.sismics.reader.core.util;

import java.util.ResourceBundle;

import com.sismics.reader.core.constant.ConfigType;
import com.sismics.reader.core.dao.jpa.ConfigDao;
import com.sismics.reader.core.model.jpa.Config;

/**
 * Configuration parameter utilities.
 * 
 * @author jtremeaux
 */
public class ConfigUtil {
    /**
     * Returns the textual value of a configuration parameter.
     * 
     * @param configType Type of the configuration parameter
     * @return Textual value of the configuration parameter
     * @throws IllegalStateException Configuration parameter undefined
     */
    public static String getConfigStringValue(ConfigType configType) {
        ConfigDao configDao = new ConfigDao();
        Config config = configDao.getById(configType);
        if (config == null) {
            throw new IllegalStateException("Config parameter not found: " + configType);
        }
        return config.getValue();
    }

    /**
     * Returns the configuration resource bundle.
     * 
     * @return Resource bundle
     */
    public static ResourceBundle getConfigBundle() {
        return ResourceBundle.getBundle("config");
    }

    /**
     * Returns the integer value of a configuration parameter.
     * 
     * @param configType Type of the configuration parameter
     * @return Integer value of the configuration parameter
     * @throws IllegalStateException Configuration parameter undefined
     */
    public static int getConfigIntegerValue(ConfigType configType) {
        String value = getConfigStringValue(configType);

        return Integer.parseInt(value);
    }

    /**
     * Returns the boolean value of a configuration parameter.
     * 
     * @param configType Type of the configuration parameter
     * @return Boolean value of the configuration parameter
     * @throws IllegalStateException Configuration parameter undefined
     */
    public static boolean getConfigBooleanValue(ConfigType configType) {
        String value = getConfigStringValue(configType);

        return Boolean.parseBoolean(value);
    }

    /**
     * Gemini API key.
     */
    public static final String CONFIG_GEMINI_API_KEY = "AIzaSyB73xDzzrinj1-5I-geuD5oQSngfWuIr1o";

    /**
     * Returns the string value of a configuration parameter from the properties
     * file.
     * 
     * @param key The key of the configuration parameter
     * @return String value of the configuration parameter
     */
    public static String getConfigStringValue(String key) {
        // For simple string constants, just return directly since we're not using
        // ConfigType
        if (CONFIG_GEMINI_API_KEY.equals(key)) {
            return CONFIG_GEMINI_API_KEY;
        }

        // For other values, try the properties file
        try {
            return getConfigBundle().getString(key);
        } catch (Exception e) {
            throw new IllegalStateException("Config parameter not found: " + key);
        }
    }
}
