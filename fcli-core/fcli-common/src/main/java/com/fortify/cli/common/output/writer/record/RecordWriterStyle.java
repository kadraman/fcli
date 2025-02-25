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
package com.fortify.cli.common.output.writer.record;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fortify.cli.common.exception.FcliSimpleException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecordWriterStyle {
    private final Map<RecordWriterStyleElementGroup, RecordWriterStyleElement> styleElementsByGroup;
    
    public static final RecordWriterStyle apply(String... styleElements) {
        if ( styleElements==null ) { return none(); }
        return apply(Arrays.stream(styleElements)
                    .map(RecordWriterStyleElement::asStyleElement)
                    .toArray(RecordWriterStyleElement[]::new));
    }
    
    public static final RecordWriterStyle apply(RecordWriterStyleElement... styleElements) {
        if ( styleElements==null ) { return none(); }
        var styleElementsByGroup = new HashMap<RecordWriterStyleElementGroup, RecordWriterStyleElement>();
        for (var styleElement : styleElements) {
            var existingStyleElement = styleElementsByGroup.get(styleElement.getGroup());
            if ( existingStyleElement!=null && existingStyleElement!=styleElement ) {
                throw new FcliSimpleException("Conflicting style elements specified: %s, %s", existingStyleElement, styleElement);
            }
            styleElementsByGroup.put(styleElement.getGroup(), styleElement);
        }
        return new RecordWriterStyle(styleElementsByGroup);
    }
    
    public static final RecordWriterStyle none() {
        return new RecordWriterStyle(new HashMap<RecordWriterStyleElementGroup, RecordWriterStyleElement>());
    }
    
    public RecordWriterStyle applyDefaultStyleElements(RecordWriterStyleElement... styleElements) {
        if ( styleElements!=null ) {
            for (var styleElement : styleElements) {
                var group = styleElement.getGroup();
                if ( !styleElementsByGroup.containsKey(group) ) {
                    styleElementsByGroup.put(group, styleElement);
                }
            }
        }
        return this; // Allow for chaining
    }
    
    public final boolean withHeaders() {
        return getOrDefault(RecordWriterStyleElementGroup.HEADER)==RecordWriterStyleElement.header;
    }
    
    public final boolean isPretty() {
        return getOrDefault(RecordWriterStyleElementGroup.PRETTY)==RecordWriterStyleElement.pretty;
    }
    
    public final boolean isFlat() {
        return getOrDefault(RecordWriterStyleElementGroup.FLAT)==RecordWriterStyleElement.flat;
    }
    
    public final boolean isArray() {
        return getOrDefault(RecordWriterStyleElementGroup.SINGULAR)==RecordWriterStyleElement.array;
    }
    
    private final RecordWriterStyleElement getOrDefault(RecordWriterStyleElementGroup group) {
        return styleElementsByGroup.getOrDefault(group, group.defaultStyle());
    }
    
    @RequiredArgsConstructor
    public static enum RecordWriterStyleElement {
        header(RecordWriterStyleElementGroup.HEADER), no_header(RecordWriterStyleElementGroup.HEADER),
        pretty(RecordWriterStyleElementGroup.PRETTY), no_pretty(RecordWriterStyleElementGroup.PRETTY),
        flat(RecordWriterStyleElementGroup.FLAT), no_flat(RecordWriterStyleElementGroup.FLAT),
        array(RecordWriterStyleElementGroup.SINGULAR), single(RecordWriterStyleElementGroup.SINGULAR);
        
        @Getter private final RecordWriterStyleElementGroup group;
        
        public String toString() {
            return name().replace('_', '-');
        }
        
        public static final RecordWriterStyleElement asStyleElement(String s) {
            return valueOf(RecordWriterStyleElement.class, s.replace('-', '_'));
        }
    }
    
    @RequiredArgsConstructor
    public static enum RecordWriterStyleElementGroup {
        HEADER("header"), 
        PRETTY("pretty"), 
        FLAT("no-flat"), 
        SINGULAR("array");
        
        private final String defaultStyleElementName;
        
        public RecordWriterStyleElement defaultStyle() {
            return RecordWriterStyleElement.asStyleElement(defaultStyleElementName);
        }
    }
}
