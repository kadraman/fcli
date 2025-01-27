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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.AbstractActionStepForEach;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionParameter;
import com.fortify.cli.common.action.model.ActionRequestTarget;
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
import com.fortify.cli.common.action.runner.ActionRunner.IActionRequestHelper.ActionRequestDescriptor;
import com.fortify.cli.common.action.runner.ActionRunner.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.cli.util.FcliCommandExecutor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.IOptionDescriptor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionsParseResult;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonHelper.JsonNodeDeepCopyWalker;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.spring.expression.IConfigurableSpelEvaluator;
import com.fortify.cli.common.spring.expression.ISpelEvaluator;
import com.fortify.cli.common.spring.expression.SpelEvaluator;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.JavaHelper;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

// TODO Move processing of each descriptor element into a separate class,
//      either for all elements or just for step elements.
//      For example, each of these classes could have a (static?) 
//      process(context, descriptor element), with context providing access
//      to ActionRunner fields, parent steps, local data, shared methods like 
//      setDataValue(), ...
@Builder
public class ActionRunner implements AutoCloseable {
    /** Jackson {@link ObjectMapper} used for various JSON-related operations */
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    /** Jackson {@link ObjectMapper} used for formatting steps in logging/exception messages */
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(ActionRunner.class);
    /** Progress writer, provided through builder method */
    private final IProgressWriterI18n progressWriter;
    /** Root CommandLine object for executing fcli commands, provided through builder method */
    private final CommandLine rootCommandLine;
    /** Data extract action, provided through builder method */
    @Getter private final Action action;
    /** Callback to handle validation errors */
    private final Function<OptionsParseResult, RuntimeException> onValidationErrors;
    /** ObjectNode holding parameter values as generated by ActionParameterProcessor */
    @Getter private final ObjectNode parameters = objectMapper.createObjectNode();
    /** SpEL evaluator configured with {@link ActionSpelFunctions} and variables for
     *  parameters, partialOutputs and outputs as defined above */
    @Getter private final IConfigurableSpelEvaluator spelEvaluator = SpelEvaluator.JSON_GENERIC.copy().configure(this::configureSpelEvaluator);
    /** Parameter converters as generated by {@link #createDefaultParameterConverters()} amended by 
     *  custom converters as added through the {@link #addParameterConverter(String, BiFunction)} and
     *  {@link #addParameterConverter(String, Function)} methods. */
    private final Map<String, BiFunction<String, ParameterTypeConverterArgs, JsonNode>> parameterConverters = createDefaultParameterConverters();
    /** Request helpers as configured through the {@link #addRequestHelper(String, IActionRequestHelper)} method */
    private final Map<String, IActionRequestHelper> requestHelpers = new HashMap<>();
    /** Check statuses */
    private final Map<String, CheckStatus> checkStatuses = new LinkedHashMap<>(); 
    // We need to delay writing output to console as to not interfere with progress writer
    private final List<Runnable> delayedConsoleWriterRunnables = new ArrayList<>();
    /** Save original stdout for delayed output operations */
    private final PrintStream stdout = System.out;
    /** Save original stderr for delayed output operations */
    private final PrintStream stderr = System.err;
    @Builder.Default private int exitCode = 0;
    @Builder.Default private boolean exitRequested = false;
    
    public final Callable<Integer> run(String[] args) {
        initializeCheckStatuses();
        progressWriter.writeProgress("Processing action parameters");
        processParameters(args);
        var actionData = new ActionData(spelEvaluator, parameters);
        new ActionAddRequestTargetsProcessor(actionData).addRequestTargets();
        progressWriter.writeProgress("Processing action steps");
        new ActionStepsProcessor(actionData).processSteps();
        progressWriter.writeProgress("Action processing finished");
        
        return ()->{
            delayedConsoleWriterRunnables.forEach(Runnable::run);
            if ( !checkStatuses.isEmpty() ) {
                checkStatuses.entrySet().forEach(
                    e-> printCheckResult(e.getValue(), e.getKey()));
                var overallStatus = CheckStatus.combine(checkStatuses.values());
                stdout.println("Status: "+overallStatus);
                if ( exitCode==0 && overallStatus==CheckStatus.FAIL ) {
                    exitCode = 100;
                }
            }
            return exitCode;
        };
    }
    
