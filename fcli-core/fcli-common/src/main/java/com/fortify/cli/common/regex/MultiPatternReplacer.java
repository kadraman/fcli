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
package com.fortify.cli.common.regex;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fortify.cli.common.exception.FcliBugException;

public final class MultiPatternReplacer {
    private final StringBuilder globalMatchPatternBuilder = new StringBuilder();
    private Pattern globalMatchPattern = null;
    private final Map<String,String> valuesToReplace = new HashMap<>();
    private final Map<Pattern, String> patternsToReplace = new HashMap<>();
    
    public final MultiPatternReplacer registerValue(String valueToMask, String replacement) {
        valuesToReplace.put(valueToMask, replacement);
        registerGlobalMatchPattern(Pattern.quote(valueToMask));
        return this;
    }
    
    public final MultiPatternReplacer registerPattern(String patternString, String defaultReplacement) {
        patternsToReplace.put(Pattern.compile(patternString), defaultReplacement);
        registerGlobalMatchPattern(patternString);
        return this;
    }
    
    private final void registerGlobalMatchPattern(String patternString) {
        if (!globalMatchPatternBuilder.isEmpty()) {
            globalMatchPatternBuilder.append('|');
        }
        globalMatchPatternBuilder.append(patternString);
        globalMatchPattern = Pattern.compile(globalMatchPatternBuilder.toString(), Pattern.MULTILINE); // TODO Make flags configurable?
    }

    public final String applyReplacements(String msg) {
        return applyReplacements(msg, this::registerValue);
    }
    
    public final String applyReplacements(String msg, BiConsumer<String,String> replacementValueConsumer) {
        if ( globalMatchPattern==null ) { return msg; }
        return globalMatchPattern.matcher(msg).replaceAll(mr->applyReplacement(mr, replacementValueConsumer));
    }
    
    private final String applyReplacement(MatchResult mr, BiConsumer<String,String> replacementValueConsumer) {
        var matchingValue = mr.group();
        var result = valuesToReplace.get(matchingValue);
        if ( result == null ) { result = applyGroupReplacements(matchingValue, replacementValueConsumer); }
        return Matcher.quoteReplacement(result);
    }
    
    private String applyGroupReplacements(String value, BiConsumer<String,String> replacementValueConsumer) {
        var entry = findMatchingMatcher(value);
        var matcher = entry.getKey();
        var defaultReplacement = entry.getValue();
        if ( matcher.groupCount()==0 ) {
            return valuesToReplace.getOrDefault(value, defaultReplacement);
        } else {
            StringBuilder sb = new StringBuilder(value);
            for ( var i = 1 ; i <= matcher.groupCount() ; i++ ) {
                var groupValue = matcher.group(i);
                var replacement = valuesToReplace.getOrDefault(groupValue, defaultReplacement);
                sb.replace(matcher.start(i), matcher.end(i), replacement);
                replacementValueConsumer.accept(groupValue, replacement); // Replace value on future calls
            }
            return sb.toString();
        }
    }
    
    private final Map.Entry<Matcher, String> findMatchingMatcher(String value) {
        for ( var entry : patternsToReplace.entrySet() ) {
            var matcher = entry.getKey().matcher(value);
            if ( matcher.matches() ) {
                return new AbstractMap.SimpleEntry<>(matcher, entry.getValue());
            }
        }
        throw new FcliBugException("Can't find pattern that matches "+value);
    }
}
