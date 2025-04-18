/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.action.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.log.LogMaskHelper.LogSensitivityLevel;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes an action option mask.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionInputMask implements IActionElement {
    @JsonPropertyDescription("""
        Optional enum value: Option sensitivity; high/medium/low. Default value: high   
        """)
    @JsonProperty(value = "sensitivity", required = false) private LogSensitivityLevel sensitivityLevel = LogSensitivityLevel.high;
    
    @JsonPropertyDescription("""
        Optional string: Mask description, used to generate the masked output.
        """)
    @JsonProperty(value = "description", required = false) private String description;
    
    @JsonPropertyDescription("""
        Optional string: Pattern for fine-tuning which option value contents should be
        masked. Every matching regex group will be masked using the given description.
        If no pattern is defined, or if the pattern doesn't contain any groups, the
        full option value will be masked.
        """)
    @JsonProperty(value = "pattern", required = false) private String pattern;
    
    public final void postLoad(Action action) {
    }
}