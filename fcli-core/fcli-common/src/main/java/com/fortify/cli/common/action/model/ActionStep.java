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
public final class ActionStep extends AbstractActionElementIf {
    // Partial description for instructions that set variables like 'var.set' and 'var.fmt'
    private static final String VAR_SET = """
        This step takes a list of variables to set, with each list item taking a single yaml property that \
        represents the variable name to set, which may be specified as an SpEL template expression. Based on \
        the format of the variable name, this step can either set/replace a single-value variable, \
        set/replace a property on a variable containing a set of properties, or append a value to an array-type \
        variable. Following are some examples that show how to specify the operation to perform, and thereby \
        implicitly declaring the variable type:
        
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
        
        Values may be specified as either an SpEL template expression, or as 'value' and 'fmt' \
        properties. An 'if' property is also supported to conditionally set the variable. If \
        formatter is specified, the given formatter from the 'formatters' section will be used to \
        format the given value, or, if no value is given, the formatter will be evaluated against \
        the set of all variables. Some examples:
        
        simpleValue1: Hello ${name}
        formattedValue1: {fmt: myFormatter, if: "${someExpression}"}
        formatterValue2: {value: "${myVar}", fmt: "${myVarFormatterExpression}"}     
        
        Within a single 'var.set*' step, variables are processed in the order that they are \
        declared, allowing earlier declared variables to be referenced by variables or \
        formatters that are declared later in the same step.
        """;
    
    @JsonPropertyDescription("Set one or more variables values for use in later action steps."+VAR_SET)
    @JsonProperty(value = "var.set", required = false) private LinkedHashMap<TemplateExpression,TemplateExpressionWithFormatter> varSet;
    
    @JsonPropertyDescription("""
        Remove one or more variables. Variable names to remove can be provided as plain text or \
        as a SpEL template expression.
        """)
    @JsonProperty(value = "var.rm", required = false) private List<TemplateExpression> varRemove;
    
    @JsonPropertyDescription("""
        Set one or more global variables that can be accessed by both this action, and any other \
        actions that are executed within the same fcli invocation. For example, if action 1 uses the \
        'run.fcli' step to run action 2, any global variables set by action 2 can also be accessed by \
        later steps in action 1. Global variables can be accessed through ${global.varName}.
        
        """+VAR_SET)
    @JsonProperty(value = "var.set-global", required = false) private LinkedHashMap<TemplateExpression,TemplateExpressionWithFormatter> varSetGlobal;
        
    @JsonPropertyDescription("""
        Remove one or more global variables. Variable names to remove can be provided as plain text or \
        as a SpEL template expression.
        """)
    @JsonProperty(value = "var.rm-global", required = false) private List<TemplateExpression> varRemoveGlobal;
    
    @JsonPropertyDescription("Write a progress message.")
    @JsonProperty(value = "log.progress", required = false) private TemplateExpression logProgress;
    
    @JsonPropertyDescription("""
        Write a warning message to console and log file (if enabled). Note that depending on the \
        config:output setting, warning messages may be shown either immediately, or only after all \
        action steps have been executed, to not interfere with progress messages.
        """)
    @JsonProperty(value = "log.warn", required = false) private TemplateExpression logWarn;
    
    @JsonPropertyDescription("Write a debug message to log file (if enabled).")
    @JsonProperty(value = "log.debug", required = false) private TemplateExpression logDebug;
    
    @JsonPropertyDescription("""
        Add REST request targets for use in 'rest.call' steps. This step takes a map, with \
        keys defining REST target names, and values defining the REST target definition. 
        """)
    @JsonProperty(value = "rest.target", required = false) private Map<String, ActionStepRestTarget> restTargets;
    
    @JsonPropertyDescription("""
        Execute one or more REST calls. This step takes a map, with keys defining an indentifier \
        for the REST call, and values defining the request data and how to process the response. \
        For paged REST requests, a single 'rest.call' instruction will execute multiple REST \
        requests to load the individual pages. The response of each individual REST request \
        will be stored as local action variables. For example, given a rest.call identifier 'x' \
        the following local action variables will be set:
        
        x: The processed response
        x_raw: The raw, unprocessed response
        x_exception: Java Exception instance if the request failed
        
        These variables can be referenced only within the current 'rest.call' map entry, for \
        example by 'log.progress', 'on.success', and 'records.for-each'. They are not accessible 
        by later steps or other map entries within the same 'rest.call' step. If you wish to make \
        any data produced by the REST call available to later steps, you'll need to use 'var.*' \
        steps in either 'on.success' or 'records.for-each' instructions.
         
        Note that multiple REST calls defined within a single 'rest.call' step will be executed \
        in the specified order, but the requests are built independent of each other. As such, \
        within a single 'rest.call' step, variables set by one 'rest.call' map entry cannot be \
        accessed in the request definition (uri, query, body, ...) of another map entry. The \
        reason is that for target systems that supports bulk requests (like SSC), multiple \
        requests within a single 'rest.call' instruction may be combined into a single bulk \
        request, so none of the REST responses will be available yet while building the bulk \
        request. If you need to use the output from one REST call as input for another REST \
        call, these REST calls should be defined in separate 'rest.call' steps.
        """)
    @JsonProperty(value = "rest.call", required = false) private LinkedHashMap<String, ActionStepRestCall> restCalls;
    
