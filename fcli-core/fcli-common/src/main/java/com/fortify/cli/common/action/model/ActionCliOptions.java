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
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes a action parameter.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionCliOptions implements IActionElement {
    @JsonPropertyDescription("""
        Required string: Action parameter description to be shown in action usage help.    
        """)
    @JsonProperty(value = "description", required = true) private String description;
    
    @JsonPropertyDescription("""
        Optional string: This will allow the action to also accept a CLI option named \
        `--[alias]` or `-[alias]` for single-letter aliases. Note that only option \
        name, not alias, may be referenced through ${cli.*} expressions.
        """)
    @JsonProperty(value = "alias", required = false) private String alias;
    
    @JsonPropertyDescription("""
        Optional string: Action parameter type: string (default), boolean, int, long, double, float, or array.
        """)
    @JsonProperty(value = "type", required = false) private String type;
        
    @JsonPropertyDescription("""
        Optional SpEL template expression: Default value for this CLI option if no value is specified by the user. \
        For example, this can be used to read a default value from an environment variable using ${#env('ENV_NAME')}  
        """)
    @JsonProperty(value = "default", required = false) private TemplateExpression defaultValue;
    
    @JsonPropertyDescription("""
        Optional boolean: All parameters are required by default, unless this property is set to false.    
        """)
    @JsonProperty(value = "required", required = false, defaultValue = "true") private boolean required = true;
    
    @JsonPropertyDescription("""
        Optional string: Allows for defining groups of parameters, which can for example be used with \
        ${#action.copyParametersFromGroup("optionGroupName")} 
        """)
    @JsonProperty(value = "group", required = false) private String group;
    
    public final void postLoad(Action action) {
        Action.checkNotNull("CLI option description", getDescription(), this);
        // TODO Check no duplicate option names; ideally ActionRunner should also verify
        //      that option names/aliases don't conflict with command options
        //      like --help/-h, --log-file, ...
    }
}