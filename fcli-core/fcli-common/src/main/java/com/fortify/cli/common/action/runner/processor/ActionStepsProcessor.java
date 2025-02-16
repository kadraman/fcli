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
package com.fortify.cli.common.action.runner.processor;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.action.model.AbstractActionElementForEachRecord;
import com.fortify.cli.common.action.model.ActionConfig.ActionConfigOutput;
import com.fortify.cli.common.action.model.ActionStep;
import com.fortify.cli.common.action.model.ActionStepCheck;
import com.fortify.cli.common.action.model.ActionStepCheck.CheckStatus;
import com.fortify.cli.common.action.model.ActionStepForEachRecord;
import com.fortify.cli.common.action.model.ActionStepForEachRecord.IActionStepForEachProcessor;
import com.fortify.cli.common.action.model.ActionStepRestCall;
import com.fortify.cli.common.action.model.ActionStepRestCall.ActionStepRequestForEachResponseRecord;
import com.fortify.cli.common.action.model.ActionStepRestCall.ActionStepRequestType;
import com.fortify.cli.common.action.model.ActionStepRestCall.ActionStepRestCallLogProgressDescriptor;
import com.fortify.cli.common.action.model.ActionStepRestTarget;
import com.fortify.cli.common.action.model.ActionStepRunFcli;
import com.fortify.cli.common.action.model.ActionStepRunFcli.ActionStepFcliForEachDescriptor;
import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.action.model.IActionStepIfSupplier;
import com.fortify.cli.common.action.model.TemplateExpressionWithFormatter;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerHelper;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.FcliActionStepException;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.ActionRequestDescriptor;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.OutputHelper.Result;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.UnirestException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ActionStepsProcessor {
    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(ActionStepsProcessor.class);
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    
    public final void processSteps(List<ActionStep> steps) {
        if ( steps!=null ) { steps.forEach(this::processStep); }
    }
    
    private final void processStep(ActionStep step) {
        if ( _if(step) ) {
            processStepSupplier(step::getLogProgress, this::processLogProgressStep);
            processStepSupplier(step::getLogWarn, this::processLogWarnStep);
            processStepSupplier(step::getLogDebug, this::processLogDebugStep);
            processStepEntries(step::getRestTargets, this::processRestTargetsStep);
            processStepSupplier(step::getRestCalls, this::processRestCallStep);
            processStepSupplier(step::getVarSet, this::processVarSetStep);
            processStepSupplier(step::getVarRemove, this::processVarRemoveStep);
            processStepSupplier(step::getVarSetGlobal, this::processVarSetGlobalStep);
            processStepSupplier(step::getVarRemoveGlobal, this::processVarRemoveGlobalStep);
            processStepEntries(step::getRunFcli, this::processRunFcliStep);
            processStepEntries(step::getCheck, this::processCheckStep);
            processStepEntries(step::getOutWrite, this::processOutWriteStep);
            processStepSupplier(step::get_throw, this::processThrowStep);
            processStepSupplier(step::get_exit, this::processExitStep);
            processStepSupplier(step::getForEachRecord, this::processForEachRecordStep);
            processStepEntries(step::getSteps, this::processStep);
        }
    }
    
    private <T> void processStepEntries(Supplier<List<T>> supplier, Consumer<T> consumer) {
        var list = supplier.get();
        if ( list!=null ) { list.forEach(value->processStep(value, consumer)); }
    }
    
    private <N,T> void processStepEntries(Supplier<Map<N,T>> supplier, BiConsumer<N,T> consumer) {
        var map = supplier.get();
        if ( map!=null ) { map.entrySet().forEach(e->processStep(e.getKey(), e.getValue(), consumer)); }
    }
    
    private <T> void processStepSupplier(Supplier<T> supplier, Consumer<T> consumer) {
        processStep(supplier.get(), consumer);
    }
    
    private <T> void processStep(T value, Consumer<T> consumer) {
        if ( _if(value) ) {
            String valueString = null;
            if ( LOG.isDebugEnabled() ) {
                valueString = getStepAsString(valueString, value);
                LOG.debug("Start processing:\n"+valueString);
            }
            try {
                consumer.accept(value);
            } catch ( Exception e ) {
                if ( e instanceof FcliActionStepException ) {
                    throw e;
                } else {
                    valueString = getStepAsString(valueString, value);
                    throw new FcliActionStepException("Error processing:\n"+valueString, e);
                }
            }
            if ( LOG.isDebugEnabled() ) {
                valueString = getStepAsString(valueString, value);
                LOG.debug("End processing:\n"+valueString);
            }
        }
    }
    
    private <N,T> void processStep(N name, T value, BiConsumer<N,T> consumer) {
        if ( _if(value) ) {
            String valueString = null;
            if ( LOG.isDebugEnabled() ) {
                valueString = getStepAsString(valueString, value);
                LOG.debug("Start processing:\n"+valueString);
            }
            try {
                consumer.accept(name, value);
            } catch ( Exception e ) {
                if ( e instanceof FcliActionStepException ) {
                    throw e;
                } else {
                    valueString = getStepAsString(valueString, value);
                    throw new FcliActionStepException("Error processing:\n"+valueString, e);
                }
            }
            if ( LOG.isDebugEnabled() ) {
                valueString = getStepAsString(valueString, value);
                LOG.debug("End processing:\n"+valueString);
            }
        }
    }
    
    private final String getStepAsString(String cachedString, Object value) {
        if ( value==null ) { return null; }
        if ( cachedString!=null ) { return cachedString; }
        try {
            cachedString = String.format("%s:\n%s", 
                StringUtils.indent(value.getClass().getCanonicalName(), "  "),
                StringUtils.indent(ctx.getYamlObjectMapper().valueToTree(value).toString(), "    "));
        } catch ( Exception e ) {
            cachedString = StringUtils.indent(value.toString(), "  ");
        }
        return cachedString;
    }
    
    private final boolean _if(Object o) {
        if ( o==null ) { return false; }
        if (ctx.isExitRequested() ) {
            LOG.debug("SKIPPED due to exit requested:\n"+getStepAsString(null, o));
            return false; 
        }
        if (o instanceof IActionStepIfSupplier ) {
            var _if = ((IActionStepIfSupplier) o).get_if();
            if ( _if!=null ) {
                var result = vars.eval(_if, Boolean.class);
                if ( !result ) { LOG.debug("SKIPPED due to 'if' evaluating to false:\n"+getStepAsString(null, o)); }
                return result;
            }
        }
        return true;
    }
    
    private final void processVarSetStep(Map<TemplateExpression, TemplateExpressionWithFormatter> map) {
        processVarSet(map, vars::set);
    }
    
    private final void processVarSetGlobalStep(Map<TemplateExpression, TemplateExpressionWithFormatter> map) {
        processVarSet(map, vars::setGlobal);
    }
    
    private final void processVarSet(Map<TemplateExpression, TemplateExpressionWithFormatter> map, BiConsumer<String, JsonNode> setter) {
        if ( map!=null ) { map.entrySet().forEach(e->processVarSet(e.getKey(), e.getValue(), setter)); }
    }

    private final void processVarSet(TemplateExpression nameExpression, TemplateExpressionWithFormatter templateExpressionWithFormatter, BiConsumer<String, JsonNode> setter) {
        if ( templateExpressionWithFormatter==null || _if(templateExpressionWithFormatter) ) {
            var name = vars.eval(nameExpression, String.class);
            var value = ActionRunnerHelper.formatValueAsJsonNode(ctx, vars, templateExpressionWithFormatter);
            setter.accept(name, value);
        }
    }
    
    private final void processVarRemoveGlobalStep(List<TemplateExpression> list) {
        processVarRemove(list, vars::unsetGlobal);
    }
    
    private final void processVarRemoveStep(List<TemplateExpression> list) {
        processVarRemove(list, vars::unset);
    }

    private final void processVarRemove(List<TemplateExpression> list, Consumer<String> remover) {
        list.forEach(i->processVarRemove(i, remover));
    }
    
    private final void processVarRemove(TemplateExpression entry, Consumer<String> remover) {
        remover.accept(vars.eval(entry, String.class));
    }
    
    private void processOutWriteStep(TemplateExpression target, TemplateExpressionWithFormatter templateExpressionWithFormatter) {
        write(target, ActionRunnerHelper.formatValueAsObject(ctx, vars, templateExpressionWithFormatter));
    }
    
    private void write(TemplateExpression destinationExpression, Object valueObject) {
        var destination = vars.eval(destinationExpression, String.class);
        var value = asString(valueObject);
        try {
            switch (destination.toLowerCase()) {
            case "stdout": writeImmediateOrDelayed(ctx.getStdout(), value); break;
            case "stderr": writeImmediateOrDelayed(ctx.getStderr(), value); break;
            default: write(new File(destination), value);
            }
        } catch (IOException e) {
            throw new FcliActionStepException("Error writing action output to "+destination);
        }
    }
    
    private Runnable createRunner(PrintStream out, String output) {
        return ()->out.print(output);
    }
    
    private void writeImmediateOrDelayed(PrintStream out, String value) {
        if ( ctx.getConfig().getAction().getConfig().getOutput()==ActionConfigOutput.delayed ) {
            ctx.getDelayedConsoleWriterRunnables().add(createRunner(out, value));
        } else {
            out.print(value);
        }
    }

    private void write(File file, String output) throws IOException {
        try ( var out = new PrintStream(file, StandardCharsets.UTF_8) ) {
            out.println(output);
        }
    }

    private final String asString(Object output) {
        if ( output instanceof TextNode ) {
            return ((TextNode)output).asText();
        } else if ( output instanceof JsonNode ) {
            return ((JsonNode)output).toPrettyString();
        } else {
            return output.toString();
        }
    }  

    private void processLogProgressStep(TemplateExpression progress) {
        ctx.getProgressWriter().writeProgress(asString(vars.eval(progress, Object.class)));
    }
    
    private void processLogWarnStep(TemplateExpression progress) {
        ctx.getProgressWriter().writeWarning(asString(vars.eval(progress, Object.class)));
    }
    
    private void processLogDebugStep(TemplateExpression progress) {
        LOG.debug(asString(vars.eval(progress, Object.class)));
    }
    
    private void processThrowStep(TemplateExpression message) {
        throw new FcliActionStepException(vars.eval(message, String.class));
    }
    
    private void processExitStep(TemplateExpression exitCodeExpression) {
        ctx.setExitCode(vars.eval(exitCodeExpression, Integer.class));
        ctx.setExitRequested(true);
    }
    
    private void processForEachRecordStep(ActionStepForEachRecord forEachStep) {
        // TODO Clean up this method
        var from = vars.eval(forEachStep.getFrom(), Object.class);
        if ( from==null ) { return; }
        if ( from instanceof IActionStepForEachProcessor ) {
            ((IActionStepForEachProcessor)from).process(node->processForEachStepNode(forEachStep, node));
            return;
        }
        if ( from instanceof Collection<?> ) {
            from = JsonHelper.getObjectMapper().valueToTree(from);
        }
        if ( from instanceof ArrayNode ) {
            JsonHelper.stream((ArrayNode)from)
                .allMatch(value->processForEachStepNode(forEachStep, value));
        } else {
            throw new FcliActionStepException("steps:records.for-each:from must evaluate to either an array or IActionStepForEachProcessor instance");
        }
    }
    
    private boolean processForEachStepNode(AbstractActionElementForEachRecord forEachRecord, JsonNode node) {
        if ( forEachRecord==null ) { return false; }
        var breakIf = forEachRecord.getBreakIf();
        vars.set(forEachRecord.getVarName(), node);
        if ( breakIf!=null && vars.eval(breakIf, Boolean.class) ) {
            return false;
        }
        if ( _if(forEachRecord) ) {
            processSteps(forEachRecord.get_do());
        }
        return true;
    }
    
    private void processCheckStep(String key, ActionStepCheck checkStep) {
        var failIf = checkStep.getFailIf();
        var passIf = checkStep.getPassIf();
        var pass = passIf!=null 
                ? vars.eval(passIf, Boolean.class)
                : !vars.eval(failIf, Boolean.class);
        var currentStatus = pass ? CheckStatus.PASS : CheckStatus.FAIL;
        var newCheckStatus = ctx.getCheckStatuses().compute(checkStep, (s,oldStatus)->CheckStatus.combine(oldStatus, currentStatus));
        vars.set("checkStatus."+key, new TextNode(newCheckStatus.name()));
    }
    
    private void processRestTargetsStep(String name, ActionStepRestTarget descriptor) {
        ctx.addRequestHelper(name, createBasicRequestHelper(name, descriptor));
    }
    
    private IActionRequestHelper createBasicRequestHelper(String name, ActionStepRestTarget descriptor) {
        var baseUrl = vars.eval(descriptor.getBaseUrl(), String.class);
        var headers = vars.eval(descriptor.getHeaders(), String.class);
        IUnirestInstanceSupplier unirestInstanceSupplier = () -> GenericUnirestFactory.getUnirestInstance(name, u->{
            u.config().defaultBaseUrl(baseUrl).getDefaultHeaders().add(headers);
            UnirestUnexpectedHttpResponseConfigurer.configure(u);
            UnirestJsonHeaderConfigurer.configure(u);
        });
        return new BasicActionRequestHelper(unirestInstanceSupplier, null);
    }
    
    private void processRunFcliStep(String name, ActionStepRunFcli fcli) {
        var cmd = vars.eval(fcli.getCmd(), String.class).replaceFirst("^fcli\s+", "");
        ctx.getProgressWriter().writeProgress("Executing fcli %s", cmd);
        var recordConsumer = createFcliRecordConsumer(fcli);
        var requestedStdoutOutputType = getFcliOutputTypeOrDefault(fcli.getStdoutOutputType(), recordConsumer==null ? OutputType.show : OutputType.suppress );
        var requestedStderrOutputType = getFcliOutputTypeOrDefault(fcli.getStderrOutputType(), OutputType.show );
        var actualStdoutOutputType = overrideFcliShowWithCollectOutputTypeIfDelayed(requestedStdoutOutputType);
        var actualStderrOutputType = overrideFcliShowWithCollectOutputTypeIfDelayed(requestedStderrOutputType);
        var delayedStdout = actualStdoutOutputType==OutputType.collect && requestedStdoutOutputType==OutputType.show;
        var delayedStderr = actualStderrOutputType==OutputType.collect && requestedStdoutOutputType==OutputType.show;
        var cmdExecutor = FcliCommandExecutorFactory.builder()
                .cmd(cmd)
                .progressOptionValueIfNotPresent(ctx.getConfig().getProgressWriterFactory().getType().name())
                .sessionOptionValueIfNotPresent(ctx.getConfig().getRequestedSessionName())
                .stdoutOutputType(actualStdoutOutputType)
                .stderrOutputType(actualStderrOutputType)
                .onResult(r->onFcliResult(fcli, recordConsumer, delayedStdout, delayedStderr, r))
                .onSuccess(r->onFcliSuccess(fcli))
                .onException(e->onFcliException(fcli, e))
                .onFail(r->onFcliFail(fcli, recordConsumer, delayedStdout, delayedStderr, r))
                .recordConsumer(recordConsumer).build().create();
        if ( recordConsumer!=null && !cmdExecutor.canCollectRecords() ) {
            throw new FcliActionValidationException("Can't use records.for-each on fcli command: "+cmd);
        }
        cmdExecutor.execute();
    }
    
    // Set variables, write delayed output
    private void onFcliResult(ActionStepRunFcli fcli, FcliRecordConsumer recordConsumer, boolean delayedStdout, boolean delayedStderr, Result result) {
        setFcliVars(fcli, recordConsumer, delayedStdout, delayedStderr, result);
        if ( delayedStderr ) { 
            writeImmediateOrDelayed(ctx.getStderr(), result.getErr());
        }
        if ( delayedStdout ) {
            writeImmediateOrDelayed(ctx.getStdout(), result.getOut());
        }
    }

    private void onFcliSuccess(ActionStepRunFcli fcli) {
        if ( fcli.getOnSuccess()!=null ) {
            processSteps(fcli.getOnSuccess());
        }
    }

    private void onFcliFail(ActionStepRunFcli fcli, FcliRecordConsumer recordConsumer, boolean delayedStdout, boolean delayedStderr, Result result) {
        setFcliVars(fcli, recordConsumer, delayedStdout, delayedStderr, result);
        List<ActionStep> onFail = fcli.getOnFail();
        if ( onFail==null ) {
            throw new FcliActionStepException(String.format("'%s' returned non-zero exit code %s", 
                    getFcliCmdWithoutOpts(fcli), result.getExitCode()));
        } else {
            processSteps(onFail);
        }
    }

    private String getFcliCmdWithoutOpts(ActionStepRunFcli fcli) {
        var result = vars.eval(fcli.getCmd(), String.class).replaceAll("\s+-.*", "");
        return result.startsWith("fcli") ? result : String.format("fcli %s", result);
    }

    private void onFcliException(ActionStepRunFcli fcli, Throwable t) {
        vars.set(fcli.getKey()+"_exception", new POJONode(t));
        // See comments on commented out onException in ActionStepRunFcli
        //List<ActionStep> onException = fcli.getOnException();
        //if ( onException==null ) {
            throw t instanceof RuntimeException 
                ? (RuntimeException)t
                : new FcliActionStepException("Fcli command terminated with an exception", t);
        //} else {
        //    processSteps(onException);
        //}
    }
    
    private void setFcliVars(ActionStepRunFcli fcli, FcliRecordConsumer recordConsumer, boolean delayedStdout, boolean delayedStderr, Result result) {
        var name = fcli.getKey();
        vars.set(name, recordConsumer!=null ? recordConsumer.getRecords() : JsonHelper.getObjectMapper().createArrayNode());
        // If stdout/stderr were collected for delayed output, we set the variables to empty string
        vars.set(name+"_stdout", delayedStdout ? "" : result.getOut());
        vars.set(name+"_stderr", delayedStderr ? "" : result.getErr());
        vars.set(name+"_exitCode", new IntNode(result.getExitCode()));
    }

    private OutputType getFcliOutputTypeOrDefault(OutputType outputType, OutputType _default) {
        return outputType==null ? _default : outputType;
    }
    
    private OutputType overrideFcliShowWithCollectOutputTypeIfDelayed(OutputType requestedOutputType) {
        return ctx.getConfig().getAction().getConfig().getOutput()==ActionConfigOutput.delayed
                && requestedOutputType==OutputType.show
                ? OutputType.collect
                : requestedOutputType;
    }

    private final FcliRecordConsumer createFcliRecordConsumer(ActionStepRunFcli fcli) {
        var forEachRecord = fcli.getForEachRecord();
        var collectRecords = fcli.isCollectRecords();
        return forEachRecord!=null || collectRecords 
                ? new FcliRecordConsumer(forEachRecord, collectRecords)
                : null;
    }
    @RequiredArgsConstructor
    private class FcliRecordConsumer implements Consumer<ObjectNode> {
        private final ActionStepFcliForEachDescriptor fcliForEach;
        private final boolean collectRecords;
        
        @Getter(lazy=true) private final ArrayNode records = JsonHelper.getObjectMapper().createArrayNode();
        private boolean continueDoSteps = true;
        @Override
        public void accept(ObjectNode record) {
            if ( collectRecords ) {
                getRecords().add(record);
            }
            if ( continueDoSteps && fcliForEach!=null ) {
                continueDoSteps = processForEachStepNode(fcliForEach, record);
            }
        }
    }

    private void processRestCallStep(Map<String, ActionStepRestCall> requests) {
        if ( requests!=null ) {
            var requestsProcessor = new ActionStepRequestsProcessor(ctx);
            requestsProcessor.addRequests(requests, this::processResponse, this::processFailure, vars);
            requestsProcessor.executeRequests();
        }
    }
    
    private final void processResponse(ActionStepRestCall requestDescriptor, JsonNode rawBody) {
        var name = requestDescriptor.getKey();
        var body = ctx.getRequestHelper(requestDescriptor.getTarget()).transformInput(rawBody);
        vars.setLocal(name+"_raw", rawBody);
        vars.setLocal(name, body);
        processOnResponse(requestDescriptor);
        processRequestStepForEach(requestDescriptor);
    }
    
    private final void processFailure(ActionStepRestCall requestDescriptor, UnirestException e) {
        var onFailSteps = requestDescriptor.getOnFail();
        if ( onFailSteps==null ) { throw e; }
        vars.setLocal(requestDescriptor.getKey()+"_exception", new POJONode(e));
        processSteps(onFailSteps);
    }
    
    private final void processOnResponse(ActionStepRestCall requestDescriptor) {
        var onResponseSteps = requestDescriptor.getOnResponse();
        processSteps(onResponseSteps);
    }

    private final void processRequestStepForEach(ActionStepRestCall requestDescriptor) {
        var forEach = requestDescriptor.getForEach();
        if ( forEach!=null ) {
            var input = vars.get(requestDescriptor.getKey());
            if ( input!=null ) {
                if ( input instanceof ArrayNode ) {
                    updateRequestStepForEachTotalCount(forEach, (ArrayNode)input);
                    processRequestStepForEachEmbed(forEach, (ArrayNode)input);
                    processRequestStepForEach(forEach, (ArrayNode)input, this::processRequestStepForEachEntryDo);
                } else {
                    throw new FcliActionValidationException("forEach not supported on node type "+input.getNodeType());
                }
            }
        }
    }
    
    private final void processRequestStepForEachEmbed(ActionStepRequestForEachResponseRecord forEach, ArrayNode source) {
        var requestExecutor = new ActionStepRequestsProcessor(ctx);
        processRequestStepForEach(forEach, source, getRequestForEachEntryEmbedProcessor(requestExecutor));
        requestExecutor.executeRequests();
    }
    
    @FunctionalInterface
    private interface IRequestStepForEachEntryProcessor {
        void process(ActionStepRequestForEachResponseRecord forEach, JsonNode currentNode, ActionRunnerVars vars);
    }
    
    private final void processRequestStepForEach(ActionStepRequestForEachResponseRecord forEach, ArrayNode source, IRequestStepForEachEntryProcessor entryProcessor) {
        for ( int i = 0 ; i < source.size(); i++ ) {
            var currentNode = source.get(i);
            var newVars = vars.createChild();
            newVars.setLocal(forEach.getVarName(), currentNode);
            var breakIf = forEach.getBreakIf();
            if ( breakIf!=null && newVars.eval(breakIf, Boolean.class) ) {
                break;
            }
            var _if = forEach.get_if(); 
            if ( _if==null || newVars.eval(_if, Boolean.class) ) {
                entryProcessor.process(forEach, currentNode, newVars);
            }
        }
    }
    
    private void updateRequestStepForEachTotalCount(ActionStepRequestForEachResponseRecord forEach, ArrayNode array) {
        var totalCountName = String.format("total%sCount", StringUtils.capitalize(forEach.getVarName()));
        var totalCount = vars.get(totalCountName);
        if ( totalCount==null ) { totalCount = new IntNode(0); }
        vars.setLocal(totalCountName, new IntNode(totalCount.asInt()+array.size()));
    }

    private void processRequestStepForEachEntryDo(ActionStepRequestForEachResponseRecord forEach, JsonNode currentNode, ActionRunnerVars newVars) {
        var processor = new ActionStepsProcessor(ctx, newVars);
        processor.processSteps(forEach.get_do());
    }
    
    private IRequestStepForEachEntryProcessor getRequestForEachEntryEmbedProcessor(ActionStepRequestsProcessor requestExecutor) {
        return (forEach, currentNode, newVars) -> {
            if ( !currentNode.isObject() ) {
                // TODO Improve exception message?
                throw new FcliActionStepException("Cannot embed data on non-object nodes: "+forEach.getVarName());
            }
            requestExecutor.addRequests(forEach.getEmbed(), 
                    (rd,r)->((ObjectNode)currentNode).set(rd.getKey(), ctx.getRequestHelper(rd.getTarget()).transformInput(r)), 
                    this::processFailure, newVars);
        };
    }
    
    @RequiredArgsConstructor
    private static final class ActionStepRequestsProcessor {
        private final ActionRunnerContext ctx;
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> simpleRequests = new LinkedHashMap<>();
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> pagedRequests = new LinkedHashMap<>();
        
        final void addRequests(Map<String, ActionStepRestCall> requestDescriptors, BiConsumer<ActionStepRestCall, JsonNode> responseConsumer, BiConsumer<ActionStepRestCall, UnirestException> failureConsumer, ActionRunnerVars vars) {
            if ( requestDescriptors!=null ) {
                requestDescriptors.values().forEach(r->addRequest(r, responseConsumer, failureConsumer, vars));
            }
        }
        
        private final void addRequest(ActionStepRestCall requestDescriptor, BiConsumer<ActionStepRestCall, JsonNode> responseConsumer, BiConsumer<ActionStepRestCall, UnirestException> failureConsumer, ActionRunnerVars vars) {
            var _if = requestDescriptor.get_if();
            if ( _if==null || vars.eval(_if, Boolean.class) ) {
                var method = requestDescriptor.getMethod();
                var uri = vars.eval(requestDescriptor.getUri(), String.class);
                checkUri(uri);
                var query = vars.eval(requestDescriptor.getQuery(), Object.class);
                var body = requestDescriptor.getBody()==null ? null : vars.eval(requestDescriptor.getBody(), Object.class);
                var requestData = new IActionRequestHelper.ActionRequestDescriptor(method, uri, query, body, r->responseConsumer.accept(requestDescriptor, r), e->failureConsumer.accept(requestDescriptor, e));
                addLogProgress(requestData, requestDescriptor.getLogProgress(), vars);
                if ( requestDescriptor.getType()==ActionStepRequestType.paged ) {
                    pagedRequests.computeIfAbsent(requestDescriptor.getTarget(), s->new ArrayList<IActionRequestHelper.ActionRequestDescriptor>()).add(requestData);
                } else {
                    simpleRequests.computeIfAbsent(requestDescriptor.getTarget(), s->new ArrayList<IActionRequestHelper.ActionRequestDescriptor>()).add(requestData);
                }
            }
        }

        private void checkUri(String uriString) {
            try {
                var uri = new URI(uriString);
                // We don't allow absolute URIs, as this could expose authorization
                // headers and other data to systems other than the predefined target
                // system.
                if ( uri.isAbsolute() ) {
                    throw new FcliActionValidationException("Absolute request uri is not allowed: "+uriString);
                }
            } catch ( URISyntaxException e ) {
                throw new FcliActionValidationException("Invalid request uri: "+uriString);
            }
        }

        private void addLogProgress(ActionRequestDescriptor requestData, ActionStepRestCallLogProgressDescriptor pagingProgress, ActionRunnerVars vars) {
            if ( pagingProgress!=null ) {
                addPagingProgress(pagingProgress.getPrePageLoad(), requestData::setPrePageLoad, vars);
                addPagingProgress(pagingProgress.getPostPageLoad(), requestData::setPostPageLoad, vars);
                addPagingProgress(pagingProgress.getPostPageProcess(), requestData::setPostPageProcess, vars);
            }
        }
        
        private void addPagingProgress(TemplateExpression expr, Consumer<Runnable> consumer, ActionRunnerVars vars) {
            if ( expr!=null ) {
                consumer.accept(()->ctx.getProgressWriter().writeProgress(vars.eval(expr, String.class)));
            }
        }
        
        final void executeRequests() {
            simpleRequests.entrySet().forEach(e->executeRequest(e.getKey(), e.getValue(), false));
            pagedRequests.entrySet().forEach(e->executeRequest(e.getKey(), e.getValue(), true));
        }
        
        private void executeRequest(String target, List<ActionRequestDescriptor> requests, boolean isPaged) {
            var requestHelper = ctx.getRequestHelper(target);
            if ( isPaged ) {
                requests.forEach(r->requestHelper.executePagedRequest(r));
            } else {
                requestHelper.executeSimpleRequests(requests);
            }
        }
    }
}