    @JsonPropertyDescription("""
        Execute one or more fcli commands. This step takes a map, with map keys defining an identifier \
        for the fcli invocation, and values defining the fcli command to run and how to process the \
        output and exit code. The identifier can be used in later steps (or later fcli invocations in \
        the same 'run.fcli' step) to access the output of the fcli command, like stdout, stderr, and \
        exit code that were produced by the fcli command, depending on step configuration. For example, 
        given an fcli invocation identifier named 'x', the following action variables may be set: 
        
        x: Array of records produced by the fcli invocation if 'records.collect' is set to 'true'
        x_stdout: Output produced on stdout by the fcli invocation if 'stdout' is set to 'collect'
        x_stderr: Output produced on stderr by the fcli invocation if 'stderr' is set to 'collect'
        x_exitCode: Exit code of the fcli invocation
        x_exception: Java Exception instance if fcli invocation threw an exception
        """)
    @JsonProperty(value = "run.fcli", required = false) private LinkedHashMap<String, ActionStepRunFcli> runFcli;
    
    @JsonPropertyDescription("""
        Write data to a file, stdout, or stderr. This step takes a map, with map keys defining the destination, \
        and map values defining the data to write to the destination. Destination can be specified as either \
        stdout, stderr, or a file name. If a file already exists, it will be overwritten. Note that depending \
        on the config:output setting, data written to stdout or stderr may be shown either immediately, or only \
        after all action steps have been executed, to not interfere with progress messages.
        
        Map values may be specified as either an SpEL template expression, or as 'value' and 'fmt' \
        properties. An 'if' property is also supported to conditionally write to the output. If \
        formatter is specified, the given formatter from the 'formatters' section will be used to \
        format the given value, or, if no value is given, the formatter will be evaluated against \
        the set of all variables. Some examples:
        
        /path/to/myFile1: Hello ${name}
        /path/to/myFile2: {fmt: myFormatter, if: "${someExpression}"}
        /path/to/myFile3: {value: "${myVar}", fmt: "${myVarFormatterExpression}"}     
        """)
    @JsonProperty(value="out.write", required = false) private LinkedHashMap<TemplateExpression, TemplateExpressionWithFormatter> outWrite;
    
    @JsonPropertyDescription("""
        Mostly used for security policy and similar actions to define PASS/FAIL criteria. Upon action termination, \
        check results will be written to console and return a non-zero exit code if the outcome of one or more checks \
        was FAIL. This instructions takes a map, with keys defining the check name, and values defining the check \
        definition. Current check status can be accessed through ${checkStatus.checkName}, for example allowing to \
        conditionally execute additional checks based on earlier check outcome. Note that if the same check name \
        (map key) is used in different 'check' steps, they will be treated as separate checks, and ${checkStatus.checkName} \
        will contain the status of the last executed check for the given check name.
        """)
    @JsonProperty(value = "check", required = false) private LinkedHashMap<String, ActionStepCheck> check;
    
    @JsonPropertyDescription("""
        Execute the steps defined in the 'do' block for every record provided by the 'from' expression.    
        """)
    @JsonProperty(value = "records.for-each", required = false) private ActionStepForEachRecord forEachRecord;
    
    @JsonPropertyDescription("""
        Sub-steps to be executed; useful for grouping or conditional execution of multiple steps.    
        """)
    @JsonProperty(value = "steps", required = false) private List<ActionStep> steps;
    
    @JsonPropertyDescription("""
        Throw an exception, thereby terminating action execution.
        """)
    @JsonProperty(value = "throw", required = false) private TemplateExpression _throw;
    
    @JsonPropertyDescription("""
        Terminate action execution and return the given exit code.
        """)
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
            throw new FcliActionValidationException("Action step doesn't define any instruction");
        }
        if ( nonNullInstructionNames.size()>1 ) {
            throw new FcliActionValidationException("Action step contains multiple instructions: "+nonNullInstructionNames);
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