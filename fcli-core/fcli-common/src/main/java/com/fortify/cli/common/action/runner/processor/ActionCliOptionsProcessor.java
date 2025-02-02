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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionCliOptions;
import com.fortify.cli.common.action.model.ActionValidationException;
import com.fortify.cli.common.action.runner.ActionRunnerConfig;
import com.fortify.cli.common.cli.util.SimpleOptionsParser;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.IOptionDescriptor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionDescriptor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionsParseResult;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spring.expression.IConfigurableSpelEvaluator;
import com.fortify.cli.common.util.StringUtils;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import lombok.Builder;

@Builder
public final class ActionCliOptionsProcessor {
    private final ActionRunnerConfig config;
    private final IConfigurableSpelEvaluator spelEvaluator;
    private static final Map<String, Function<String, JsonNode>> parameterConverters = createParameterConverters();

    public final ObjectNode parseParameterValues(String[] args) {
        var parameterValues = JsonHelper.getObjectMapper().createObjectNode();
        var optionsParseResult = _parseParameterValues(parameterValues, args);
        if ( optionsParseResult.hasValidationErrors() ) {
            throw config.getOnValidationErrors().apply(optionsParseResult);
        }
        config.getAction().getCliOptions().entrySet().forEach(e->addParameterValues(optionsParseResult, e.getKey(), e.getValue(), parameterValues));
        return parameterValues;
    }
    
    private final OptionsParseResult _parseParameterValues(ObjectNode result, String[] args) {
        List<IOptionDescriptor> optionDescriptors = ActionParameterHelper.getOptionDescriptors(config.getAction());
        var parseResult = new SimpleOptionsParser(optionDescriptors).parse(args);
        addDefaultValues(parseResult, result);
        addValidationMessages(parseResult);
        return parseResult;
    }

    private final void addDefaultValues(OptionsParseResult parseResult, ObjectNode parameterValues) {
        config.getAction().getCliOptions().entrySet().forEach(e->addDefaultValue(parseResult, e.getKey(), e.getValue(), parameterValues));
    }
    
    private final void addValidationMessages(OptionsParseResult parseResult) {
        config.getAction().getCliOptions().entrySet().forEach(e->addValidationMessages(parseResult, e.getKey(), e.getValue()));
    }
    
    private final void addDefaultValue(OptionsParseResult parseResult, String name, ActionCliOptions parameter, ObjectNode parameterValues) {
        var value = getOptionValue(parseResult, name, parameter);
        if ( value==null ) {
            var defaultValueExpression = parameter.getDefaultValue();
            value = defaultValueExpression==null 
                    ? null 
                    : spelEvaluator.evaluate(defaultValueExpression, parameterValues, String.class);
        }
        parseResult.getOptionValuesByName().put(ActionParameterHelper.getOptionName(name), value);
    }
    
    private final void addValidationMessages(OptionsParseResult parseResult, String name, ActionCliOptions parameter) {
        if ( parameter.isRequired() && StringUtils.isBlank(getOptionValue(parseResult, name, parameter)) ) {
            parseResult.getValidationErrors().add("No value provided for required option "+
                    ActionParameterHelper.getOptionName(name));                
        }
    }

    private final void addParameterValues(OptionsParseResult optionsParseResult, String name, ActionCliOptions parameter, ObjectNode parameterValues) {
        var value = getOptionValue(optionsParseResult, name, parameter);
        if ( value==null ) {
            var defaultValueExpression = parameter.getDefaultValue();
            value = defaultValueExpression==null 
                    ? null 
                    : spelEvaluator.evaluate(defaultValueExpression, parameterValues, String.class);
        }
        parameterValues.set(name, convertParameterValue(value, name, parameter, parameterValues));
    }
    private String getOptionValue(OptionsParseResult parseResult, String name, ActionCliOptions parameter) {
        var optionName = ActionParameterHelper.getOptionName(name);
        return parseResult.getOptionValuesByName().get(optionName);
    }
    
    private JsonNode convertParameterValue(String value, String name, ActionCliOptions parameter, ObjectNode parameterValues) {
        var type = StringUtils.isBlank(parameter.getType()) ? "string" : parameter.getType();
        var paramConverter = parameterConverters.get(type);
        if ( paramConverter==null ) {
            throw new ActionValidationException(String.format("Unknown parameter type %s for parameter %s", type, name)); 
        } else {
            var result = paramConverter.apply(value);
            return result==null ? NullNode.instance : result; 
        }
    }
    
    public static final class ActionParameterHelper {
        private ActionParameterHelper() {}
        
        public static final List<IOptionDescriptor> getOptionDescriptors(Action action) {
            var parameters = action.getCliOptions();
            List<IOptionDescriptor> result = new ArrayList<>(parameters.size());
            parameters.entrySet().forEach(e->addOptionDescriptor(result, e.getKey(), e.getValue()));
            return result;
        }

        private static final void addOptionDescriptor(List<IOptionDescriptor> result, String name, ActionCliOptions parameter) {
            result.add(OptionDescriptor.builder()
                    .name(getOptionName(name))
                    .alias(getOptionName(parameter.getAlias()))
                    .description(parameter.getDescription())
                    .bool(parameter.getType()!=null && parameter.getType().equals("boolean"))
                    .build());
        }
        
        static final String getOptionName(String parameterNameOrAlias) {
            if ( StringUtils.isBlank(parameterNameOrAlias) ) { return null; }
            var prefix = parameterNameOrAlias.length()==1 ? "-" : "--";
            return prefix+parameterNameOrAlias;
        }
        
        public static final String getSupportedOptionsTable(Action action) {
            return getSupportedOptionsTable(getOptionDescriptors(action));
        }
        
        public static final String getSupportedOptionsTable(List<IOptionDescriptor> options) {
            return AsciiTable.builder()
                .border(AsciiTable.NO_BORDERS)
                .data(new Column[] {
                        new Column().dataAlign(HorizontalAlign.LEFT),
                        new Column().dataAlign(HorizontalAlign.LEFT),
                    },
                    options.stream()
                        .map(option->new String[] {option.getOptionNamesAndAliasesString(" | "), option.getDescription()})
                        .toList().toArray(String[][]::new))
                .asString();
        }
    }
    
    private static final Map<String, Function<String, JsonNode>> createParameterConverters() {
        Map<String, Function<String, JsonNode>> result = new HashMap<>();
        // TODO Most of these will likely fail in case value is null or empty
        result.put("string",  v->new TextNode(v));
        result.put("boolean", v->BooleanNode.valueOf(Boolean.parseBoolean(v)));
        result.put("int",     v->IntNode.valueOf(Integer.parseInt(v)));
        result.put("long",    v->LongNode.valueOf(Long.parseLong(v)));
        result.put("double",  v->DoubleNode.valueOf(Double.parseDouble(v)));
        result.put("float",   v->FloatNode.valueOf(Float.parseFloat(v)));
        result.put("array",   v->StringUtils.isBlank(v)
                ? JsonHelper.toArrayNode(new String[] {}) 
                : JsonHelper.toArrayNode(v.split(",")));
        // TODO Add BigIntegerNode/DecimalNode/ShortNode support?
        return result;
    }
}