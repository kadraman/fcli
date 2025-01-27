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

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.ActionStepCheck;
import com.fortify.cli.common.action.model.ActionStepCheck.CheckStatus;
import com.fortify.cli.common.action.runner.processor.ActionAddRequestTargetsProcessor;
import com.fortify.cli.common.action.runner.processor.ActionParameterProcessor;
import com.fortify.cli.common.action.runner.processor.ActionStepsProcessor;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

import lombok.RequiredArgsConstructor;

// TODO Move processing of each descriptor element into a separate class,
//      either for all elements or just for step elements.
//      For example, each of these classes could have a (static?) 
//      process(context, descriptor element), with context providing access
//      to ActionRunner fields, parent steps, local data, shared methods like 
//      setDataValue(), ...
@RequiredArgsConstructor
public class ActionRunner implements AutoCloseable {
    private final ActionRunnerConfig config;
    
    public final Integer run(String[] args) {
        return _run(args).get();
    }
    
    public final Supplier<Integer> _run(String[] args) {
        try ( var progressWriter = config.getProgressWriterFactory().overrideAutoIfNoConsole(ProgressWriterType.none) ) {
            var parameterValues = getParameterValues(args, progressWriter);
            var ctx = createContext(progressWriter, parameterValues);
            initializeCheckStatuses(ctx);
            ctx.getProgressWriter().writeProgress("Processing action parameters");
            ActionRunnerData data = new ActionRunnerData(ctx.getSpelEvaluator(), ctx.getParameterValues());
            new ActionAddRequestTargetsProcessor(ctx, data).addRequestTargets();
            ctx.getProgressWriter().writeProgress("Processing action steps");
            new ActionStepsProcessor(ctx, data).processSteps(config.getAction().getSteps());
            ctx.getProgressWriter().writeProgress("Action processing finished");
         
            return ()->{
                ctx.getDelayedConsoleWriterRunnables().forEach(Runnable::run);
                if ( !ctx.getCheckStatuses().isEmpty() ) {
                    ctx.getCheckStatuses().entrySet().forEach(
                        e-> printCheckResult(ctx, e.getValue(), e.getKey()));
                    var overallStatus = CheckStatus.combine(ctx.getCheckStatuses().values());
                    ctx.getStdout().println("Status: "+overallStatus);
                    if ( ctx.getExitCode()==0 && overallStatus==CheckStatus.FAIL ) {
                        ctx.setExitCode(100);
                    }
                }
                return ctx.getExitCode();
            };
        }
    }

    private ActionRunnerContext createContext(IProgressWriterI18n progressWriter, ObjectNode parameterValues) {
        return ActionRunnerContext.builder()
                .config(config)
                .progressWriter(progressWriter)
                .spelEvaluator(config.getSpelEvaluator())
                .parameterValues(parameterValues)
                .build();
    }

    private ObjectNode getParameterValues(String[] args, IProgressWriterI18n progressWriter) {
        var parameterValues = ActionParameterProcessor.builder()
                .config(config)
                .progressWriter(progressWriter)
                .spelEvaluator(config.getSpelEvaluator())
                .build()
                .parseParameterValues(args);
        return parameterValues;
    }
    
    private static final void initializeCheckStatuses(ActionRunnerContext ctx) {
        for ( var elt : ctx.getConfig().getAction().getAllActionElements() ) {
            if ( elt instanceof ActionStepCheck ) {
                var checkStep = (ActionStepCheck)elt;
                var displayName = checkStep.getDisplayName();
                var value = CheckStatus.combine(ctx.getCheckStatuses().get(displayName), checkStep.getIfSkipped());
                ctx.getCheckStatuses().put(displayName, value);
            }
        }
    }

    private static final void printCheckResult(ActionRunnerContext ctx, CheckStatus status, String displayName) {
        if ( status!=CheckStatus.HIDE ) {
            // Even when flushing, output may appear in incorrect order if some 
            // check statuses are written to stdout and others to stderr.
            //var out = status==CheckStatus.PASS?stdout:stderr;
            var out = ctx.getStdout();
            out.println(String.format("%s: %s", status, displayName));
            //out.flush();
        }
    }

    public final void close() {
        config.getRequestHelpers().values().forEach(IActionRequestHelper::close);
    }
}
