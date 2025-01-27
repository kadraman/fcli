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
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionParameter;
import com.fortify.cli.common.action.model.ActionValidationException;
import com.fortify.cli.common.action.runner.ActionRunnerConfig;
import com.fortify.cli.common.action.runner.ActionRunnerConfig.ParameterTypeConverterArgs;
import com.fortify.cli.common.cli.util.SimpleOptionsParser;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.IOptionDescriptor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionDescriptor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionsParseResult;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.spring.expression.IConfigurableSpelEvaluator;
import com.fortify.cli.common.util.StringUtils;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import lombok.Builder;

@Builder
public final class ActionParameterProcessor {
    private final ActionRunnerConfig config;
    private final IConfigurableSpelEvaluator spelEvaluator;
    private final IProgressWriterI18n progressWriter;

    public final ObjectNode parseParameterValues(String[] args) {
        var parameterValues = JsonHelper.getObjectMapper().createObjectNode();
        var optionsParseResult = _parseParameterValues(parameterValues, args);
        if ( optionsParseResult.hasValidationErrors() ) {
            throw config.getOnValidationErrors().apply(optionsParseResult);
        }
        config.getAction().getParameters().forEach(p->addParameterValues(optionsParseResult, p, parameterValues));
        return parameterValues;
    }
    
    private final OptionsParseResult _parseParameterValues(ObjectNode parameterValues, String[] args) {
        List<IOptionDescriptor> optionDescriptors = ActionParameterHelper.getOptionDescriptors(config.getAction());
        var parseResult = new SimpleOptionsParser(optionDescriptors).parse(args);
        addDefaultValues(parseResult, parameterValues);
        addValidationMessages(parseResult);
        return parseResult;
    }

    private final void addDefaultValues(OptionsParseResult parseResult, ObjectNode parameterValues) {
        config.getAction().getParameters().forEach(p->addDefaultValue(parseResult, p, parameterValues));
    }
    
    private final void addValidationMessages(OptionsParseResult parseResult) {
        config.getAction().getParameters().forEach(p->addValidationMessages(parseResult, p));
    }
    
    private final void addDefaultValue(OptionsParseResult parseResult, ActionParameter parameter, ObjectNode parameterValues) {
        var name = parameter.getName();
        var value = getOptionValue(parseResult, parameter);
        if ( value==null ) {
            var defaultValueExpression = parameter.getDefaultValue();
            value = defaultValueExpression==null 
                    ? null 
                    : spelEvaluator.evaluate(defaultValueExpression, parameterValues, String.class);
        }
        parseResult.getOptionValuesByName().put(ActionParameterHelper.getOptionName(name), value);
    }
    
    private final void addValidationMessages(OptionsParseResult parseResult, ActionParameter parameter) {
        if ( parameter.isRequired() && StringUtils.isBlank(getOptionValue(parseResult, parameter)) ) {
            parseResult.getValidationErrors().add("No value provided for required option "+
                    ActionParameterHelper.getOptionName(parameter.getName()));                
        }
    }

    private final void addParameterValues(OptionsParseResult optionsParseResult, ActionParameter parameter, ObjectNode parameterValues) {
        var name = parameter.getName();
        var value = getOptionValue(optionsParseResult, parameter);
        if ( value==null ) {
            var defaultValueExpression = parameter.getDefaultValue();
            value = defaultValueExpression==null 
                    ? null 
                    : spelEvaluator.evaluate(defaultValueExpression, parameterValues, String.class);
        }
        parameterValues.set(name, convertParameterValue(value, parameter, parameterValues));
    }
    private String getOptionValue(OptionsParseResult parseResult, ActionParameter parameter) {
        var optionName = ActionParameterHelper.getOptionName(parameter.getName());
        return parseResult.getOptionValuesByName().get(optionName);
    }
    
    private JsonNode convertParameterValue(String value, ActionParameter parameter, ObjectNode parameterValues) {
        var name = parameter.getName();
        var type = StringUtils.isBlank(parameter.getType()) ? "string" : parameter.getType();
        var paramConverter = config.getParameterConverters().get(type);
        if ( paramConverter==null ) {
            throw new ActionValidationException(String.format("Unknown parameter type %s for parameter %s", type, name)); 
        } else {
            var args = ParameterTypeConverterArgs.builder()
                    .progressWriter(progressWriter)
                    .spelEvaluator(spelEvaluator)
                    .action(config.getAction())
                    .parameter(parameter)
                    .parameters(parameterValues)
                    .build();
            var result = paramConverter.apply(value, args);
            return result==null ? NullNode.instance : result; 
        }
    }
    
    public static final class ActionParameterHelper {
        private ActionParameterHelper() {}
        
        public static final List<IOptionDescriptor> getOptionDescriptors(Action action) {
            var parameters = action.getParameters();
            List<IOptionDescriptor> result = new ArrayList<>(parameters.size());
            parameters.forEach(p->addOptionDescriptor(result, p));
            return result;
        }

        private static final void addOptionDescriptor(List<IOptionDescriptor> result, ActionParameter parameter) {
            result.add(OptionDescriptor.builder()
                    .name(getOptionName(parameter.getName()))
                    .aliases(getOptionAliases(parameter.getCliAliasesArray()))
                    .description(parameter.getDescription())
                    .bool(parameter.getType()!=null && parameter.getType().equals("boolean"))
                    .build());
        }
        
        static final String getOptionName(String parameterNameOrAlias) {
            var prefix = parameterNameOrAlias.length()==1 ? "-" : "--";
            return prefix+parameterNameOrAlias;
        }
        
        private static final List<String> getOptionAliases(String[] aliases) {
            return aliases==null ? null : Stream.of(aliases).map(ActionParameterHelper::getOptionName).toList();
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
}