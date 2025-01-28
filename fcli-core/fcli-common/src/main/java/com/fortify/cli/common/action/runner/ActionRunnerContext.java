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
package com.fortify.cli.common.action.runner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepCheck.CheckStatus;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.spring.expression.IConfigurableSpelEvaluator;
import com.fortify.cli.common.spring.expression.ISpelEvaluator;
import com.fortify.cli.common.util.StringUtils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * This class holds action execution context
 * @author Ruud Senden
 */
@Getter @Builder
public class ActionRunnerContext {
    /** Jackson {@link ObjectMapper} used for various JSON-related operations */
    private final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    /** Jackson {@link ObjectMapper} used for formatting steps in logging/exception messages */
    private final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    /** Action runner configuration, provided through builder method */
    private final ActionRunnerConfig config;
    /** Progress writer, provided through builder method */
    private final IProgressWriterI18n progressWriter;
    /** ObjectNode holding parameter values as generated by ActionParameterProcessor */
    private final ObjectNode parameterValues;
    /** Check statuses */
    private final Map<String, CheckStatus> checkStatuses = new LinkedHashMap<>(); 
    
    // We need to delay writing output to console as to not interfere with progress writer
    private final List<Runnable> delayedConsoleWriterRunnables = new ArrayList<>();
    /** Save original stdout for delayed output operations */
    private final PrintStream stdout = System.out;
    /** Save original stderr for delayed output operations */
    private final PrintStream stderr = System.err;
    @Setter @Builder.Default private int exitCode = 0;
    @Setter @Builder.Default boolean exitRequested = false;
    
    /** Modifiable map with Request helpers, either configured on {@link #config} or added during action execution */
    @Getter(value=AccessLevel.PRIVATE, lazy=true) private final Map<String, IActionRequestHelper> requestHelpers = initializeRequestHelpers();
    /** Factory for creating the single {@link ISpelEvaluator} instance. By using a factory, we can
     *  check for illegal access to the {@link ISpelEvaluator} during configuration phase. */
    @Getter(AccessLevel.NONE) private final ActionConfigSpelEvaluatorFactory spelEvaluatorFactory = new ActionConfigSpelEvaluatorFactory(this);
    
    private final Map<String, IActionRequestHelper> initializeRequestHelpers() {
        var result = new HashMap<String, IActionRequestHelper>();
        result.putAll(config.getRequestHelpers());
        return result;
    }
    
    public final void addRequestHelper(String name, IActionRequestHelper requestHelper) {
        getRequestHelpers().put(name, requestHelper);
    }
    
    public final IActionRequestHelper getRequestHelper(String name) {
        var requestHelpers = getRequestHelpers();
        if ( StringUtils.isBlank(name) ) {
            if ( requestHelpers.size()==1 ) {
                return requestHelpers.values().iterator().next();
            } else {
                throw new IllegalStateException(String.format("Required 'from:' property (allowed values: %s) missing", requestHelpers.keySet()));
            }
        } 
        var result = requestHelpers.get(name);
        if ( result==null ) {
            throw new IllegalStateException(String.format("Invalid 'from: %s', allowed values: %s", name, requestHelpers.keySet()));
        }
        return result;
    }
    
    public final IConfigurableSpelEvaluator getSpelEvaluator() {
        return spelEvaluatorFactory.getSpelEvaluator();
    }
    
    @RequiredArgsConstructor
    private static final class ActionConfigSpelEvaluatorFactory extends AbstractSpelEvaluatorFactory {
        private final ActionRunnerContext actionRunnerContext;
        
        protected final void configureSpelContext(SimpleEvaluationContext spelContext) {
            var config = actionRunnerContext.getConfig();
            configureSpelContext(spelContext, config.getActionConfigSpelEvaluatorConfigurers(), config);
            configureSpelContext(spelContext, config.getActionContextSpelEvaluatorConfigurers(), actionRunnerContext);
            spelContext.setVariable("action", new ActionUtil(actionRunnerContext));
        }
    }
    
    @Reflectable @RequiredArgsConstructor
    private static final class ActionUtil {
        private final ActionRunnerContext ctx;
        @SuppressWarnings("unused")
        public final String copyParametersFromGroup(String group) {
            StringBuilder result = new StringBuilder();
            for ( var p : ctx.getConfig().getAction().getParameters() ) {
                if ( group==null || group.equals(p.getGroup()) ) {
                    var val = ctx.getParameterValues().get(p.getName());
                    if ( val!=null && StringUtils.isNotBlank(val.asText()) ) {
                        result
                          .append("\"--")
                          .append(p.getName())
                          .append("=")
                          .append(val.asText())
                          .append("\" ");
                    }
                }
            }
            return result.toString();
        }
    }
}
