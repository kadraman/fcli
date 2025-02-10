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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.OutputHelper.OutputType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a forEach element, allowing iteration over the output of
 * a given input.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStepRunFcli extends AbstractActionElementIf implements IMapStringKeyAware {
    @JsonIgnore private String key;
    
    /** Allow for deserializing from a string that specified the fcli command to run, rather than object */
    public ActionStepRunFcli(String cmdString) {
        this.cmd = SpelHelper.parseTemplateExpression(cmdString);
    }
    
    @JsonPropertyDescription("""
        Required SpEL template expression: The fcli command to run. This can be \
        specified with or without the 'fcli' command itself. Some examples:
        
        ssc appversion get --av ${av.id} --embed=attrValuesByName
        
        fcli fod rel ls
        """)
    @JsonProperty(value = "cmd", required = true) private TemplateExpression cmd;
    
    @JsonPropertyDescription("""
        Optional enum value: Specify how to handle output written to stdout by this fcli command:
            
        suppress: Suppress fcli output to stdout
        collect:  Collect stdout output in an action variable, for example x_stdout
        show:     Show fcli stdout output.
        
        Note that depending on the config:output setting, 'show' will either show the output \
        immediately, or output is delayed until action processing has completed.
        
        Default: 'suppress' if output is being processed through another instruction \
        ('records.for-each', 'records.collect', 'stdout.parser'), 'show' otherwise.
        """)
    @JsonProperty(value = "stdout", required = false) private OutputType stdoutOutputType;
    
    @JsonPropertyDescription("""
        Optional enum value: Specify how to handle output written to stderr by this fcli command:
                
        suppress: Suppress fcli output to stderr
        collect:  Collect stderr output in an action variable, for example x_stderr
        show:     Show fcli stderr output
        
        Note that depending on the config:output setting, 'show' will either show the output \
        immediately, or output is delayed until action processing has completed.
        
        Default value: 'show'
        """)
    @JsonProperty(value = "stderr", required = false) private OutputType stderrOutputType;
    
    @JsonPropertyDescription("""
        Optional list: Steps to be executed if the fcli command throws an exception. If not \
        specified, an exception will be thrown and action execution will terminate. Steps can \
        reference a variable named after the identifier for this fcli invocation, for example \
        'x_exception', to access the Java Exception object that represents the failure that occurred.
        """)
    @JsonProperty(value = "on.exception", required = false) private List<ActionStep> onException;   
    
    @JsonPropertyDescription("""
        Optional list: Steps to be executed if the fcli command returns a non-zero exit code. \
        If not specified, an exception will be thrown and action execution will terminate. Steps \
        can reference the usual action variables generated by the fcli invocation to access \
        any records, stdout/stderr output, and exit code produced by the fcli command.
        """)
    @JsonProperty(value = "on.exit-fail", required = false) private List<ActionStep> onNonZeroExitCode;   
    
    @JsonPropertyDescription("""
        Optional boolean: If set to 'true', records produced by this fcli command will \
        be collected and accessible through an action variable named after the 'run.fcli' \
        identifier/map key.
        """)
    @JsonProperty(value = "records.collect", required = false) private boolean collectRecords;
    
    @JsonPropertyDescription("""
        Optional object: For fcli commands that produce records, this allows for running the \
        steps specified in the 'do' instruction for each individual record.
        """)
    @JsonProperty(value = "records.for-each", required = false) private ActionStepRunFcli.ActionStepFcliForEachDescriptor forEachRecord;
    
    /**
     * This method is invoked by the {@link ActionStep#postLoad()}
     * method. It checks that required properties are set, then calls the postLoad() method for
     * each sub-step.
     */
    public final void postLoad(Action action) {
        Action.checkNotNull("fcli cmd", cmd, this);
    }
    
    /**
     * This class describes an fcli forEach element, allowing iteration over the output of
     * the fcli command. 
     */
    @Reflectable @NoArgsConstructor
    @Data @EqualsAndHashCode(callSuper = true)
    public static final class ActionStepFcliForEachDescriptor extends AbstractActionElementForEachRecord {
        protected final void _postLoad(Action action) {}
    }
}