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
import org.springframework.expression.spel.SpelEvaluationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fortify.cli.common.action.model.AbstractActionStepForEach;
import com.fortify.cli.common.action.model.ActionConfig.ActionConfigOutput;
import com.fortify.cli.common.action.model.ActionStep;
import com.fortify.cli.common.action.model.ActionStepAppend;
import com.fortify.cli.common.action.model.ActionStepCheck;
import com.fortify.cli.common.action.model.ActionStepCheck.CheckStatus;
import com.fortify.cli.common.action.model.ActionStepFcli;
import com.fortify.cli.common.action.model.ActionStepForEach;
import com.fortify.cli.common.action.model.ActionStepForEach.IActionStepForEachProcessor;
import com.fortify.cli.common.action.model.ActionStepRequest;
import com.fortify.cli.common.action.model.ActionStepRequest.ActionStepRequestForEachDescriptor;
import com.fortify.cli.common.action.model.ActionStepRequest.ActionStepRequestPagingProgressDescriptor;
import com.fortify.cli.common.action.model.ActionStepRequest.ActionStepRequestType;
import com.fortify.cli.common.action.model.ActionStepSet;
import com.fortify.cli.common.action.model.ActionStepUnset;
import com.fortify.cli.common.action.model.ActionStepWrite;
import com.fortify.cli.common.action.model.ActionValidationException;
import com.fortify.cli.common.action.model.ActionValueTemplate;
import com.fortify.cli.common.action.model.IActionStepIfSupplier;
import com.fortify.cli.common.action.model.IActionStepValueSupplier;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerData;
import com.fortify.cli.common.action.runner.StepProcessingException;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.ActionRequestDescriptor;
import com.fortify.cli.common.cli.util.FcliCommandExecutor;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonHelper.JsonNodeDeepCopyWalker;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.UnirestException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ActionStepsProcessor {
    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(ActionStepsProcessor.class);
    private final ActionRunnerContext ctx;
    private final ActionRunnerData data;
    
    public final void processSteps(List<ActionStep> steps) {
        if ( steps!=null ) { steps.forEach(this::processStep); }
    }
    
    private final void processStep(ActionStep step) {
        if ( _if(step) ) {
            processStepSupplier(step::getProgress, this::processProgressStep);
            processStepSupplier(step::getWarn, this::processWarnStep);
            processStepSupplier(step::getDebug, this::processDebugStep);
            processStepSupplier(step::get_throw, this::processThrowStep);
            processStepSupplier(step::get_exit, this::processExitStep);
            processStepSupplier(step::getRequests, this::processRequestsStep);
            processStepSupplier(step::getForEach, this::processForEachStep);
            processStepEntries(step::getFcli, this::processFcliStep);
            processStepEntries(step::getSet, this::processSetStep);
            processStepEntries(step::getAppend, this::processAppendStep);
            processStepEntries(step::getUnset, this::processUnsetStep);
            processStepEntries(step::getCheck, this::processCheckStep);
            processStepEntries(step::getWrite, this::processWriteStep);
            processStepEntries(step::getSteps, this::processStep);
        }
    }
    
    private <T> void processStepEntries(Supplier<List<T>> supplier, Consumer<T> consumer) {
        var list = supplier.get();
        if ( list!=null ) { list.forEach(value->processStep(value, consumer)); }
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
                return data.eval(_if, Boolean.class);
            }
        }
        return true;
    }
    
    private void processSetStep(ActionStepSet set) {
        var name = set.getName();
        var value = getValue(set);
        data.set(name, value);
    }
    
    private void processAppendStep(ActionStepAppend append) {
        var name = append.getName();
        var property = append.getProperty();
        var currentValue = data.get(name);
        var valueToAppend = getValue(append);
        if ( property==null ) {
            appendToArray(name, currentValue, valueToAppend);
        } else {
            appendToObject(name, currentValue, data.eval(property, String.class), valueToAppend);
        }
    }

    private void appendToArray(String name, JsonNode currentValue, JsonNode valueToAppend) {
        if ( currentValue==null ) {
            currentValue = ctx.getObjectMapper().createArrayNode();
        }
        if ( !currentValue.isArray() ) {
            throw new IllegalStateException("Cannot append value to non-array node "+currentValue.getNodeType());
        } else {
            if ( valueToAppend!=null ) {
                ((ArrayNode)currentValue).add(valueToAppend);
            }
            data.set(name, currentValue); // Update copies in parents
        }
    }
    
    private void appendToObject(String name, JsonNode currentValue, String property, JsonNode valueToAppend) {
        if ( currentValue==null ) {
            currentValue = ctx.getObjectMapper().createObjectNode();
        }
        if ( !currentValue.isObject() ) {
            throw new IllegalStateException(String.format("Cannot append value to non-object node "+currentValue.getNodeType()));
        } else {
            if ( valueToAppend!=null ) {
                ((ObjectNode)currentValue).set(property, valueToAppend);
            }
            data.set(name, currentValue); // Update copies in parents
        }
    }

    private void processUnsetStep(ActionStepUnset unset) {
        data.unset(unset.getName());
    }
    
    private JsonNode getValue(IActionStepValueSupplier supplier) {
        var value = supplier.getValue();
        var valueTemplate = supplier.getValueTemplate();
        if ( value!=null ) { return getValue(value); }
        else if ( StringUtils.isNotBlank(valueTemplate) ) { return getTemplateValue(valueTemplate); }
        else { throw new IllegalStateException("Either value or valueTemplate must be specified"); }
    }

    private JsonNode getValue(TemplateExpression valueExpression) {
        return ctx.getObjectMapper().valueToTree(data.eval(valueExpression, Object.class));
    }
    
    private final JsonNode getTemplateValue(String templateName) {
        var valueTemplateDescriptor = ctx.getConfig().getAction().getValueTemplatesByName().get(templateName);
        var outputRawContents = valueTemplateDescriptor.getContents();
        return new JsonNodeOutputWalker(valueTemplateDescriptor, data).walk(outputRawContents);
    }
    
    private void processWriteStep(ActionStepWrite write) {
        var to = data.eval(write.getTo(), String.class);
        var value = asString(getValue(write));
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

    private void processProgressStep(TemplateExpression progress) {
        ctx.getProgressWriter().writeProgress(data.eval(progress, String.class));
    }
    
    private void processWarnStep(TemplateExpression progress) {
        ctx.getProgressWriter().writeWarning(data.eval(progress, String.class));
    }
    
    private void processDebugStep(TemplateExpression progress) {
        LOG.debug(data.eval(progress, String.class));
    }
    
    private void processThrowStep(TemplateExpression message) {
        throw new StepProcessingException(data.eval(message, String.class));
    }
    
    private void processExitStep(TemplateExpression exitCodeExpression) {
        ctx.setExitCode(data.eval(exitCodeExpression, Integer.class));
        ctx.setExitRequested(true);
    }
    
    private void processForEachStep(ActionStepForEach forEach) {
        var processorExpression = forEach.getProcessor();
        var valuesExpression = forEach.getValues();
        if ( processorExpression!=null ) {
            var processor = data.eval(processorExpression, IActionStepForEachProcessor.class);
            if ( processor!=null ) { processor.process(node->processForEachStepNode(forEach, node)); }
        } else if ( valuesExpression!=null ) {
            var values = data.eval(valuesExpression, ArrayNode.class);
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
        data.set(forEach.getName(), node);
        if ( breakIf!=null && data.eval(breakIf, Boolean.class) ) {
            return false;
        }
        if ( _if(forEach) ) {
            processSteps(forEach.get_do());
        }
        return true;
    }
    
    private void processCheckStep(ActionStepCheck check) {
        var displayName = check.getDisplayName();
        var failIf = check.getFailIf();
        var passIf = check.getPassIf();
        var pass = passIf!=null 
                ? data.eval(passIf, Boolean.class)
                : !data.eval(failIf, Boolean.class);
        var currentStatus = pass ? CheckStatus.PASS : CheckStatus.FAIL;
        ctx.getCheckStatuses().compute(displayName, (name,oldStatus)->CheckStatus.combine(oldStatus, currentStatus));
    }
    
    private void processFcliStep(ActionStepFcli fcli) {
        var args = data.eval(fcli.getArgs(), String.class);
        ctx.getProgressWriter().writeProgress("Executing fcli %s", args);
        var cmdExecutor = new FcliCommandExecutor(ctx.getConfig().getRootCommandLine(), args);
        Consumer<ObjectNode> recordConsumer = null;
        var forEach = fcli.getForEach();
        var name = fcli.getName();
        if ( StringUtils.isNotBlank(name) ) {
            data.set(name, ctx.getObjectMapper().createArrayNode());
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
        private final ActionStepFcli fcli;
        private boolean continueProcessing = true;
        @Override
        public void accept(ObjectNode record) {
            var name = fcli.getName();
            if ( StringUtils.isNotBlank(name) ) {
                // For name attribute, we want to collect all records,
                // independent of break condition in the forEach block.
                appendToArray(name, data.get(name), record);
            }
            if ( continueProcessing ) {
                continueProcessing = processForEachStepNode(fcli.getForEach(), record);
            }
        }
    }

    private void processRequestsStep(List<ActionStepRequest> requests) {
        if ( requests!=null ) {
            var requestsProcessor = new ActionStepRequestsProcessor(ctx);
            requestsProcessor.addRequests(requests, this::processResponse, this::processFailure, data);
            requestsProcessor.executeRequests();
        }
    }
    
    private final void processResponse(ActionStepRequest requestDescriptor, JsonNode rawBody) {
        var name = requestDescriptor.getName();
        var body = ctx.getConfig().getRequestHelper(requestDescriptor.getTarget()).transformInput(rawBody);
        data.setLocal(name+"_raw", rawBody);
        data.setLocal(name, body);
        processOnResponse(requestDescriptor);
        processRequestStepForEach(requestDescriptor);
    }
    
    private final void processFailure(ActionStepRequest requestDescriptor, UnirestException e) {
        var onFailSteps = requestDescriptor.getOnFail();
        if ( onFailSteps==null ) { throw e; }
        data.setLocal("exception", new POJONode(e));
        processSteps(onFailSteps);
    }
    
    private final void processOnResponse(ActionStepRequest requestDescriptor) {
        var onResponseSteps = requestDescriptor.getOnResponse();
        processSteps(onResponseSteps);
    }

    private final void processRequestStepForEach(ActionStepRequest requestDescriptor) {
        var forEach = requestDescriptor.getForEach();
        if ( forEach!=null ) {
            var input = data.get(requestDescriptor.getName());
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
        void process(ActionStepRequestForEachDescriptor forEach, JsonNode currentNode, ActionRunnerData data);
    }
    
    private final void processRequestStepForEach(ActionStepRequestForEachDescriptor forEach, ArrayNode source, IRequestStepForEachEntryProcessor entryProcessor) {
        for ( int i = 0 ; i < source.size(); i++ ) {
            var currentNode = source.get(i);
            var newData = data.createChild();
            newData.setLocal(forEach.getName(), currentNode);
            var breakIf = forEach.getBreakIf();
            if ( breakIf!=null && newData.eval(breakIf, Boolean.class) ) {
                break;
            }
            var _if = forEach.get_if(); 
            if ( _if==null || newData.eval(_if, Boolean.class) ) {
                entryProcessor.process(forEach, currentNode, newData);
            }
        }
    }
    
    private void updateRequestStepForEachTotalCount(ActionStepRequestForEachDescriptor forEach, ArrayNode array) {
        var totalCountName = String.format("total%sCount", StringUtils.capitalize(forEach.getName()));
        var totalCount = data.get(totalCountName);
        if ( totalCount==null ) { totalCount = new IntNode(0); }
        data.setLocal(totalCountName, new IntNode(totalCount.asInt()+array.size()));
    }

    private void processRequestStepForEachEntryDo(ActionStepRequestForEachDescriptor forEach, JsonNode currentNode, ActionRunnerData newData) {
        var processor = new ActionStepsProcessor(ctx, newData);
        processor.processSteps(forEach.get_do());
    }
    
    private IRequestStepForEachEntryProcessor getRequestForEachEntryEmbedProcessor(ActionStepRequestsProcessor requestExecutor) {
        return (forEach, currentNode, newData) -> {
            if ( !currentNode.isObject() ) {
                // TODO Improve exception message?
                throw new IllegalStateException("Cannot embed data on non-object nodes: "+forEach.getName());
            }
            requestExecutor.addRequests(forEach.getEmbed(), 
                    (rd,r)->((ObjectNode)currentNode).set(rd.getName(), ctx.getConfig().getRequestHelper(rd.getTarget()).transformInput(r)), 
                    this::processFailure, newData);
        };
    }
    
    @RequiredArgsConstructor
    private static final class ActionStepRequestsProcessor {
        private final ActionRunnerContext ctx;
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> simpleRequests = new LinkedHashMap<>();
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> pagedRequests = new LinkedHashMap<>();
        
        final void addRequests(List<ActionStepRequest> requestDescriptors, BiConsumer<ActionStepRequest, JsonNode> responseConsumer, BiConsumer<ActionStepRequest, UnirestException> failureConsumer, ActionRunnerData data) {
            if ( requestDescriptors!=null ) {
                requestDescriptors.forEach(r->addRequest(r, responseConsumer, failureConsumer, data));
            }
        }
        
        private final void addRequest(ActionStepRequest requestDescriptor, BiConsumer<ActionStepRequest, JsonNode> responseConsumer, BiConsumer<ActionStepRequest, UnirestException> failureConsumer, ActionRunnerData data) {
            var _if = requestDescriptor.get_if();
            if ( _if==null || data.eval(_if, Boolean.class) ) {
                var method = requestDescriptor.getMethod();
                var uri = data.eval(requestDescriptor.getUri(), String.class);
                checkUri(uri);
                var query = data.eval(requestDescriptor.getQuery(), Object.class);
                var body = requestDescriptor.getBody()==null ? null : data.eval(requestDescriptor.getBody(), Object.class);
                var requestData = new IActionRequestHelper.ActionRequestDescriptor(method, uri, query, body, r->responseConsumer.accept(requestDescriptor, r), e->failureConsumer.accept(requestDescriptor, e));
                addPagingProgress(requestData, requestDescriptor.getPagingProgress(), data);
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

        private void addPagingProgress(ActionRequestDescriptor requestData, ActionStepRequestPagingProgressDescriptor pagingProgress, ActionRunnerData data) {
            if ( pagingProgress!=null ) {
                addPagingProgress(pagingProgress.getPrePageLoad(), requestData::setPrePageLoad, data);
                addPagingProgress(pagingProgress.getPostPageLoad(), requestData::setPostPageLoad, data);
                addPagingProgress(pagingProgress.getPostPageProcess(), requestData::setPostPageProcess, data);
            }
        }
        
        private void addPagingProgress(TemplateExpression expr, Consumer<Runnable> consumer, ActionRunnerData data) {
            if ( expr!=null ) {
                consumer.accept(()->ctx.getProgressWriter().writeProgress(data.eval(expr, String.class)));
            }
        }
        
        final void executeRequests() {
            simpleRequests.entrySet().forEach(e->executeRequest(e.getKey(), e.getValue(), false));
            pagedRequests.entrySet().forEach(e->executeRequest(e.getKey(), e.getValue(), true));
        }
        
        private void executeRequest(String target, List<ActionRequestDescriptor> requests, boolean isPaged) {
            var requestHelper = ctx.getConfig().getRequestHelper(target);
            if ( isPaged ) {
                requests.forEach(r->requestHelper.executePagedRequest(r));
            } else {
                requestHelper.executeSimpleRequests(requests);
            }
        }
    }
    
    @RequiredArgsConstructor
    private static final class JsonNodeOutputWalker extends JsonNodeDeepCopyWalker {
        private final ActionValueTemplate outputDescriptor;
        private final ActionRunnerData data;
        @Override
        protected JsonNode copyValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
            if ( !(node instanceof TextNode) ) {
                return super.copyValue(state, path, parent, node);
            } else {
                TemplateExpression expression = outputDescriptor.getValueExpressions().get(path);
                if ( expression==null ) { throw new RuntimeException("No expression for "+path); }
                try {
                    var rawResult = data.eval(expression, Object.class);
                    if ( rawResult instanceof CharSequence ) {
                        rawResult = new TextNode(((String)rawResult).replace("\\n", "\n"));
                    }
                    return JsonHelper.getObjectMapper().valueToTree(rawResult);
                } catch ( SpelEvaluationException e ) {
                    throw new RuntimeException("Error evaluating action expression "+expression.getExpressionString(), e);
                }
            }
        }
    }
}