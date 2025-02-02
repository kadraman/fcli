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
package com.fortify.cli.common.action.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.expression.ParseException;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.crypto.helper.SignatureHelper.PublicKeyDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.json.JsonHelper.AbstractJsonNodeWalker;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

/**
 * This class describes an action deserialized from an action YAML file, 
 * containing elements describing things like:
 * <ul> 
 *  <li>Action metadata like name and description</li> 
 *  <li>Action parameters</li>
 *  <li>Steps to be executed, like executing REST requests and writing output</li>
 *  <li>Value templates</li>
 *  <Various other action configuration elements</li>
 * </ul> 
 *
 * @author Ruud Senden
 */
@Reflectable @NoArgsConstructor
@Data
@JsonClassDescription("Fortify CLI action definition")
public class Action implements IActionElement {
    @JsonPropertyDescription("Required string unless `yaml-language-server` comment with schema location is provided: Schema location.")
    @JsonProperty(value = "$schema", required=false) public String schema;
    
    @JsonPropertyDescription("Required string: Author of this action.")
    @JsonProperty(required = true) private String author;
    
    @JsonPropertyDescription("Required object: Action usage help.")
    @JsonProperty(required = true) private ActionUsage usage;
    
    @JsonPropertyDescription("Optional object: Action configuration properties.")
    @JsonProperty(required = false) private ActionConfig config;
    
    @JsonPropertyDescription("""
        Optional map: CLI options accepted by this action. Map keys define option names, values define option \
        definitions. Option values can be referenced by action steps through the 'cli' variable, for example \
        ${cli.myOption} or ${cli['my-option']}.   
        """)
    @JsonProperty(value = "cli.options", required = false) private Map<String, ActionCliOptions> cliOptions;
    
    @JsonPropertyDescription("Required list: Steps to be executed when this action is being run.")
    @JsonProperty(required = true) private List<ActionStep> steps;
    
    @JsonPropertyDescription("""
        Optional map: Formatters that can be referenced in action steps to format data. Map keys define formatter \
        name, map values define how the data should be formatted. The outcome of a formatter can either be a \
        simple string, or a structured (JSON) object or array.
        """)
    @JsonProperty(required = false) private Map<String, JsonNode> formatters;
    
    @JsonIgnore ActionMetadata metadata;
    /** Maps/Collections listing action elements. 
     *  These get filled by the {@link #visit(Action, Object)} method. */ 
    @ToString.Exclude @JsonIgnore private final List<IActionElement> allActionElements = new ArrayList<>();
    /** Cached mapping from text node property path to corresponding TemplateExpression instance */  
    @JsonIgnore private final Map<String, TemplateExpression> formatterExpressions = new LinkedHashMap<>();
    
    public List<IActionElement> getAllActionElements() {
        return Collections.unmodifiableList(allActionElements);
    }
    
    /**
     * This method is invoked by ActionHelper after deserializing an instance 
     * of this class from a YAML file. It performs the following tasks:
     * <ul>
     *  <li>Set the given attributes on this action</li>
     *  <li>Initialize the collections above</li>
     *  <li>Validate required action elements are present</li>
     *  <li>Invoke the {@link IActionElement#postLoad(Action)} method
     *      for every {@link IActionElement} collected during collection
     *      initialization</li>
     * </ul>
     * We need to initialize collections before invoking {@link IActionElement#postLoad(Action)}
     * methods, as these methods may utilize the collections.
     * @param signatureStatus 
     */
    public final void postLoad(ActionMetadata metadata) {
        this.metadata = metadata;
        initializeCollections();
        allActionElements.forEach(elt->elt.postLoad(this));
    }
    
    /**
     * {@link IActionElement#postLoad(Action)} implementation
     * for this root action element, checking required elements
     * are present.
     */
    public final void postLoad(Action action) {
        checkNotNull("action usage", usage, this);
        checkNotNull("action steps", steps, this);
        if ( cliOptions==null ) {
            cliOptions = Collections.emptyMap();
        } if ( config==null ) {
            config = new ActionConfig();
        }
        if ( formatters!=null ) {
            var formatterContentsWalker = new FormatterContentsWalker();
            formatters.values().forEach(formatterContentsWalker::walk);
        }
    }
    
    /**
     * Utility method for throwing an {@link ActionValidationException}
     * if the given boolean value is true.
     * @param isFailure
     * @param entity
     * @param msgSupplier
     */
    static final void throwIf(boolean isFailure, Object entity, Supplier<String> msgSupplier) {
        if ( isFailure ) {
            throw new ActionValidationException(msgSupplier.get(), entity);
        }
    }
    
