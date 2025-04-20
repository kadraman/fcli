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
package com.fortify.cli.ssc.access_control.helper;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Reflectable @NoArgsConstructor 
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SSCTokenGetOrCreateResponse {
    private SSCTokenGetOrCreateResponse.SSCTokenData data;
    @Data 
    @Reflectable @NoArgsConstructor
    public static final class SSCTokenData {
        private String id;
        private Date terminalDate;
        private Date creationDate;
        private String type;
        @MaskValue(sensitivity = LogSensitivityLevel.high, description = "SSC TOKEN")
        private char[] token;
        //private String _href;
    }
}