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
package com.fortify.cli.common.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class FcliException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    @Getter @Setter @Accessors(fluent = true) private boolean includeFullStackTrace;

    public FcliException() {}

    public FcliException(String message) {
        super(message);
    }

    public FcliException(Throwable cause) {
        super(cause);
    }

    public FcliException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public String getSummaryWithCause() {
        var cause = getCause();
        var causeString = cause==null ? "" : String.format("\nCaused by: %s", ExceptionUtils.getStackTrace(cause));
        return String.format("%s%s", getSummary(), causeString);
    }
    
    private String getSummary() {
        if ( includeFullStackTrace() ) { return ExceptionUtils.getStackTrace(this); }
        var firstElt = getFirstStackTraceElement();
        var stackTraceString = firstElt==null ? "" : String.format("\n\tat "+firstElt);
        return String.format("%s: %s%s", getClass().getSimpleName(), getMessage(), stackTraceString);
    }

    private StackTraceElement getFirstStackTraceElement() {
        var elts = getStackTrace();
        return elts==null || elts.length==0 ? null : elts[0];
    }
}
