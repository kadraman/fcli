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
package com.fortify.cli.common.cli.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.cmd.IOutputHelperSupplier;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputWriter;
import com.fortify.cli.common.util.JavaHelper;
import com.fortify.cli.common.util.OutputHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.OutputHelper.Result;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

@Builder @Data
public final class FcliCommandExecutor {
    private final CommandLine rootCommandLine;
    private final String cmd;
    private final Consumer<ObjectNode> recordConsumer;
    @Builder.Default private final OutputType stdoutOutputType = OutputType.show;
    @Builder.Default private final OutputType stderrOutputType = OutputType.show;
    private final Consumer<Throwable> onException;
    private final Consumer<Result> onNonZeroExitCode;
    @Getter(lazy = true, value = AccessLevel.PRIVATE) private final ParseResult parseResult = createParseResult(); 
    
    public final Result execute() {
        if ( rootCommandLine==null ) {
            throw new IllegalStateException("Root command line instance must be provided to run fcli commands");
        }
        if ( StringUtils.isBlank(cmd) ) {
            throw new IllegalStateException("Fcli command to be run may not be blank");
        }
        if ( recordConsumer!=null && canCollectRecords() ) {
            StandardOutputWriter.collectRecords(recordConsumer, stdoutOutputType!=OutputType.show);
        }
        try {
            var result = OutputHelper.call(()->_execute(), stdoutOutputType, stderrOutputType, StandardCharsets.UTF_8);
            if ( result.getExitCode()!=0 ) {
                consume(result, onNonZeroExitCode, this::throwExceptionOnNonZeroExitCode);
            }
            return result;
        } catch ( Throwable t ) {
            if ( t instanceof ExecutionException ) {
                t = t.getCause();
            }
            consume(t, onException, this::rethrowAsRuntimeException);
            return new Result(999, "", "");
        }
    }

    private final int _execute() throws Exception {
        rootCommandLine.clearExecutionResults();
        return rootCommandLine.getExecutionStrategy().execute(getParseResult());
    }
    
    public final boolean canCollectRecords() {
        return getLeafCommand(IOutputHelperSupplier.class).isPresent();
    }
    
    private final CommandSpec getLeafCommandSpec() {
        var leafCommand = getParseResult().subcommand();
        while (leafCommand.hasSubcommand() ) {
            leafCommand = leafCommand.subcommand();
        }
        return leafCommand.commandSpec();
    }
    
    private final <T> Optional<T> getLeafCommand(Class<T> type) {
        return JavaHelper.as(getLeafCommandSpec().userObject(), type);
    }
    
    private final ParseResult createParseResult() {
        List<String> argsList = new ArrayList<String>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(cmd);
        while (m.find()) { argsList.add(m.group(1).replace("\"", "")); }
        return rootCommandLine.parseArgs(argsList.toArray(String[]::new));
    }
    
    private <T> void consume(T value, Consumer<T> consumer, Consumer<T> defaultConsumer) {
        if ( consumer==null ) {
            defaultConsumer.accept(value);
        } else {
            consumer.accept(value);
        }
    }
    
    private void rethrowAsRuntimeException(Throwable t) {
        throw new IllegalStateException("Fcli command threw an exception", t);
    }
    
    private final void throwExceptionOnNonZeroExitCode(Result r) {
        throw new IllegalStateException("Fcli command terminated with non-zero exit code "+r.getExitCode());
    }
}
