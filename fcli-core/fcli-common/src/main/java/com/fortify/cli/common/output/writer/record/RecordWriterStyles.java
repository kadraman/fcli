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
public final class RecordWriterStyles {
    private final Map<RecordWriterStyleGroup, RecordWriterStyle> stylesByGroup;
    
    public static final RecordWriterStyles apply(String... styles) {
        if ( styles==null ) { return none(); }
        return apply(Arrays.stream(styles)
                    .map(RecordWriterStyle::asStyle)
                    .toArray(RecordWriterStyle[]::new));
    }
    
    public static final RecordWriterStyles apply(RecordWriterStyle... styles) {
        if ( styles==null ) { return none(); }
        var stylesByGroup = new HashMap<RecordWriterStyleGroup, RecordWriterStyle>();
        for (var style : styles) {
            var existingStyle = stylesByGroup.get(style.getGroup());
            if ( existingStyle!=null && existingStyle!=style ) {
                throw new FcliSimpleException("Conflicting styles specified: %s, %s", existingStyle, style);
            }
            stylesByGroup.put(style.getGroup(), style);
        }
        return new RecordWriterStyles(stylesByGroup);
    }
    
    public static final RecordWriterStyles none() {
        return new RecordWriterStyles(new HashMap<RecordWriterStyleGroup, RecordWriterStyle>());
    }
    
    public RecordWriterStyles applyDefaultStyles(RecordWriterStyle... styles) {
        if ( styles!=null ) {
            for (var style : styles) {
                var group = style.getGroup();
                if ( !stylesByGroup.containsKey(group) ) {
                    stylesByGroup.put(group, style);
                }
            }
        }
        return this; // Allow for chaining
    }
    
    public final boolean withHeaders() {
        return getOrDefault(RecordWriterStyleGroup.HEADER)==RecordWriterStyle.header;
    }
    
    public final boolean isPretty() {
        return getOrDefault(RecordWriterStyleGroup.PRETTY)==RecordWriterStyle.pretty;
    }
    
    public final boolean isFlat() {
        return getOrDefault(RecordWriterStyleGroup.FLAT)==RecordWriterStyle.flat;
    }
    
    public final boolean isArray() {
        return getOrDefault(RecordWriterStyleGroup.SINGULAR)==RecordWriterStyle.array;
    }
    
    private final RecordWriterStyle getOrDefault(RecordWriterStyleGroup group) {
        return stylesByGroup.getOrDefault(group, group.defaultStyle());
    }
    
    @RequiredArgsConstructor
    public static enum RecordWriterStyle {
        header(RecordWriterStyleGroup.HEADER), no_header(RecordWriterStyleGroup.HEADER),
        pretty(RecordWriterStyleGroup.PRETTY), no_pretty(RecordWriterStyleGroup.PRETTY),
        flat(RecordWriterStyleGroup.FLAT), no_flat(RecordWriterStyleGroup.FLAT),
        array(RecordWriterStyleGroup.SINGULAR), single(RecordWriterStyleGroup.SINGULAR);
        
        @Getter private final RecordWriterStyleGroup group;
        
        public String toString() {
            return name().replace('_', '-');
        }
        
        public static final RecordWriterStyle asStyle(String s) {
            return valueOf(RecordWriterStyle.class, s.replace('-', '_'));
        }
    }
    
    @RequiredArgsConstructor
    public static enum RecordWriterStyleGroup {
        HEADER("header"), 
        PRETTY("pretty"), 
        FLAT("no-flat"), 
        SINGULAR("array");
        
        private final String defaultStyleName;
        
        public RecordWriterStyle defaultStyle() {
            return RecordWriterStyle.asStyle(defaultStyleName);
        }
    }
}
