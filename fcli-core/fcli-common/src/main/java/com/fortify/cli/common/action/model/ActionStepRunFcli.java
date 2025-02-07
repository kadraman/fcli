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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.OutputHelper.OutputType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * This class describes a forEach element, allowing iteration over the output of
 * a given input.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStepRunFcli extends AbstractActionElementIf implements IMapStringKeyAware {
    private static final String PARSER_INFO = """
        
        none:  Don't parse output (default)
        json:  Parse output as JSON
        lines: Parse output as an array of lines
        
        This is only meant to be used for fcli commands that don't produce records, \
        i.e., commands that don't provide the standard --output, --store, and --to-file \
        options. For example, it can be used with 'fcli * action run' commands to parse \
        JSON data produced by some fcli action, or with 'fcli tool * run' commands to \
        allow for accessing the individual output lines produced by the tool. Note that \
        any progress messages produced by the fcli command will be automatically suppressed \
        if stdout is being parsed, to avoid progress messages from interfering with \
        JSON parsing or appearing in the parsed lines.  
        
        Parsed output will be stored in action variables named after the 'run.fcli' \
        identifier/map key, for example 'x_stdout_parsed' and 'x_stderr_parsed'. For \
        convenience, if only one of stdout or stderr is being parsed, the parsed output \
        will also be stored in an action variable without those suffixes, for example 'x'.     
        """;
    
    @JsonIgnore private String key;
    
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
        Optional enum value: Allows for parsing fcli stdout output:
        """+PARSER_INFO)
    @JsonProperty(value = "stdout.parse", required = false) private FcliOutputParser stdoutParser = FcliOutputParser.none;
        
    @JsonPropertyDescription("""
        Optional enum value: Allows for parsing fcli stderr output:
        """+PARSER_INFO)
    @JsonProperty(value = "stderr.parse", required = false) private FcliOutputParser stderrParser = FcliOutputParser.none;
    
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
    
    @Reflectable @RequiredArgsConstructor
    public static enum FcliOutputParser {
        none(s->null), 
        json(FcliOutputParser::parseJson), 
        lines(FcliOutputParser::parseLines);
        
        private final Function<String, JsonNode> parser;
        
        public JsonNode parse(String s) {
            return parser.apply(s);
        }
        
        @SneakyThrows
        private static final JsonNode parseJson(String s) {
            if ( StringUtils.isBlank(s) ) { return NullNode.getInstance(); }
            return JsonHelper.getObjectMapper().readTree(s);
        }
        
        private static final JsonNode parseLines(String s) {
            var result = JsonHelper.getObjectMapper().createArrayNode();
            if ( StringUtils.isNotBlank(s) ) {
                Arrays.stream(s.split("\n")).forEach(result::add);
            }
            return result;
        }
    }
}