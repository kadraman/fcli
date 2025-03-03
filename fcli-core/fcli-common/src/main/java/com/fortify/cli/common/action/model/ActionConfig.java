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

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes default values for various action properties.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionConfig implements IActionElement {
    @JsonPropertyDescription("""
        Optional string: Default target to use for rest.call steps.    
        """)
    @JsonProperty(value = "rest.target.default", required = false) private String restTargetDefault;
    
    @JsonPropertyDescription("""
        Optional enum value: If set to 'delayed' (default), all output to stdout/stderr except for progress \
        messages will be delayed until the end of action execution. If set to 'immediate', output will be \
        written immediately and progress writer will be configured to 'simple' mode (unless '--progress=none' \
        is specified by the user) to avoid such output from interfering with progress messages.    
        """)
    @JsonProperty(value = "output", required = false) private ActionConfigOutput output = ActionConfigOutput.delayed;
    
    @Override
    public void postLoad(Action action) {}
    
    public enum ActionConfigOutput {
        immediate, delayed
    }
    
    public enum ActionConfigSessionFromEnvOutput {
        show, suppress
    }
}