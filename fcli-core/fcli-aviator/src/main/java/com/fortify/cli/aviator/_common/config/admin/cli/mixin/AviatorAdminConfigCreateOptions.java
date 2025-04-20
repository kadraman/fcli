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
package com.fortify.cli.aviator._common.config.admin.cli.mixin;

import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class AviatorAdminConfigCreateOptions {
    @Option(names = {"--url", "-u"}, required = true, order=1)
    @MaskValue(sensitivity = LogSensitivityLevel.low, description = "AVIATOR HOST NAME", pattern = MaskValue.URL_HOSTNAME_PATTERN)
    @Getter private String aviatorUrl;

    @Option(names = {"--tenant", "-t"}, required = true, order=2)
    @MaskValue(sensitivity = LogSensitivityLevel.low, description = "AVIATOR TENANT")
    @Getter private String tenant;

    @Mixin @Getter private AviatorAdminPrivateKeyResolverMixin privateKeyResolver;
}