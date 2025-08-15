/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.json;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.Expression;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.spel.SpelEvaluator;

import lombok.Getter;

/**
 * This bean provides utility methods for working with Jackson JsonNode trees.
 * 
 * @author Ruud Senden
 *
 */
public class JsonHelper {
    @Getter private static final ObjectMapper objectMapper = _createObjectMapper();
    //private static final Logger LOG = LoggerFactory.getLogger(JsonHelper.class);
    private static final ObjectMapper _createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
    
    public static final <R> R evaluateSpelExpression(JsonNode input, Expression expression, Class<R> returnClass) {
        return SpelEvaluator.JSON_GENERIC.evaluate(expression, input, returnClass);
    }

    public static final <R> R evaluateSpelExpression(JsonNode input, String expression, Class<R> returnClass) {
        return SpelEvaluator.JSON_GENERIC.evaluate(expression, input, returnClass);
    }
    
    public static final Iterable<JsonNode> iterable(ArrayNode arrayNode) {
        Iterator<JsonNode> iterator = arrayNode.iterator();
        return () -> iterator;
    }
    
    public static final Stream<JsonNode> stream(ArrayNode arrayNode) {
        return StreamSupport.stream(iterable(arrayNode).spliterator(), false);
    }
    
    public static final ObjectNode shallowCopy(ObjectNode node) {
        var newData = objectMapper.createObjectNode();
        newData.setAll(node);
        return newData;
    }
    
    public static final ArrayNodeCollector arrayNodeCollector() {
        return new ArrayNodeCollector();
    }
    
    public static final ArrayNode toArrayNode(String... strings) {
        return Stream.of(strings).map(TextNode::new).collect(arrayNodeCollector());
    }
    
    public static final ArrayNode toArrayNode(JsonNode... objects) {
        return Stream.of(objects).collect(arrayNodeCollector());
    }
    
    public static <T> T treeToValue(JsonNode node, Class<T> returnType) {
        if ( node==null ) { return null; }
        try {
            T result = objectMapper.treeToValue(node, returnType);
            if ( result instanceof IJsonNodeHolder ) {
                ((IJsonNodeHolder)result).setJsonNode(node);
            }
            return result;
        } catch (JsonProcessingException jpe ) {
            throw new FcliTechnicalException("Error processing JSON data", jpe);
        }
    }
    
    public static <T> T jsonStringToValue(String jsonString, Class<T> returnType) {
        if ( StringUtils.isBlank(jsonString) ) { return null; }
        try {
            return treeToValue(objectMapper.readTree(jsonString), returnType);
        } catch (JsonProcessingException jpe) {
            throw new FcliTechnicalException("Error processing JSON data", jpe);
        }
    }
    
    private static final class ArrayNodeCollector implements Collector<JsonNode, ArrayNode, ArrayNode> {
        @Override
        public Supplier<ArrayNode> supplier() {
            return objectMapper::createArrayNode;
        }

        @Override
        public BiConsumer<ArrayNode, JsonNode> accumulator() {
            return ArrayNode::add;
        }

        @Override
        public BinaryOperator<ArrayNode> combiner() {
            return (x, y) -> {
                x.addAll(y);
                return x;
            };
        }

        @Override
        public Function<ArrayNode, ArrayNode> finisher() {
            return accumulator -> accumulator;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return EnumSet.of(Characteristics.UNORDERED);
        }
    }
}
