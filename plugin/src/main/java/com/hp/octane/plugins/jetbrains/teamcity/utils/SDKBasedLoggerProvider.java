package com.hp.octane.plugins.jetbrains.teamcity.utils;

import com.hp.octane.integrations.services.logging.CommonLoggerContextUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;

/**
 * SDK based (log4j brought by SDK) logger provider
 * Main purpose of this custom logger provider is to ensure correct logs location configuration at the earliest point of the plugin initialization
 */
public final class SDKBasedLoggerProvider {

    public static Logger getLogger(Class<?> type) {
        return LogManager.getLogger(type);
    }

    public static void configure(File allowedStorage) {
        CommonLoggerContextUtil.configureLogger(allowedStorage);
    }

}