    private void initializeCheckStatuses() {
        for ( var elt : action.getAllActionElements() ) {
            if ( elt instanceof ActionStepCheck ) {
                var checkStep = (ActionStepCheck)elt;
                var displayName = checkStep.getDisplayName();
                var value = CheckStatus.combine(checkStatuses.get(displayName), checkStep.getIfSkipped());
                checkStatuses.put(displayName, value);
            }
        }
    }

    private final void printCheckResult(CheckStatus status, String displayName) {
        if ( status!=CheckStatus.HIDE ) {
            // Even when flushing, output may appear in incorrect order if some 
            // check statuses are written to stdout and others to stderr.
            //var out = status==CheckStatus.PASS?stdout:stderr;
            var out = stdout;
            out.println(String.format("%s: %s", status, displayName));
            //out.flush();
        }
    }

    public final void close() {
        requestHelpers.values().forEach(IActionRequestHelper::close);
    }
    
    private final void processParameters(String[] args) {
        var optionsParseResult = new ActionParameterProcessor(args, parameters).processParameters();
        if ( optionsParseResult.hasValidationErrors() ) {
            throw onValidationErrors.apply(optionsParseResult);
        }
    }
    
    private final void configureSpelEvaluator(SimpleEvaluationContext context) {
        SpelHelper.registerFunctions(context, ActionSpelFunctions.class);
        context.setVariable("action", new ActionUtil());
    }
    