    /**
     * Utility method for checking whether the given value is not blank, throwing an
     * exception otherwise.
     * @param property Descriptive name of the YAML property being checked
     * @param value Value to be checked for not being blank
     * @param entity The object containing the property to be checked
     */
    static final void checkNotBlank(String property, String value, Object entity) {
        throwIf(StringUtils.isBlank(value), entity, ()->String.format("Action %s property must be specified", property));
    }
    
    /**
     * Utility method for checking whether the given value is not null, throwing an
     * exception otherwise.
     * @param property Descriptive name of the YAML property being checked
     * @param value Value to be checked for not being null
     * @param entity The object containing the property to be checked
     */
    static final void checkNotNull(String property, Object value, Object entity) {
        throwIf(value==null, entity, ()->String.format("Action %s property must be specified", property));
    }
    
    /**
     * Initialize the {@link #allActionElements} and {@link #formattersByName}
     * collections, using the reflective visit methods. We use reflection as
     * manually navigating the action element tree proved to be too error-prone,
     * often forgetting to handle newly added action element types.
     */
    private void initializeCollections() {
        visit(this, this, elt->{
            allActionElements.add(elt);
        });
    }
    
    /**
     * Visit the given action element. If the action element implements 
     * the {@link IActionElement} interface (i.e., it's not a simple value 
     * like String or TemplateExpression), it is passed to the given consumer
     * and subsequently we recurse into all action element fields.
     * If the given action element is a collection, we recurse into each
     * collection element.
     */
    private final void visit(Action action, Object actionElement, Consumer<IActionElement> consumer) {
        if ( actionElement!=null ) {
            if ( actionElement instanceof IActionElement ) {
                var instance = (IActionElement)actionElement;
                consumer.accept(instance);
                visitFields(action, instance.getClass(), instance, consumer);
            } else if ( actionElement instanceof Collection ) {
                ((Collection<?>)actionElement).stream()
                    .forEach(o->visit(action, o, consumer));
            }
        }
    }

    /**
     * Visit all fields of the given class, with field values being
     * retrieved from the given action element. 
     */
    private void visitFields(Action action, Class<?> clazz, Object actionElement, Consumer<IActionElement> consumer) {
        if ( clazz!=null && IActionElement.class.isAssignableFrom(clazz) ) {
            // Visit fields provided by any superclasses of the given class.
            visitFields(action, clazz.getSuperclass(), actionElement, consumer);
            // Iterate over all declared fields, and invoke the
            // postLoad(action, actionElement) for each field value.
            Stream.of(clazz.getDeclaredFields())
                .peek(f->f.setAccessible(true))
                .filter(f->f.getAnnotation(JsonIgnore.class)==null)
                .map(f->getFieldValue(actionElement, f))
                .forEach(o->visit(action, o, consumer));
        }
    }

    /**
     * Get the value for the given field from the given action element.
     * Only reason to have this method is to handle any exceptions
     * through {@link SneakyThrows}, to allow getting field values in
     * lambda expressions like above.
     */
    @SneakyThrows
    private Object getFieldValue(Object actionElement, Field field) {
        return field.get(actionElement);
    }
    
    @Reflectable @Data @Builder(toBuilder = true) @AllArgsConstructor
    public static final class ActionMetadata {
        private final String name;
        private final boolean custom;
        private final SignatureDescriptor signatureDescriptor;
        private final PublicKeyDescriptor publicKeyDescriptor;
        @Builder.Default private final SignatureStatus signatureStatus = SignatureStatus.NOT_VERIFIED;
        
        public static final ActionMetadata create(boolean custom) {
            return builder().custom(custom).build();
        }
    }
    
    private final class FormatterContentsWalker extends AbstractJsonNodeWalker<Void, Void> {
        @Override
        protected Void getResult() { return null; }
        @Override
        protected void walkValue(Void state, String path, JsonNode parent, ValueNode node) {
            if ( node instanceof TextNode ) {
                var expr = node.asText();
                try {
                    formatterExpressions.put(path, SpelHelper.parseTemplateExpression(expr));
                } catch (ParseException e) {
                    throw new ActionValidationException(String.format("Error parsing template expression '%s'", expr), this, e);
                }
            }
            super.walkValue(state, path, parent, node);
        }
    }
}
