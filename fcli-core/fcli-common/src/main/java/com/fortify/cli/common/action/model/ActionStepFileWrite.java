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

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'write' step.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStepFileWrite extends AbstractActionStep {
    @JsonPropertyDescription("Required SpEL template expression: Specify where to write the given data; either 'stdout', 'stderr' or a filename.")
    @JsonProperty(required = true) private TemplateExpression to;
    
    @JsonPropertyDescription("SpEL template expression: Value to be written to the given output. Required if 'fmt' is not specified, otherwise defaults to '#root' to allow the formatter to access all action variables")
    @JsonProperty(required = false) private TemplateExpression value;
    
    @JsonPropertyDescription("Optional string: Format the value specified through 'value' using the given formatter.")
    @JsonProperty(required = false) private String fmt;
    
    public void postLoad(Action action) {
        Action.checkNotNull("write to", to, this);
        Action.throwIf(value==null && StringUtils.isBlank(fmt), this, ()->"Either 'value', 'fmt', or both need to be specified");
    }
}