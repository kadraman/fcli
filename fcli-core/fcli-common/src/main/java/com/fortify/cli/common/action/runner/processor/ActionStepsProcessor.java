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
import com.fortify.cli.common.action.model.AbstractActionStepForEach;
import com.fortify.cli.common.action.model.ActionConfig.ActionConfigOutput;
import com.fortify.cli.common.action.model.ActionStep;
import com.fortify.cli.common.action.model.ActionStepCheck;
import com.fortify.cli.common.action.model.ActionStepCheck.CheckStatus;
import com.fortify.cli.common.action.model.ActionStepFileWrite;
import com.fortify.cli.common.action.model.ActionStepForEach;
import com.fortify.cli.common.action.model.ActionStepForEach.IActionStepForEachProcessor;
import com.fortify.cli.common.action.model.ActionStepRestCall;
import com.fortify.cli.common.action.model.ActionStepRestCall.ActionStepRequestForEachDescriptor;
import com.fortify.cli.common.action.model.ActionStepRestCall.ActionStepRequestPagingProgressDescriptor;
import com.fortify.cli.common.action.model.ActionStepRestCall.ActionStepRequestType;
import com.fortify.cli.common.action.model.ActionStepRestTarget;
import com.fortify.cli.common.action.model.ActionStepRunFcli;
import com.fortify.cli.common.action.model.ActionValidationException;
import com.fortify.cli.common.action.model.IActionStepIfSupplier;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerHelper;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.StepProcessingException;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.ActionRequestDescriptor;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.cli.util.FcliCommandExecutor;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.UnirestException;
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
            processStepSupplier(step::getVarFmt, this::processVarFmtStep);
            processStepSupplier(step::getVarUnset, this::processVarUnsetStep);
            processStepEntries(step::getRunFcli, this::processRunFcliStep);
            processStepEntries(step::getCheck, this::processCheckStep);
            processStepEntries(step::getFileWrite, this::processFileWriteStep);
            processStepSupplier(step::get_throw, this::processThrowStep);
            processStepSupplier(step::get_exit, this::processExitStep);
            processStepSupplier(step::getForEach, this::processForEachStep);
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
                if ( e instanceof StepProcessingException ) {
                    throw e;
                } else {
                    valueString = getStepAsString(valueString, value);
                    throw new StepProcessingException("Error processing:\n"+valueString, e);
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
                if ( e instanceof StepProcessingException ) {
                    throw e;
                } else {
                    valueString = getStepAsString(valueString, value);
                    throw new StepProcessingException("Error processing:\n"+valueString, e);
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
                StringUtils.indent(ctx.getYamlObjectMapper().valueToTree(value).toPrettyString(), "    "));
        } catch ( Exception e ) {
            cachedString = StringUtils.indent(value.toString(), "  ");
        }
        return cachedString;
    }
    
    private final boolean _if(Object o) {
        if (ctx.isExitRequested() || o==null) { return false; }
        if (o instanceof IActionStepIfSupplier ) {
            var _if = ((IActionStepIfSupplier) o).get_if();
            if ( _if!=null ) {
                return vars.eval(_if, Boolean.class);
            }
        }
        return true;
    }
    
    private final void processVarFmtStep(LinkedHashMap<TemplateExpression, TemplateExpression> map) {
        map.entrySet().forEach(this::processVarFmtStepEntry);
    }
    
    private final void processVarFmtStepEntry(Map.Entry<TemplateExpression, TemplateExpression> entry) {
        var name = vars.eval(entry.getKey(), String.class);
        var formatter = vars.eval(entry.getValue(), String.class);
        var value = ActionRunnerHelper.fmt(ctx, formatter, vars.getValues());
        setVar(name, value);
    }
    
    private final void processVarSetStep(LinkedHashMap<TemplateExpression, TemplateExpression> map) {
        map.entrySet().forEach(this::processVarSetStepEntry);
    }
    
    private final void processVarSetStepEntry(Map.Entry<TemplateExpression, TemplateExpression> entry) {
        var name = vars.eval(entry.getKey(), String.class);
        var rawValue = vars.eval(entry.getValue(), Object.class);
        var value = ctx.getObjectMapper().valueToTree(rawValue);
        setVar(name, value);
    }

    private final void setVar(String name, JsonNode value) {
        if ( name.endsWith("..") ) { 
            appendToArray(name.substring(0, name.length()-2), value); 
        } else {
            var varName = StringUtils.substringBefore(name, ".");
            var propName = StringUtils.substringAfter(name, ".");
            if ( StringUtils.isBlank(propName) ) {
                vars.set(name, value);
            } else {
                appendToObject(varName, propName, value);
            }
        }
    }
    
    private final void appendToArray(String name, JsonNode value) {
        var array = getOrCreateVar(name, ()->ctx.getObjectMapper().createArrayNode());
        if ( !array.isArray() ) {
            throw new StepProcessingException("Variable "+name+" is not an array; cannot append value");
        }
        ((ArrayNode)array).add(value);
    }

    private final void appendToObject(String name, String property, JsonNode value) {
        var obj = getOrCreateVar(name, ()->ctx.getObjectMapper().createObjectNode());
        if ( !obj.isObject() ) {
            throw new StepProcessingException("Variable "+name+" is not a set of properties; can't set property "+property);
        }
        ((ObjectNode)obj).set(property, value);
    }
    
    private final JsonNode getOrCreateVar(String name, Supplier<JsonNode> supplier) {
        var v = vars.get(name);
        if ( v==null || v.isNull() ) {
            v = supplier.get();
            vars.set(name, v);
        }
        return v;
    }

    private final void processVarUnsetStep(List<TemplateExpression> list) {
        list.forEach(this::processVarUnsetStepEntry);
    }
    
    private final void processVarUnsetStepEntry(TemplateExpression entry) {
        vars.unset(vars.eval(entry, String.class));
    }
    
    private void processFileWriteStep(ActionStepFileWrite fileWrite) {
        var to = vars.eval(fileWrite.getTo(), String.class);
        var valueExpression = fileWrite.getValue();
        var fmt = fileWrite.getFmt();
        String value;
        if (StringUtils.isBlank(fmt) ) {
            value = asString(vars.eval(valueExpression, Object.class));
        } else {
            if ( valueExpression==null ) {
                valueExpression = SpelHelper.parseTemplateExpression("${#root}");
            }
            value = asString(ActionRunnerHelper.fmt(ctx, fmt, vars.eval(valueExpression, JsonNode.class)));
        }
        try {
            switch (to.toLowerCase()) {
            case "stdout": writeImmediateOrDelayed(ctx.getStdout(), value); break;
            case "stderr": writeImmediateOrDelayed(ctx.getStderr(), value); break;
            default: write(new File(to), value);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing action output to "+to);
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
        ctx.getProgressWriter().writeProgress(vars.eval(progress, String.class));
    }
    
    private void processLogWarnStep(TemplateExpression progress) {
        ctx.getProgressWriter().writeWarning(vars.eval(progress, String.class));
    }
    
    private void processLogDebugStep(TemplateExpression progress) {
        LOG.debug(vars.eval(progress, String.class));
    }
    
    private void processThrowStep(TemplateExpression message) {
        throw new StepProcessingException(vars.eval(message, String.class));
    }
    
    private void processExitStep(TemplateExpression exitCodeExpression) {
        ctx.setExitCode(vars.eval(exitCodeExpression, Integer.class));
        ctx.setExitRequested(true);
    }
    
    private void processForEachStep(ActionStepForEach forEach) {
        var processorExpression = forEach.getProcessor();
        var valuesExpression = forEach.getValues();
        if ( processorExpression!=null ) {
            var processor = vars.eval(processorExpression, IActionStepForEachProcessor.class);
            if ( processor!=null ) { processor.process(node->processForEachStepNode(forEach, node)); }
        } else if ( valuesExpression!=null ) {
            var values = vars.eval(valuesExpression, ArrayNode.class);
            if ( values!=null ) { 
                // Process values until processForEachStepNode() returns false
                JsonHelper.stream(values)
                    .allMatch(value->processForEachStepNode(forEach, value));
            }
        }
    }
    
    private boolean processForEachStepNode(AbstractActionStepForEach forEach, JsonNode node) {
        if ( forEach==null ) { return false; }
        var breakIf = forEach.getBreakIf();
        vars.set(forEach.getName(), node);
        if ( breakIf!=null && vars.eval(breakIf, Boolean.class) ) {
            return false;
        }
        if ( _if(forEach) ) {
            processSteps(forEach.get_do());
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
        appendToObject("checkStatus", key, new TextNode(newCheckStatus.name()));
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
    
    private void processRunFcliStep(ActionStepRunFcli fcli) {
        var args = vars.eval(fcli.getArgs(), String.class);
        ctx.getProgressWriter().writeProgress("Executing fcli %s", args);
        var cmdExecutor = new FcliCommandExecutor(ctx.getConfig().getRootCommandLine(), args);
        Consumer<ObjectNode> recordConsumer = null;
        var forEach = fcli.getForEach();
        var name = fcli.getName();
        if ( StringUtils.isNotBlank(name) ) {
            vars.set(name, ctx.getObjectMapper().createArrayNode());
        }
        if ( forEach!=null || StringUtils.isNotBlank(name) ) {
            if ( !cmdExecutor.canCollectRecords() ) {
                throw new IllegalStateException("Can't use forEach or name on fcli command: "+args);
            } else {
                recordConsumer = new FcliRecordConsumer(fcli);
            }
        }
        
        // TODO Implement optional output suppression
        var output = cmdExecutor.execute(recordConsumer, true);
        writeImmediateOrDelayed(ctx.getStderr(), output.getErr());
        writeImmediateOrDelayed(ctx.getStdout(), output.getOut());
        if ( output.getExitCode() >0 ) { 
            throw new StepProcessingException("Fcli command returned non-zero exit code "+output.getExitCode()); 
        }
    }
    @RequiredArgsConstructor
    private class FcliRecordConsumer implements Consumer<ObjectNode> {
        private final ActionStepRunFcli fcli;
        private boolean continueProcessing = true;
        @Override
        public void accept(ObjectNode record) {
            var name = fcli.getName();
            if ( StringUtils.isNotBlank(name) ) {
                // For name attribute, we want to collect all records,
                // independent of break condition in the forEach block.
                appendToArray(name, record);
            }
            if ( continueProcessing ) {
                continueProcessing = processForEachStepNode(fcli.getForEach(), record);
            }
        }
    }

    private void processRestCallStep(List<ActionStepRestCall> requests) {
        if ( requests!=null ) {
            var requestsProcessor = new ActionStepRequestsProcessor(ctx);
            requestsProcessor.addRequests(requests, this::processResponse, this::processFailure, vars);
            requestsProcessor.executeRequests();
        }
    }
    
    private final void processResponse(ActionStepRestCall requestDescriptor, JsonNode rawBody) {
        var name = requestDescriptor.getName();
        var body = ctx.getRequestHelper(requestDescriptor.getTarget()).transformInput(rawBody);
        vars.setLocal(name+"_raw", rawBody);
        vars.setLocal(name, body);
        processOnResponse(requestDescriptor);
        processRequestStepForEach(requestDescriptor);
    }
    
    private final void processFailure(ActionStepRestCall requestDescriptor, UnirestException e) {
        var onFailSteps = requestDescriptor.getOnFail();
        if ( onFailSteps==null ) { throw e; }
        vars.setLocal("exception", new POJONode(e));
        processSteps(onFailSteps);
    }
    
    private final void processOnResponse(ActionStepRestCall requestDescriptor) {
        var onResponseSteps = requestDescriptor.getOnResponse();
        processSteps(onResponseSteps);
    }

    private final void processRequestStepForEach(ActionStepRestCall requestDescriptor) {
        var forEach = requestDescriptor.getForEach();
        if ( forEach!=null ) {
            var input = vars.get(requestDescriptor.getName());
            if ( input!=null ) {
                if ( input instanceof ArrayNode ) {
                    updateRequestStepForEachTotalCount(forEach, (ArrayNode)input);
                    processRequestStepForEachEmbed(forEach, (ArrayNode)input);
                    processRequestStepForEach(forEach, (ArrayNode)input, this::processRequestStepForEachEntryDo);
                } else {
                    throw new ActionValidationException("forEach not supported on node type "+input.getNodeType());
                }
            }
        }
    }
    
    private final void processRequestStepForEachEmbed(ActionStepRequestForEachDescriptor forEach, ArrayNode source) {
        var requestExecutor = new ActionStepRequestsProcessor(ctx);
        processRequestStepForEach(forEach, source, getRequestForEachEntryEmbedProcessor(requestExecutor));
        requestExecutor.executeRequests();
    }
    
    @FunctionalInterface
    private interface IRequestStepForEachEntryProcessor {
        void process(ActionStepRequestForEachDescriptor forEach, JsonNode currentNode, ActionRunnerVars vars);
    }
    
    private final void processRequestStepForEach(ActionStepRequestForEachDescriptor forEach, ArrayNode source, IRequestStepForEachEntryProcessor entryProcessor) {
        for ( int i = 0 ; i < source.size(); i++ ) {
            var currentNode = source.get(i);
            var newVars = vars.createChild();
            newVars.setLocal(forEach.getName(), currentNode);
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
    
    private void updateRequestStepForEachTotalCount(ActionStepRequestForEachDescriptor forEach, ArrayNode array) {
        var totalCountName = String.format("total%sCount", StringUtils.capitalize(forEach.getName()));
        var totalCount = vars.get(totalCountName);
        if ( totalCount==null ) { totalCount = new IntNode(0); }
        vars.setLocal(totalCountName, new IntNode(totalCount.asInt()+array.size()));
    }

    private void processRequestStepForEachEntryDo(ActionStepRequestForEachDescriptor forEach, JsonNode currentNode, ActionRunnerVars newVars) {
        var processor = new ActionStepsProcessor(ctx, newVars);
        processor.processSteps(forEach.get_do());
    }
    
    private IRequestStepForEachEntryProcessor getRequestForEachEntryEmbedProcessor(ActionStepRequestsProcessor requestExecutor) {
        return (forEach, currentNode, newVars) -> {
            if ( !currentNode.isObject() ) {
                // TODO Improve exception message?
                throw new IllegalStateException("Cannot embed data on non-object nodes: "+forEach.getName());
            }
            requestExecutor.addRequests(forEach.getEmbed(), 
                    (rd,r)->((ObjectNode)currentNode).set(rd.getName(), ctx.getRequestHelper(rd.getTarget()).transformInput(r)), 
                    this::processFailure, newVars);
        };
    }
    
    @RequiredArgsConstructor
    private static final class ActionStepRequestsProcessor {
        private final ActionRunnerContext ctx;
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> simpleRequests = new LinkedHashMap<>();
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> pagedRequests = new LinkedHashMap<>();
        
        final void addRequests(List<ActionStepRestCall> requestDescriptors, BiConsumer<ActionStepRestCall, JsonNode> responseConsumer, BiConsumer<ActionStepRestCall, UnirestException> failureConsumer, ActionRunnerVars vars) {
            if ( requestDescriptors!=null ) {
                requestDescriptors.forEach(r->addRequest(r, responseConsumer, failureConsumer, vars));
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
                addPagingProgress(requestData, requestDescriptor.getPagingProgress(), vars);
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
                    throw new IllegalStateException("Absolute request uri is not allowed: "+uriString);
                }
            } catch ( URISyntaxException e ) {
                throw new IllegalStateException("Invalid request uri: "+uriString);
            }
        }

        private void addPagingProgress(ActionRequestDescriptor requestData, ActionStepRequestPagingProgressDescriptor pagingProgress, ActionRunnerVars vars) {
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