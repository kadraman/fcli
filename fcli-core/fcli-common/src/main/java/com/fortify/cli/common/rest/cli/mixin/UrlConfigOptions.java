/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMaskHelper.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;

import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Configure connection options to a remote system
 * </pre>
 * @author Ruud Senden
 */
public class UrlConfigOptions extends ConnectionConfigOptions implements IUrlConfig {
    @Option(names = {"--url"}, required = true, order=1)
    @MaskValue(sensitivity = LogSensitivityLevel.low, description = "HOST NAME", pattern = LogMaskHelper.URL_HOSTNAME_PATTERN)
    @Getter private String url;
    
    public boolean hasUrlConfig() {
        return url!=null;
    }
}
