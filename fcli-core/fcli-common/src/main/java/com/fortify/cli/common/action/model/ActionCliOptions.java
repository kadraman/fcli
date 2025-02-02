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
    @JsonPropertyDescription("Required string: Action parameter description to be shown in action usage help.")
    @JsonProperty(required = true) private String description;
    
    @JsonPropertyDescription("Optional string: This will allow the action to also accept a CLI option named `--[alias]` or `-[alias]` for single-letter aliases. Aliases cannot be referenced in SpEL expressions.")
    @JsonProperty(required = false) private String alias;
    
    @JsonPropertyDescription("Optional string: Action parameter type: string (default), boolean, int, long, double, float, or array.")
    @JsonProperty(required = false) private String type;
        
    @JsonPropertyDescription("Optional SpEL template expression: Default value for this action parameter if no value is specified by the user.")
    @JsonProperty(required = false) private TemplateExpression defaultValue;
    
    @JsonPropertyDescription("Optional boolean: All parameters are required by default, unless this property is set to false.")
    @JsonProperty(required = false, defaultValue = "true") private boolean required = true;
    
    @JsonPropertyDescription("Optional string: Allows for defining groups of parameters")
    @JsonProperty(required = false) private String group;
    
    public final void postLoad(Action action) {
        Action.checkNotNull("parameter description", getDescription(), this);
        // TODO Check no duplicate names; ideally ActionRunner should also verify
        //      that option names/aliases don't conflict with command options
        //      like --help/-h, --log-file, ...
    }
}