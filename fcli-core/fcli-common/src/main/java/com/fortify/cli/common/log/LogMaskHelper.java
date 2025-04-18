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
package com.fortify.cli.common.log;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.regex.MultiPatternReplacer;
import com.fortify.cli.common.util.JavaHelper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LogMaskHelper {
    public static final LogMaskHelper INSTANCE = new LogMaskHelper();
    public static final String URL_HOSTNAME_PATTERN = "https?://([^/]+).*";
    
    @Setter private LogMaskLevel logMaskLevel;
    private final Map<LogMessageType, MultiPatternReplacer> multiPatternReplacers = new HashMap<>();
    
    public final LogMaskHelper registerValue(MaskValue maskAnnotation, String source, Object value) {
        var valueString = valueAsString(value);
        var patternString = maskAnnotation.pattern(); 
        if ( StringUtils.isNotBlank(patternString) ) {
            var matcher = Pattern.compile(patternString).matcher(valueString);
            if ( matcher.matches() ) {
                valueString = matcher.group(1);
            }
        }
        return registerValue(maskAnnotation.sensitivity(), valueString, String.format("<REDACTED %s (%s)>", maskAnnotation.description().toUpperCase(), source.toUpperCase()));
    }
    
    private final String valueAsString(Object value) {
        return JavaHelper.as(value, String.class).orElseGet(
            ()->JavaHelper.as(value, char[].class).map(ca->String.valueOf(ca))
            .orElseThrow(()->new FcliBugException("MaskValue annotation can only be used on String or char[] fields")));
    }
    
    public final LogMaskHelper registerValue(LogSensitivityLevel sensitivityLevel, String valueToMask, String replacement, LogMessageType... logMessageTypes) {
        if ( isMaskingNeeded(sensitivityLevel) ) {
            for ( var logMessageType : getLogMessageTypesOrDefault(logMessageTypes) ) {
                getMultiPatternReplacer(logMessageType)
                    .registerValue(valueToMask, replacement)
                    .registerValue(URLEncoder.encode(valueToMask, StandardCharsets.UTF_8), replacement);
            }
        }
        return this;
    }
    
    public final LogMaskHelper registerPattern(LogSensitivityLevel sensitivityLevel, String patternString, String replacement, LogMessageType... logMessageTypes) {
        if ( isMaskingNeeded(sensitivityLevel) ) {
            for ( var logMessageType : getLogMessageTypesOrDefault(logMessageTypes) ) {
                getMultiPatternReplacer(logMessageType).registerPattern(patternString, replacement);
            }
        }
        return this;
    }

    private LogMessageType[] getLogMessageTypesOrDefault(LogMessageType... logMessageTypes) {
        return logMessageTypes!=null && logMessageTypes.length!=0 ? logMessageTypes : LogMessageType.all();
    }
    
    private final MultiPatternReplacer getMultiPatternReplacer(LogMessageType logMessageType) {
        return multiPatternReplacers.computeIfAbsent(logMessageType, t->new MultiPatternReplacer());
    }
    
    private final boolean isMaskingNeeded(LogSensitivityLevel sensitivityLevel) {
        return logMaskLevel.getSensitivityLevels().contains(sensitivityLevel);
    }

    public final String mask(LogMessageType logMessageType, String msg) {
        var multiPattern = getMultiPatternReplacer(logMessageType);
        if ( multiPattern==null ) { return null; }
        try {
            return multiPattern.applyReplacements(msg,
                // We want to register replacement values for all log message types, not just
                // the log message type on which the replacement was applied. We don't know the
                // original sensitivity level here, but if the value was replaced before, it should
                // always be replaced again, hence we use sensitivity level 'high'.        
                (v,r)->registerValue(LogSensitivityLevel.high, v, r));
        } catch ( FcliBugException e ) {
            // Exceptions will be eaten by the logging framework, causing the log message to be lost,
            // so instead we return a fixed string indicating that an fcli bug occurred.
            return "<MASKED DUE TO FCLI BUG>";
        }
    }
    
    public static enum LogMaskLevel {
        high(LogSensitivityLevel.high, LogSensitivityLevel.medium, LogSensitivityLevel.low), 
        medium(LogSensitivityLevel.high, LogSensitivityLevel.medium), 
        low(LogSensitivityLevel.high), 
        none;
        
        @Getter private final Set<LogSensitivityLevel> sensitivityLevels;
        private LogMaskLevel(LogSensitivityLevel... sensitivityLevels) {
            this.sensitivityLevels = sensitivityLevels==null ? Collections.emptySet() : Set.of(sensitivityLevels);
        }
    }
    
    public static enum LogSensitivityLevel {
        high, medium, low;
    }
    

}
