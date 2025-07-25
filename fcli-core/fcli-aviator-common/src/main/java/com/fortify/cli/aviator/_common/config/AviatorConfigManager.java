package com.fortify.cli.aviator._common.config;

import com.fortify.cli.aviator._common.exception.AviatorBugException;
import com.fortify.cli.aviator.config.ExtensionsConfig;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.util.FileTypeLanguageMapperUtil;
import com.fortify.cli.aviator.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AviatorConfigManager {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorConfigManager.class);
    private static final String EXTENSIONS_CONFIG_RESOURCE = "extensions_config.yaml";
    private static final String DEFAULT_TAG_MAPPING_RESOURCE = "default_tag_mapping.yaml";

    private static volatile AviatorConfigManager instance;
    private static final Object lock = new Object();

    private final ExtensionsConfig extensionsConfig;
    private final TagMappingConfig defaultTagMappingConfig;

    private AviatorConfigManager() {
        LOG.debug("Initializing AviatorConfigManager...");
        this.extensionsConfig = ResourceUtil.loadYamlResource(EXTENSIONS_CONFIG_RESOURCE, ExtensionsConfig.class);
        this.defaultTagMappingConfig = ResourceUtil.loadYamlResource(DEFAULT_TAG_MAPPING_RESOURCE, TagMappingConfig.class);

        if (this.extensionsConfig != null) {
            FileTypeLanguageMapperUtil.initializeConfig(this.extensionsConfig);
            LOG.debug("FileTypeLanguageMapperUtil initialized.");
        } else {
            LOG.error("ExtensionsConfig is null, FileTypeLanguageMapperUtil cannot be initialized properly.");
        }
        LOG.debug("AviatorConfigManager initialized successfully.");
    }

    public static AviatorConfigManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new AviatorConfigManager();
                }
            }
        }
        return instance;
    }

    public ExtensionsConfig getExtensionsConfig() {
        if (extensionsConfig == null) {
            LOG.error("ExtensionsConfig was not loaded. This indicates a bug.");
            throw new AviatorBugException("Critical: ExtensionsConfig not loaded.");
        }
        return extensionsConfig;
    }

    public TagMappingConfig getDefaultTagMappingConfig() {
        if (defaultTagMappingConfig == null) {
            LOG.error("DefaultTagMappingConfig was not loaded. This indicates a bug.");
            throw new AviatorBugException("Critical: DefaultTagMappingConfig not loaded.");
        }
        return defaultTagMappingConfig;
    }
}