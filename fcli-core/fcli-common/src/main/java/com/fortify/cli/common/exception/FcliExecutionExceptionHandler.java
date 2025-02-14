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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FcliExecutionExceptionHandler implements IExecutionExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FcliExecutionExceptionHandler.class);
    public static final FcliExecutionExceptionHandler INSTANCE = new FcliExecutionExceptionHandler();
    
    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) throws Exception {
        if ( LOG.isDebugEnabled() ) {
            LOG.debug("fcli terminating with exception", ex);
        }
        var formattedException = formatException(ex);
        if ( formattedException!=null ) {
            var err = commandLine.getErr();
            err.println(commandLine.getColorScheme().errorText(formattedException));
            err.flush();
        }
        return commandLine.getCommandSpec().exitCodeOnExecutionException();
    }
    
    private static String formatException(Exception e) {
        return (e instanceof FcliException) ? formatFcliException((FcliException)e) : formatNonFcliException(e);
    }

    private static final String formatFcliException(FcliException e) {
        return e.getSummaryWithCause();
    }

    private static final String formatNonFcliException(Exception e) {
        return ExceptionUtils.getStackTrace(e);
    }
}
