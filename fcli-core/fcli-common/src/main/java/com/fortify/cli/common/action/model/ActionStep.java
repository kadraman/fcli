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

import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a single action step element, which may contain 
 * requests, progress message, and/or set instructions. This class is 
 * used for both top-level step elements, and step elements in forEach elements. 
 * @author Ruud Senden
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStep extends AbstractActionStep {
    @JsonPropertyDescription("Optional list: Add target URLs and related properties for REST requests.")
    @JsonProperty(required = false) private List<ActionStepAddRequestTarget> addRequestTargets;
    
    @JsonPropertyDescription("Optional list: Execute one or more REST requests.")
    @JsonProperty(required = false) private List<ActionStepRequest> requests;
    
    @JsonPropertyDescription("Optional list: Execute one or more fcli commands. For now, only fcli commands that support the standard output options (--output/--store/--to-file) may be used, allowing the JSON output of those commands to be used in subsequent or nested steps. Any console output is suppressed, and any non-zero exit codes will produce an error.")
    @JsonProperty(required = false) private List<ActionStepFcli> fcli;
    
    @JsonPropertyDescription("Optional SpEL template expression: Write a progress message.")
    @JsonProperty(required = false) private TemplateExpression progress;
    
    @JsonPropertyDescription("Optional SpEL template expression: Write a warning message to console and log file (if enabled). Note that warning messages will be shown on console only after all action steps have been executed, to not interfere with progress messages.")
    @JsonProperty(required = false) private TemplateExpression warn;
    
    @JsonPropertyDescription("Optional SpEL template expression: Write a debug message to log file (if enabled).")
    @JsonProperty(required = false) private TemplateExpression debug;

    @JsonPropertyDescription("Optional SpEL template expression: Throw an exception, thereby terminating action execution.")
    @JsonProperty(value = "throw", required = false) private TemplateExpression _throw;
    
    @JsonPropertyDescription("Optional SpEL template expression: Terminate action execution and return the given exit code.")
    @JsonProperty(value = "exit", required = false) private TemplateExpression _exit;
    
    @JsonPropertyDescription("""
        Optional map: Set variables for use in subsequent steps. Both keys and values may be \
        specified as SpEL template expressions. Variables can either contain a single value, \
        a set of properties, or an array, based on the following formats for the map keys:
          <variable name> - Single value variable
          <variable name>.<property name> - Variable containing a set of properties   
          <variable name>.. - Variable containing an array of values
        For example:
          var1: val1        # Set variable 'var1' to 'val1'
          var2.prop1: val2  # Set 'prop1' in 'var2' to 'val2'
          var2.prop2: val3  # Add second property in 'var2'
          var3..: val3      # Append 'val3' to the 'var' array
        Only a single format is supported for a given variable, for example the following fails:
          var4.prop1: val1  # Defines 'var4' as a set of properties
          var4..: val2      # Fails because var4 contains a set of properties, not an array
        Variables are set in the order that they are declared, so the following is supported:
          var.set:
            var5: x         # Set variable `var5` to 'x'
            var6.${var5}: y # Set property 'x' in 'var6' to 'y'
            var7: ${var6.x} # Set variable 'var7' to 'y'
    """)
    @JsonProperty(value = "var.set", required = false) private LinkedHashMap<TemplateExpression,TemplateExpression> varSet;
    
    @JsonPropertyDescription("Optional list: Unset variables, supports SpEL template expressions to specify the variable names to be unset.")
    @JsonProperty(value = "var.unset", required = false) private List<TemplateExpression> varUnset;
    
    @JsonPropertyDescription("Optional list: Write data to a file, stdout, or stderr. Note that output to stdout and stderr will be deferred until action termination as to not interfere with progress messages.")
    @JsonProperty(value="file.write", required = false) private List<ActionStepFileWrite> fileWrite;
    
    @JsonPropertyDescription("Optional object: Iterate over a given array of values.")
    @JsonProperty(required = false) private ActionStepForEach forEach;
    
    @JsonPropertyDescription("Optional list: Mostly used for security policy and similar actions to define PASS/FAIL criteria. Upon action termination, check results will be written to console and return a non-zero exit code if the outcome of on or more checks was FAIL.")
    @JsonProperty(required = false) private List<ActionStepCheck> check;
    
    @JsonPropertyDescription("Optional list: Sub-steps to be executed; useful for grouping or conditional execution of multiple steps.")
    @JsonProperty(required = false) private List<ActionStep> steps;
    
    /**
     * This method is invoked by the parent element (which may either be another
     * step element, or the top-level {@link Action} instance).
     * It invokes the postLoad() method on each request descriptor.
     */
    public final void postLoad(Action action) {}
}