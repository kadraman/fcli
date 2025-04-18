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

import org.apache.commons.lang3.StringUtils;

import ch.qos.logback.classic.spi.ILoggingEvent;

public enum LogMessageType {
    FCLI, HTTP_IN, HTTP_OUT, OTHER;
    
    private final String fixedLengthString;
    LogMessageType() {
        this.fixedLengthString = StringUtils.rightPad(this.name(), 8);
    }
    
    public final String toFixedLengthString() {
        return fixedLengthString;
    }
    
    public static final LogMessageType[] all() { return values(); }
    
    public static final LogMessageType getType(ILoggingEvent event) {
        var loggerName = event.getLoggerName();
        if ( loggerName!=null ) {
            if ( loggerName.startsWith("com.fortify.cli") ) {
                return LogMessageType.FCLI;
            } else if ( loggerName.startsWith("org.apache.http") && 
                    (loggerName.endsWith("headers") || loggerName.endsWith("wire")) ) {
                return StringUtils.substringAfter(event.getFormattedMessage(), " ").startsWith("<<") 
                        ? LogMessageType.HTTP_IN : LogMessageType.HTTP_OUT; 
            }
        }
        return LogMessageType.OTHER;
    }
}