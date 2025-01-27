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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spring.expression.IConfigurableSpelEvaluator;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

/**
 * This class manages action data that can be stored, formatted, and retrieved during action execution.
 * @author Ruud Senden
 */
public final class ActionRunnerData {
    private static final Logger LOG = LoggerFactory.getLogger(ActionRunnerData.class);
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    private static final String PARAMETERS_VALUE_NAME = "parameters";
    private final ObjectNode values;
    private final IConfigurableSpelEvaluator spelEvaluator;
    private final ActionRunnerData parent;
    
    /**
     * Construct a new instance of this class with the given SpEL evaluator and action parameters.
     * Only a single instance per action run is supposed to be created through this constructor,
     * acting as the top-level or global data object. Children of this top-level data object can
     * be created through the {@link #createChild()} method.
     */
    public ActionRunnerData(IConfigurableSpelEvaluator spelEvaluator, ObjectNode parameters) {
        this.spelEvaluator = spelEvaluator;
        this.values = objectMapper.createObjectNode().set(PARAMETERS_VALUE_NAME, parameters);
        this.parent = null;
    }
    
    /**
     * Constructor solely used by {@link #createChild()}
     */
    private ActionRunnerData(ActionRunnerData parent) {
        this.spelEvaluator = parent.spelEvaluator;
        this.values = JsonHelper.shallowCopy(parent.values);
        this.parent = parent;
    }
    
    /**
     * Create a child of the current {@link ActionRunnerData} instance
     */
    public final ActionRunnerData createChild() {
        return new ActionRunnerData(this);
    }
    
    /**
     * Evaluate the given SpEL expression on current data values,
     * and convert the result to the given return type.
     */
    public final <T> T eval(Expression expression, Class<T> returnType) {
        return spelEvaluator.evaluate(expression, values, returnType);
    }
    
    /**
     * Evaluate the given SpEL expression on current data values,
     * and convert the result to the given return type.
     */
    public final <T> T eval(String expression, Class<T> returnType) {
        return spelEvaluator.evaluate(expression, values, returnType);
    }
    
    public final <T> Map<String, T> eval(Map<String, TemplateExpression> expressions, Class<T> valueType) {
        Map<String, T> result = new LinkedHashMap<>();
        if ( expressions!=null ) {
            expressions.entrySet().forEach(e->result.put(e.getKey(), eval(e.getValue(), valueType)));
        }
        return result;
    }
    
    public final JsonNode get(String name) {
        return values.get(name);
    }
    
    /**
     * Set a data value on both this instance and any parent instances;
     * this method checks for attempts to update 'parameters', logs
     * some details, then defers to {@link #_set(String, JsonNode)} to
     * perform the actual update.
     */
    public final void set(String name, JsonNode value) {
        rejectParametersUpdate(name);
        logDebug(()->String.format("Set %s: %s", name, value.toPrettyString()));
        _set(name, value);
    }

    /**
     * Set a data value on both this instance and any parent instances.
     */
    private void _set(String name, JsonNode value) {
        values.set(name, value);
        if ( parent!=null ) { parent._set(name, value); }
    }
    
    /**
     * Set a data value on this instance only; this method checks for attempts 
     * to update 'parameters', logs some details, then sets the value.
     */
    public final void setLocal(String name, JsonNode value) {
        rejectParametersUpdate(name);
        logDebug(()->String.format("Set Local %s: %s", name, value.toPrettyString()));
        values.set(name, value);
    }
    
    /**
     * Unset a data value on both this instance and any parent instances;
     * this method checks for attempts to update 'parameters', logs
     * some details, then defers to {@link #_unset(String)} to
     * perform the actual update.
     */
    public final void unset(String name) {
        rejectParametersUpdate(name);
        logDebug(()->String.format("Unset %s", name));
        _unset(name);
    }

    /**
     * Unset a data value on both this instance and any parent instances.
     */
    private void _unset(String name) {
        values.remove(name);
        if ( parent!=null ) { parent._unset(name); }
    }
    
    /**
     * If the given name equals 'parameters', this method throws an exception.
     * If we ever implement some action security analysis, we want to consider 
     * user-provided parameter values as 'safe' values in potentially unsafe 
     * actions. As such, actions are not allowed to update the parameters object.
     */
    private static final void rejectParametersUpdate(String name) {
        if ( PARAMETERS_VALUE_NAME.equals(name) ) {
            throw new IllegalStateException("Action steps are not allowed to modify 'parameters'");
        }
    }
    
    private static final void logDebug(Supplier<String> messageSupplier) {
        if ( LOG.isDebugEnabled() ) {
            LOG.debug(messageSupplier.get());
        }
    }
    
}
