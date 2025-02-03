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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

/**
 * This class describes a single action step element, which may contain 
 * requests, progress message, and/or set instructions. This class is 
 * used for both top-level step elements, and step elements in forEach elements. 
 * @author Ruud Senden
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStep extends AbstractActionStep {
    // Partial description for instructions that set variables like 'var.set' and 'var.fmt'
    public static final String VAR_SET_NAME_FORMAT = """
        This step can either set/replace a single-value variable, set/replace a property on \
        a variable containing a set of properties, or append a value to an array-type variable. \
        Following are some examples that show how to specify the operation to perform, and \
        thereby implicitly declaring the variable type:
        
        # Set/replace the single-value variable named 'var1'
        var1: ...
        
        # Set/replace properties 'prop1' and 'prop2' on variable 'var2'
        var2.prop1: ...
        var2.prop2: ...
        
        # Append two items to the array-type variable 'var3' (two trailing dots) 
        var3..: ...
        var3..: ...
        
        # The following would be illegal, as a variable cannot contain both
        # a set of properties and an array:
        var4.prop1: ...
        var4..: ...
        
        Due to this syntax, variable names cannot contain dots like 'var.1', as '1' would be \
        interpreted as a property name on the 'var' variable. Property names may contain dots \
        though, so 'var5.x.y' would be intepreted as property name 'x.y' on 'var5'.
        
        Within a single 'var.set' or 'var.fmt' step, variables are processed in the order that \
        they are declared, allowing earlier declared variables to be referenced by variables or \
        formatters that are declared later.
        """;
    
    @JsonPropertyDescription("""
        Add REST request targets for use in 'rest.call' steps. This step takes a map, with \
        keys defining REST target names, and values defining the REST target definition. 
        """)
    @JsonProperty(value = "rest.target", required = false) private Map<String, ActionStepRestTarget> restTargets;
    
    @JsonPropertyDescription("""
        Execute one or more REST calls. This step takes a list, with entries defining the request data \
        and how to process the response. 
        Note that multiple REST calls defined within a single 'rest.call' instruction are executed \
        independent of each other, so they cannot reference each other's output. For example, if \
        the target system supports bulk requests (like SSC), multiple requests within a single \
        'rest.call' instruction may be combined into a single bulk request, so none of the REST \
        responses will be available yet while building the bulk request. If you need to use the \
        output from one REST call as input for another REST call, these REST calls should be invoked \
        through separate 'rest.call' instructions.
        """)
    @JsonProperty(value = "rest.call", required = false) private List<ActionStepRestCall> restCalls;
    
    @JsonPropertyDescription("Execute one or more fcli commands. For now, only fcli commands that support the standard output options (--output/--store/--to-file) may be used, allowing the JSON output of those commands to be used in subsequent or nested steps. Any console output is suppressed, and any non-zero exit codes will produce an error.")
    @JsonProperty(value = "run.fcli", required = false) private List<ActionStepRunFcli> runFcli;
    
    @JsonPropertyDescription("Write a progress message.")
    @JsonProperty(value = "log.progress", required = false) private TemplateExpression logProgress;
    
    @JsonPropertyDescription("Write a warning message to console and log file (if enabled). Note that warning messages will be shown on console only after all action steps have been executed, to not interfere with progress messages.")
    @JsonProperty(value = "log.warn", required = false) private TemplateExpression logWarn;
    
    @JsonPropertyDescription("Write a debug message to log file (if enabled).")
    @JsonProperty(value = "log.debug", required = false) private TemplateExpression logDebug;
    
    @JsonPropertyDescription("""
            Set one or more variables values for use in later action steps. This step takes a map with keys \
            specifying the variable to set or update, and values specifying the value to set. Both keys and \
            values accept SpEL template expressions. Note that there's also a 'var.fmt' step that allows for \
            setting values generated by a formatter.
            
            """+VAR_SET_NAME_FORMAT)
    @JsonProperty(value = "var.set", required = false) private LinkedHashMap<TemplateExpression,TemplateExpression> varSet;
    
    @JsonPropertyDescription("""
            Set one or more variables for use in later action steps with the output of the given formatter. \
            This step takes a map with keys specifying the variable to set or update, and values specifying \
            the formatter to use as defined in the 'formatters' section. Both keys and values accept SpEL \
            template expressions.
            
            """+VAR_SET_NAME_FORMAT)
    @JsonProperty(value = "var.fmt", required = false) private LinkedHashMap<TemplateExpression,TemplateExpression> varFmt;
    
    @JsonPropertyDescription("Unset variables, supports SpEL template expressions to specify the variable names to be unset.")
    @JsonProperty(value = "var.unset", required = false) private List<TemplateExpression> varUnset;
    
    @JsonPropertyDescription("Write data to a file, stdout, or stderr. Note that output to stdout and stderr will be deferred until action termination as to not interfere with progress messages.")
    @JsonProperty(value="file.write", required = false) private List<ActionStepFileWrite> fileWrite;
    
    @JsonPropertyDescription("""
        Mostly used for security policy and similar actions to define PASS/FAIL criteria. Upon action termination, \
        check results will be written to console and return a non-zero exit code if the outcome of one or more checks \
        was FAIL. This instructions takes a map, with keys defining the check name, and values defining the check \
        definition. Current check status can be accessed through ${checkStatus.checkName}, for example allowing to \
        conditionally execute additional checks based on earlier check outcome. Note that if the same check name \
        (map key) is used in different 'check' steps, they will be treated as separate checks, and ${checkStatus.checkName} \
        will contain the status of the last executed check for the given check name.
        """)
    @JsonProperty(value = "check", required = false) private Map<String, ActionStepCheck> check;
    
    @JsonPropertyDescription("Iterate over a given array of values.")
    @JsonProperty(value = "forEach", required = false) private ActionStepForEach forEach;
    
    @JsonPropertyDescription("Sub-steps to be executed; useful for grouping or conditional execution of multiple steps.")
    @JsonProperty(value = "steps", required = false) private List<ActionStep> steps;
    
    @JsonPropertyDescription("Throw an exception, thereby terminating action execution.")
    @JsonProperty(value = "throw", required = false) private TemplateExpression _throw;
    
    @JsonPropertyDescription("Terminate action execution and return the given exit code.")
    @JsonProperty(value = "exit", required = false) private TemplateExpression _exit;
    
    /**
     * This method is invoked by the parent element (which may either be another
     * step element, or the top-level {@link Action} instance).
     * It invokes the postLoad() method on each request descriptor.
     */
    public final void postLoad(Action action) {
        checkInstructionCount();
    }

    private void checkInstructionCount() {
        var nonNullInstructionNames = getNonNullInstructionNames();
        if ( nonNullInstructionNames.size()==0 ) {
            throw new ActionValidationException("Action step doesn't define any instruction");
        }
        if ( nonNullInstructionNames.size()>1 ) {
            throw new ActionValidationException("Action step contains multiple instructions: "+nonNullInstructionNames);
        }
    }

    @SneakyThrows
    private ArrayList<String> getNonNullInstructionNames() {
        var nonNullInstructions = new ArrayList<String>(); 
        // Note that 'if' is defined in parent class, so not included by getDeclaredFields()
        for ( var f : this.getClass().getDeclaredFields() ) {
            var jsonPropertyAnnotation = f.getAnnotation(JsonProperty.class);
            if ( jsonPropertyAnnotation!=null && f.get(this)!=null ) {
                nonNullInstructions.add(getInstructionName(f, jsonPropertyAnnotation));
            }
        }
        return nonNullInstructions;
    }

    private String getInstructionName(Field field, JsonProperty jsonPropertyAnnotation) {
        var nameFromAnnotation = jsonPropertyAnnotation.value();
        return StringUtils.isBlank(nameFromAnnotation) ? field.getName() : nameFromAnnotation;
    }
    
    
}