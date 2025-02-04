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
package com.fortify.cli.common.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.commons.io.output.NullOutputStream;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Helper class that allows for collecting or suppressing output while running a given {@link Callable}.
 *
 * @author Ruud Senden
 */
public class OutputHelper {
    public static final <T extends OutputStream> Result call(Callable<Integer> callable, OutputType stdoutType, OutputType stderrType, Charset charset) throws Exception {
        var oldStdout = System.out;
        var oldStderr = System.err;
        try ( var stdoutStream = stdoutType.streamSupplier.get();
              var stderrStream = stderrType.streamSupplier.get();
              var stdoutPS = new PrintStream(stdoutStream);
              var stderrPS = new PrintStream(stderrStream) ) {
            if (stdoutType!=OutputType.show) { System.setOut(stdoutPS); }
            if (stderrType!=OutputType.show) { System.setErr(stderrPS); }
            int exitCode = callable.call();
            System.out.flush();
            System.err.flush();
            return new Result(exitCode, stdoutType.stringFunction.apply(stdoutStream, charset), stderrType.stringFunction.apply(stderrStream, charset));
        } finally {
            System.setOut(oldStdout);
            System.setErr(oldStderr);
        }
    }
    
    @RequiredArgsConstructor
    public static enum OutputType {
        show(()->NullOutputStream.INSTANCE, (s,c)->""), 
        collect(ByteArrayOutputStream::new, (s,c)->((ByteArrayOutputStream)s).toString(c)), 
        suppress(()->NullOutputStream.INSTANCE, (s,c)->"");
        
        private final Supplier<OutputStream> streamSupplier;
        private final BiFunction<OutputStream,Charset,String> stringFunction;
    }
    
    @Data
    public static final class Result {
        private final int exitCode;
        private final String out;
        private final String err;
    }
}
