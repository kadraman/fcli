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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'with:session' step, making a session available for the
 * steps specified in the do-block, and cleaning up the session afterwards.
 */
@Reflectable @NoArgsConstructor
@Data
@JsonInclude(Include.NON_NULL)
public final class ActionStepWithWriter implements IActionElement {
    @JsonPropertyDescription("""
        Required SpEL template expression; destination where to write the output of this writer. \
        Destination can be specified as one of the following: 
        
        - A file name to write the output to
        - 'stdout' to write the output to stdout
        - 'stderr' to write the output to stderr
        - 'var.text:varName' to write the output as text into the given 'varName' action variable  
        - 'var.array:varName' to write the output as an array into the given 'varName' action variable
        
        With 'var.text' and 'var.array', the output is available to steps after the current 'with' block \
        has completed through ${varName}. Note that 'var.array' is only support for 'type: json'. 
        """)
    @JsonProperty(value = "to", required = true) private TemplateExpression to;
    
    @JsonPropertyDescription("""
        Required SpEL template expression defining the writer type. The expression must evaluate \
        to one of the following values:
        
        - csv: CSV output with header
        - csv-plain: CSV output without header
        - json: JSON output
        """)
    @JsonProperty(value = "type", required = true) private TemplateExpression type;
    
    @JsonPropertyDescription("""
        Optional map defining options for the given writer type. Different writer types may support \
        different configuration options.
        """) // TODO Add writer option descriptions, together with the writer types that they apply to
    @JsonProperty(value = "options", required = false) private Map<String, TemplateExpression> options;
    
    @Override
    public void postLoad(Action action) {
        // TODO This doesn't seem to get visited; no exception is thrown if one of these fields is not defined
        Action.checkNotNull("to", this, to);
        Action.checkNotNull("type", this, type);
    }
    
    
}