package com.fortify.cli.aviator.util;

import com.fortify.cli.aviator._common.exception.AviatorBugException;
import com.fortify.cli.aviator.config.ExtensionsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTypeLanguageMapperUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FileTypeLanguageMapperUtil.class);
    private static volatile ExtensionsConfig extensionsConfigInstance;
    private static final Object initLock = new Object();
    private static volatile boolean initialized = false;

    public static void initializeConfig(ExtensionsConfig loadedExtensionsConfig) {
        if (!initialized) {
            synchronized (initLock) {
                if (!initialized) {
                    if (loadedExtensionsConfig == null) {
                        LOG.error("Attempted to initialize FileTypeLanguageMapperUtil with null ExtensionsConfig.");
                        throw new AviatorBugException("FileTypeLanguageMapperUtil cannot be initialized: ExtensionsConfig is null.");
                    }
                    extensionsConfigInstance = loadedExtensionsConfig;
                    initialized = true;
                    LOG.debug("FileTypeLanguageMapperUtil has been initialized.");
                }
            }
        } else {
            LOG.trace("FileTypeLanguageMapperUtil already initialized, skipping re-initialization.");
        }
    }

    public static String getProgrammingLanguage(String fileExtension) {
        if (!initialized) {
            LOG.warn("FileTypeLanguageMapperUtil accessed before explicit initialization. Attempting implicit initialization.");
            com.fortify.cli.aviator._common.config.AviatorConfigManager.getInstance();
            if (!initialized) {
                LOG.error("FileTypeLanguageMapperUtil is not initialized. Call AviatorConfigManager.getInstance() first.");
                throw new AviatorBugException("FileTypeLanguageMapperUtil not initialized. Ensure AviatorConfigManager.getInstance() is called.");
            }
        }

        if (StringUtil.isEmpty(fileExtension)) {
            return "Unknown";
        }
        String ext = fileExtension.startsWith(".")
                ? fileExtension
                : "." + fileExtension;

        return extensionsConfigInstance != null
                ? extensionsConfigInstance.getLanguageForExtension(ext)
                : "Unknown";
    }
}