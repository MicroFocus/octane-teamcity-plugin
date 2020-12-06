package com.hp.octane.plugins.jetbrains.teamcity.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;

/**
 * SDK based (log4j brought by SDK) logger provider
 * Main purpose of this custom logger provider is to ensure correct logs location configuration at the earliest point of the plugin initialization
 */
public final class SDKBasedLoggerProvider {
    private static volatile boolean sysParamConfigured = false;
    private static volatile boolean allowedOctaneStorageExist = false;
    private static volatile SDKBasedLoggerProvider provider = null;

    private SDKBasedLoggerProvider() {
    }

    public static SDKBasedLoggerProvider getInstance() {
        if (provider == null) {
            synchronized (SDKBasedLoggerProvider.class) {
                if (provider == null)
                    provider = new SDKBasedLoggerProvider();
            }
        }
        return provider;
    }

    public Logger getLogger(Class<?> type) {
        initOctaneAllowedStorageProperty();
        return LogManager.getLogger(type);
    }

    public void initOctaneAllowedStorageProperty() {
        if (!sysParamConfigured) {
            System.setProperty("octaneAllowedStorage", getAllowedStorageFile().getAbsolutePath() + File.separator);
            sysParamConfigured = true;
        }
    }

    public File getAllowedStorageFile() {
        String tomcatBase = System.getProperty("catalina.base");
        String webApp = String.format("%s/webapps/ROOT", tomcatBase);
        File f = new File(webApp, "logs");
        if (!allowedOctaneStorageExist) {
            f.mkdirs();
            allowedOctaneStorageExist = true;
        }
        return f;
    }
}