    @Reflectable
    private final class ActionUtil {
        @SuppressWarnings("unused")
        public final String copyParametersFromGroup(String group) {
            StringBuilder result = new StringBuilder();
            for ( var p : action.getParameters() ) {
                if ( group==null || group.equals(p.getGroup()) ) {
                    var val = parameters.get(p.getName());
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
    
    public final ActionRunner addParameterConverter(String type, BiFunction<String, ParameterTypeConverterArgs, JsonNode> converter) {
        parameterConverters.put(type, converter);
        return this;
    }
    public final ActionRunner addParameterConverter(String type, Function<String, JsonNode> converter) {
        parameterConverters.put(type, (v,a)->converter.apply(v));
        return this;
    }
    public final ActionRunner addRequestHelper(String name, IActionRequestHelper requestHelper) {
        requestHelpers.put(name, requestHelper);
        return this;
    }
    
    public IActionRequestHelper getRequestHelper(String name) {
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
    
    private final class ActionParameterProcessor {
        private final ObjectNode parameters;
        private final OptionsParseResult optionsParseResult;

        public ActionParameterProcessor(String[] args, ObjectNode parameters) {
            this.parameters = parameters;
            this.optionsParseResult = parseParameterValues(args);
        }
        private final OptionsParseResult processParameters() {
            var validationErrors = optionsParseResult.getValidationErrors();
            if ( validationErrors.size()==0 ) {
                action.getParameters().forEach(this::addParameterData);
            }
            return optionsParseResult;
        }
        
        private final OptionsParseResult parseParameterValues(String[] args) {
            List<IOptionDescriptor> optionDescriptors = ActionParameterHelper.getOptionDescriptors(action);
            var parseResult = new SimpleOptionsParser(optionDescriptors).parse(args);
            addDefaultValues(parseResult);
            addValidationMessages(parseResult);
            return parseResult;
        }

        private final void addDefaultValues(OptionsParseResult parseResult) {
            action.getParameters().forEach(p->addDefaultValue(parseResult, p));
        }
        
        private final void addValidationMessages(OptionsParseResult parseResult) {
            action.getParameters().forEach(p->addValidationMessages(parseResult, p));
        }
        
        private final void addDefaultValue(OptionsParseResult parseResult, ActionParameter parameter) {
            var name = parameter.getName();
            var value = getOptionValue(parseResult, parameter);
            if ( value==null ) {
                var defaultValueExpression = parameter.getDefaultValue();
                value = defaultValueExpression==null 
                        ? null 
                        : spelEvaluator.evaluate(defaultValueExpression, parameters, String.class);
            }
            parseResult.getOptionValuesByName().put(ActionParameterHelper.getOptionName(name), value);
        }
        
        private final void addValidationMessages(OptionsParseResult parseResult, ActionParameter parameter) {
            if ( parameter.isRequired() && StringUtils.isBlank(getOptionValue(parseResult, parameter)) ) {
                parseResult.getValidationErrors().add("No value provided for required option "+
                        ActionParameterHelper.getOptionName(parameter.getName()));                
            }
        }

        private final void addParameterData(ActionParameter parameter) {
            var name = parameter.getName();
            var value = getOptionValue(optionsParseResult, parameter);
            if ( value==null ) {
                var defaultValueExpression = parameter.getDefaultValue();
                value = defaultValueExpression==null 
                        ? null 
                        : spelEvaluator.evaluate(defaultValueExpression, parameters, String.class);
            }
            parameters.set(name, convertParameterValue(value, parameter));
        }
        private String getOptionValue(OptionsParseResult parseResult, ActionParameter parameter) {
            var optionName = ActionParameterHelper.getOptionName(parameter.getName());
            return parseResult.getOptionValuesByName().get(optionName);
        }
        
        private JsonNode convertParameterValue(String value, ActionParameter parameter) {
            var name = parameter.getName();
            var type = StringUtils.isBlank(parameter.getType()) ? "string" : parameter.getType();
            var paramConverter = parameterConverters.get(type);
            if ( paramConverter==null ) {
                throw new ActionValidationException(String.format("Unknown parameter type %s for parameter %s", type, name)); 
            } else {
                var args = ParameterTypeConverterArgs.builder()
                        .progressWriter(progressWriter)
                        .spelEvaluator(spelEvaluator)
                        .action(action)
                        .parameter(parameter)
                        .parameters(parameters)
                        .build();
                var result = paramConverter.apply(value, args);
                return result==null ? NullNode.instance : result; 
            }
        }  
    }
    
    @RequiredArgsConstructor
    private final class ActionAddRequestTargetsProcessor {
        private final ActionData actionData;
        private final void addRequestTargets() {
            var requestTargets = action.getAddRequestTargets();
            if ( requestTargets!=null ) {
                requestTargets.forEach(this::addRequestTarget);
            }
        }
        private void addRequestTarget(ActionRequestTarget descriptor) {
            requestHelpers.put(descriptor.getName(), createBasicRequestHelper(descriptor));
        }
        
        private IActionRequestHelper createBasicRequestHelper(ActionRequestTarget descriptor) {
            var name = descriptor.getName();
            var baseUrl = actionData.eval(descriptor.getBaseUrl(), String.class);
            var headers = actionData.eval(descriptor.getHeaders(), String.class);
            IUnirestInstanceSupplier unirestInstanceSupplier = () -> GenericUnirestFactory.getUnirestInstance(name, u->{
                u.config().defaultBaseUrl(baseUrl).getDefaultHeaders().add(headers);
                UnirestUnexpectedHttpResponseConfigurer.configure(u);
                UnirestJsonHeaderConfigurer.configure(u);
            });
            return new BasicActionRequestHelper(unirestInstanceSupplier, null);
        }
    }
    
    @RequiredArgsConstructor
    private final class ActionStepsProcessor {
        private final ActionData actionData;

        private final void processSteps() {
            processSteps(action.getSteps());
        }
        
        private final void processSteps(List<ActionStep> steps) {
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
                    StringUtils.indent(yamlObjectMapper.valueToTree(value).toPrettyString(), "    "));
            } catch ( Exception e ) {
                cachedString = StringUtils.indent(value.toString(), "  ");
            }
            return cachedString;
        }
        
        private final boolean _if(Object o) {
            if (exitRequested || o==null) { return false; }
            if (o instanceof IActionStepIfSupplier ) {
                var _if = ((IActionStepIfSupplier) o).get_if();
                if ( _if!=null ) {
                    return actionData.eval(_if, Boolean.class);
                }
            }
            return true;
        }
        
        private void processSetStep(ActionStepSet set) {
            var name = set.getName();
            var value = getValue(set);
            actionData.set(name, value);
        }
        
        private void processAppendStep(ActionStepAppend append) {
            var name = append.getName();
            var property = append.getProperty();
            var currentValue = actionData.get(name);
            var valueToAppend = getValue(append);
            if ( property==null ) {
                appendToArray(name, currentValue, valueToAppend);
            } else {
                appendToObject(name, currentValue, actionData.eval(property, String.class), valueToAppend);
            }
        }

        private void appendToArray(String name, JsonNode currentValue, JsonNode valueToAppend) {
            if ( currentValue==null ) {
                currentValue = objectMapper.createArrayNode();
            }
            if ( !currentValue.isArray() ) {
                throw new IllegalStateException("Cannot append value to non-array node "+currentValue.getNodeType());
            } else {
                if ( valueToAppend!=null ) {
                    ((ArrayNode)currentValue).add(valueToAppend);
                }
                actionData.set(name, currentValue); // Update copies in parents
            }
        }
        
        private void appendToObject(String name, JsonNode currentValue, String property, JsonNode valueToAppend) {
            if ( currentValue==null ) {
                currentValue = objectMapper.createObjectNode();
            }
            if ( !currentValue.isObject() ) {
                throw new IllegalStateException(String.format("Cannot append value to non-object node "+currentValue.getNodeType()));
            } else {
                if ( valueToAppend!=null ) {
                    ((ObjectNode)currentValue).set(property, valueToAppend);
                }
                actionData.set(name, currentValue); // Update copies in parents
            }
        }

        private void processUnsetStep(ActionStepUnset unset) {
            actionData.unset(unset.getName());
        }
        
        private JsonNode getValue(IActionStepValueSupplier supplier) {
            var value = supplier.getValue();
            var valueTemplate = supplier.getValueTemplate();
            if ( value!=null ) { return getValue(value); }
            else if ( StringUtils.isNotBlank(valueTemplate) ) { return getTemplateValue(valueTemplate); }
            else { throw new IllegalStateException("Either value or valueTemplate must be specified"); }
        }

        private JsonNode getValue(TemplateExpression valueExpression) {
            return objectMapper.valueToTree(actionData.eval(valueExpression, Object.class));
        }
        
        private final JsonNode getTemplateValue(String templateName) {
            var valueTemplateDescriptor = action.getValueTemplatesByName().get(templateName);
            var outputRawContents = valueTemplateDescriptor.getContents();
            return new JsonNodeOutputWalker(valueTemplateDescriptor, actionData).walk(outputRawContents);
        }
        
        private void processWriteStep(ActionStepWrite write) {
            var to = actionData.eval(write.getTo(), String.class);
            var value = asString(getValue(write));
            try {
                switch (to.toLowerCase()) {
                case "stdout": delayedConsoleWriterRunnables.add(createRunner(stdout, value)); break;
                case "stderr": delayedConsoleWriterRunnables.add(createRunner(stderr, value)); break;
                default: write(new File(to), value);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error writing action output to "+to);
            }
        }
        
        private Runnable createRunner(PrintStream out, String output) {
            return ()->out.print(output);
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
            progressWriter.writeProgress(actionData.eval(progress, String.class));
        }
        
        private void processWarnStep(TemplateExpression progress) {
            progressWriter.writeWarning(actionData.eval(progress, String.class));
        }
        
        private void processDebugStep(TemplateExpression progress) {
            LOG.debug(actionData.eval(progress, String.class));
        }
        
        private void processThrowStep(TemplateExpression message) {
            throw new StepProcessingException(actionData.eval(message, String.class));
        }
        
        private void processExitStep(TemplateExpression exitCodeExpression) {
            exitCode = actionData.eval(exitCodeExpression, Integer.class);
            exitRequested = true;
        }
        
        private void processForEachStep(ActionStepForEach forEach) {
            var processorExpression = forEach.getProcessor();
            var valuesExpression = forEach.getValues();
            if ( processorExpression!=null ) {
                var processor = actionData.eval(processorExpression, IActionStepForEachProcessor.class);
                if ( processor!=null ) { processor.process(node->processForEachStepNode(forEach, node)); }
            } else if ( valuesExpression!=null ) {
                var values = actionData.eval(valuesExpression, ArrayNode.class);
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
            actionData.set(forEach.getName(), node);
            if ( breakIf!=null && actionData.eval(breakIf, Boolean.class) ) {
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
                    ? actionData.eval(passIf, Boolean.class)
                    : !actionData.eval(failIf, Boolean.class);
            var currentStatus = pass ? CheckStatus.PASS : CheckStatus.FAIL;
            checkStatuses.compute(displayName, (name,oldStatus)->CheckStatus.combine(oldStatus, currentStatus));
        }
        
        private void processFcliStep(ActionStepFcli fcli) {
            var args = actionData.eval(fcli.getArgs(), String.class);
            progressWriter.writeProgress("Executing fcli %s", args);
            var cmdExecutor = new FcliCommandExecutor(rootCommandLine, args);
            Consumer<ObjectNode> recordConsumer = null;
            var forEach = fcli.getForEach();
            var name = fcli.getName();
            if ( StringUtils.isNotBlank(name) ) {
                actionData.set(name, objectMapper.createArrayNode());
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
            delayedConsoleWriterRunnables.add(createRunner(System.err, output.getErr()));
            delayedConsoleWriterRunnables.add(createRunner(System.out, output.getOut()));
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
                    appendToArray(name, actionData.get(name), record);
                }
                if ( continueProcessing ) {
                    continueProcessing = processForEachStepNode(fcli.getForEach(), record);
                }
            }
        }

        private void processRequestsStep(List<ActionStepRequest> requests) {
            if ( requests!=null ) {
                var requestsProcessor = new ActionStepRequestsProcessor();
                requestsProcessor.addRequests(requests, this::processResponse, this::processFailure, actionData);
                requestsProcessor.executeRequests();
            }
        }
        
        private final void processResponse(ActionStepRequest requestDescriptor, JsonNode rawBody) {
            var name = requestDescriptor.getName();
            var body = getRequestHelper(requestDescriptor.getTarget()).transformInput(rawBody);
            actionData.setLocal(name+"_raw", rawBody);
            actionData.setLocal(name, body);
            processOnResponse(requestDescriptor);
            processRequestStepForEach(requestDescriptor);
        }
        
        private final void processFailure(ActionStepRequest requestDescriptor, UnirestException e) {
            var onFailSteps = requestDescriptor.getOnFail();
            if ( onFailSteps==null ) { throw e; }
            actionData.setLocal("exception", new POJONode(e));
            processSteps(onFailSteps);
        }
        
        private final void processOnResponse(ActionStepRequest requestDescriptor) {
            var onResponseSteps = requestDescriptor.getOnResponse();
            processSteps(onResponseSteps);
        }
    
        private final void processRequestStepForEach(ActionStepRequest requestDescriptor) {
            var forEach = requestDescriptor.getForEach();
            if ( forEach!=null ) {
                var input = actionData.get(requestDescriptor.getName());
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
            var requestExecutor = new ActionStepRequestsProcessor();
            processRequestStepForEach(forEach, source, getRequestForEachEntryEmbedProcessor(requestExecutor));
            requestExecutor.executeRequests();
        }
        
        @FunctionalInterface
        private interface IRequestStepForEachEntryProcessor {
            void process(ActionStepRequestForEachDescriptor forEach, JsonNode currentNode, ActionData actionData);
        }
        
        private final void processRequestStepForEach(ActionStepRequestForEachDescriptor forEach, ArrayNode source, IRequestStepForEachEntryProcessor entryProcessor) {
            for ( int i = 0 ; i < source.size(); i++ ) {
                var currentNode = source.get(i);
                var newActionData = actionData.createChild();
                newActionData.setLocal(forEach.getName(), currentNode);
                var breakIf = forEach.getBreakIf();
                if ( breakIf!=null && newActionData.eval(breakIf, Boolean.class) ) {
                    break;
                }
                var _if = forEach.get_if(); 
                if ( _if==null || newActionData.eval(_if, Boolean.class) ) {
                    entryProcessor.process(forEach, currentNode, newActionData);
                }
            }
        }
        
        private void updateRequestStepForEachTotalCount(ActionStepRequestForEachDescriptor forEach, ArrayNode array) {
            var totalCountName = String.format("total%sCount", StringUtils.capitalize(forEach.getName()));
            var totalCount = actionData.get(totalCountName);
            if ( totalCount==null ) { totalCount = new IntNode(0); }
            actionData.setLocal(totalCountName, new IntNode(totalCount.asInt()+array.size()));
        }

        private void processRequestStepForEachEntryDo(ActionStepRequestForEachDescriptor forEach, JsonNode currentNode, ActionData newActionData) {
            var processor = new ActionStepsProcessor(newActionData);
            processor.processSteps(forEach.get_do());
        }
        
        private IRequestStepForEachEntryProcessor getRequestForEachEntryEmbedProcessor(ActionStepRequestsProcessor requestExecutor) {
            return (forEach, currentNode, newData) -> {
                if ( !currentNode.isObject() ) {
                    // TODO Improve exception message?
                    throw new IllegalStateException("Cannot embed data on non-object nodes: "+forEach.getName());
                }
                requestExecutor.addRequests(forEach.getEmbed(), 
                        (rd,r)->((ObjectNode)currentNode).set(rd.getName(), getRequestHelper(rd.getTarget()).transformInput(r)), 
                        this::processFailure, newData);
            };
        }
    }
    
    private final class ActionStepRequestsProcessor {
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> simpleRequests = new LinkedHashMap<>();
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> pagedRequests = new LinkedHashMap<>();
        
        private final void addRequests(List<ActionStepRequest> requestDescriptors, BiConsumer<ActionStepRequest, JsonNode> responseConsumer, BiConsumer<ActionStepRequest, UnirestException> failureConsumer, ActionData actionData) {
            if ( requestDescriptors!=null ) {
                requestDescriptors.forEach(r->addRequest(r, responseConsumer, failureConsumer, actionData));
            }
        }
        
        private final void addRequest(ActionStepRequest requestDescriptor, BiConsumer<ActionStepRequest, JsonNode> responseConsumer, BiConsumer<ActionStepRequest, UnirestException> failureConsumer, ActionData actionData) {
            var _if = requestDescriptor.get_if();
            if ( _if==null || actionData.eval(_if, Boolean.class) ) {
                var method = requestDescriptor.getMethod();
                var uri = actionData.eval(requestDescriptor.getUri(), String.class);
                checkUri(uri);
                var query = actionData.eval(requestDescriptor.getQuery(), Object.class);
                var body = requestDescriptor.getBody()==null ? null : actionData.eval(requestDescriptor.getBody(), Object.class);
                var requestData = new IActionRequestHelper.ActionRequestDescriptor(method, uri, query, body, r->responseConsumer.accept(requestDescriptor, r), e->failureConsumer.accept(requestDescriptor, e));
                addPagingProgress(requestData, requestDescriptor.getPagingProgress(), actionData);
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

        private void addPagingProgress(ActionRequestDescriptor requestData, ActionStepRequestPagingProgressDescriptor pagingProgress, ActionData actionData) {
            if ( pagingProgress!=null ) {
                addPagingProgress(pagingProgress.getPrePageLoad(), requestData::setPrePageLoad, actionData);
                addPagingProgress(pagingProgress.getPostPageLoad(), requestData::setPostPageLoad, actionData);
                addPagingProgress(pagingProgress.getPostPageProcess(), requestData::setPostPageProcess, actionData);
            }
        }
        
        private void addPagingProgress(TemplateExpression expr, Consumer<Runnable> consumer, ActionData actionData) {
            if ( expr!=null ) {
                consumer.accept(()->progressWriter.writeProgress(actionData.eval(expr, String.class)));
            }
        }
        
        private final void executeRequests() {
            simpleRequests.entrySet().forEach(e->executeRequest(e.getKey(), e.getValue(), false));
            pagedRequests.entrySet().forEach(e->executeRequest(e.getKey(), e.getValue(), true));
        }
        
        private void executeRequest(String target, List<ActionRequestDescriptor> requests, boolean isPaged) {
            var requestHelper = getRequestHelper(target);
            if ( isPaged ) {
                requests.forEach(r->requestHelper.executePagedRequest(r));
            } else {
                requestHelper.executeSimpleRequests(requests);
            }
        }
    }
    
    public static interface IActionRequestHelper extends AutoCloseable {
        public UnirestInstance getUnirestInstance();
        public JsonNode transformInput(JsonNode input);
        public void executePagedRequest(ActionRequestDescriptor requestDescriptor);
        public void executeSimpleRequests(List<ActionRequestDescriptor> requestDescriptor);
        public void close();
        
        @Data
        public static final class ActionRequestDescriptor {
            private final String method;
            private final String uri;
            private final Map<String, Object> queryParams;
            private final Object body;
            private final Consumer<JsonNode> responseConsumer;
            private final Consumer<UnirestException> failureConsumer;
            private Runnable prePageLoad;
            private Runnable postPageLoad;
            private Runnable postPageProcess;
            
            public void prePageLoad() {
                run(prePageLoad);
            }
            public void postPageLoad() {
                run(postPageLoad);
            }
            public void postPageProcess() {
                run(postPageProcess);
            }
            private void run(Runnable runnable) {
                if ( runnable!=null ) { runnable.run(); }
            }
        }
        
        @RequiredArgsConstructor
        public static class BasicActionRequestHelper implements IActionRequestHelper {
            private final IUnirestInstanceSupplier unirestInstanceSupplier;
            private final IProductHelper productHelper;
            private UnirestInstance unirestInstance;
            public final UnirestInstance getUnirestInstance() {
                if ( unirestInstance==null ) {
                    unirestInstance = unirestInstanceSupplier.getUnirestInstance();
                }
                return unirestInstance;
            }
            
            @Override
            public JsonNode transformInput(JsonNode input) {
                return JavaHelper.as(productHelper, IInputTransformer.class).orElse(i->i).transformInput(input);
            }
            @Override
            public void executePagedRequest(ActionRequestDescriptor requestDescriptor) {
                var unirest = getUnirestInstance();
                INextPageUrlProducer nextPageUrlProducer = (req, resp)->{
                    var nextPageUrl = JavaHelper.as(productHelper, INextPageUrlProducerSupplier.class).get()
                            .getNextPageUrlProducer().getNextPageUrl(req, resp);
                    if ( nextPageUrl!=null ) {
                        requestDescriptor.prePageLoad();
                    }
                    return nextPageUrl;
                };
                HttpRequest<?> request = createRequest(unirest, requestDescriptor);
                requestDescriptor.prePageLoad();
                try {
                    PagingHelper.processPages(unirest, request, nextPageUrlProducer, r->{
                        requestDescriptor.postPageLoad();
                        requestDescriptor.getResponseConsumer().accept(r.getBody());
                        requestDescriptor.postPageProcess();
                    });
                } catch ( UnirestException e ) {
                    requestDescriptor.getFailureConsumer().accept(e);
                }
            }
            @Override
            public void executeSimpleRequests(List<ActionRequestDescriptor> requestDescriptors) {
                var unirest = getUnirestInstance();
                requestDescriptors.forEach(r->executeSimpleRequest(unirest, r));
            }
            private void executeSimpleRequest(UnirestInstance unirest, ActionRequestDescriptor requestDescriptor) {
                try {
                    createRequest(unirest, requestDescriptor)
                        .asObject(JsonNode.class)
                        .ifSuccess(r->requestDescriptor.getResponseConsumer().accept(r.getBody()));
                } catch ( UnirestException e ) {
                    requestDescriptor.getFailureConsumer().accept(e);
                }
            }

            private HttpRequest<?> createRequest(UnirestInstance unirest, ActionRequestDescriptor r) {
                var result = unirest.request(r.getMethod(), r.getUri())
                    .queryString(r.getQueryParams());
                return r.getBody()==null ? result : result.body(r.getBody());
            }

            @Override
            public void close() {
                if ( unirestInstance!=null ) {
                    unirestInstance.close();
                }
            }
        }
    }
    
    @Builder @Data
    public static final class ParameterTypeConverterArgs {
        private final IProgressWriterI18n progressWriter;
        private final ISpelEvaluator spelEvaluator;
        private final Action action;
        private final ActionParameter parameter;
        private final ObjectNode parameters;
    }
    
    private static final Map<String, BiFunction<String, ParameterTypeConverterArgs, JsonNode>> createDefaultParameterConverters() {
        Map<String, BiFunction<String, ParameterTypeConverterArgs, JsonNode>> result = new HashMap<>();
        // TODO Most of these will likely fail in case value is null or empty
        result.put("string",  (v,a)->new TextNode(v));
        result.put("boolean", (v,a)->BooleanNode.valueOf(Boolean.parseBoolean(v)));
        result.put("int",     (v,a)->IntNode.valueOf(Integer.parseInt(v)));
        result.put("long",    (v,a)->LongNode.valueOf(Long.parseLong(v)));
        result.put("double",  (v,a)->DoubleNode.valueOf(Double.parseDouble(v)));
        result.put("float",   (v,a)->FloatNode.valueOf(Float.parseFloat(v)));
        result.put("array",   (v,a)->StringUtils.isBlank(v)
                ? JsonHelper.toArrayNode(new String[] {}) 
                : JsonHelper.toArrayNode(v.split(",")));
        // TODO Add BigIntegerNode/DecimalNode/ShortNode support?
        // TODO Add array support?
        return result;
    }
    
    @RequiredArgsConstructor
    private static final class JsonNodeOutputWalker extends JsonNodeDeepCopyWalker {
        private final ActionValueTemplate outputDescriptor;
        private final ActionData actionData;
        @Override
        protected JsonNode copyValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
            if ( !(node instanceof TextNode) ) {
                return super.copyValue(state, path, parent, node);
            } else {
                TemplateExpression expression = outputDescriptor.getValueExpressions().get(path);
                if ( expression==null ) { throw new RuntimeException("No expression for "+path); }
                try {
                    var rawResult = actionData.eval(expression, Object.class);
                    if ( rawResult instanceof CharSequence ) {
                        rawResult = new TextNode(((String)rawResult).replace("\\n", "\n"));
                    }
                    return objectMapper.valueToTree(rawResult);
                } catch ( SpelEvaluationException e ) {
                    throw new RuntimeException("Error evaluating action expression "+expression.getExpressionString(), e);
                }
            }
        }
    }
    
    public static final class StepProcessingException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public StepProcessingException(String message, Throwable cause) {
            super(message, cause);
        }

        public StepProcessingException(String message) {
            super(message);
        }

        public StepProcessingException(Throwable cause) {
            super(cause);
        }
    }
}
