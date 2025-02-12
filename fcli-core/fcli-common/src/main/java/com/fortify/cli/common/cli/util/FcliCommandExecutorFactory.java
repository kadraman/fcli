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
import com.fortify.cli.common.variable.FcliVariableHelper;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

@Builder @Data
public final class FcliCommandExecutorFactory {
    @Setter private static CommandLine rootCommandLine;
    @NonNull private final String cmd;
    private final Consumer<ObjectNode> recordConsumer;
    @Builder.Default private final OutputType stdoutOutputType = OutputType.show;
    @Builder.Default private final OutputType stderrOutputType = OutputType.show;
    private final Consumer<Throwable> onException;
    private final Consumer<Result> onNonZeroExitCode;
    
    public final FcliCommandExecutor create() {
        if ( rootCommandLine==null ) {
            throw new RuntimeException("Root command line hasn't been configured upon fcli initialization");
        }
        if ( StringUtils.isBlank(cmd) ) {
            throw new IllegalStateException("Fcli command to be run may not be blank");
        }
        return new FcliCommandExecutor();
    }
    
    public final class FcliCommandExecutor {
        private final String[] resolvedArgs;
        private final CommandSpec replicatedLeafCommandSpec;
        
        public FcliCommandExecutor() {
            this.resolvedArgs = FcliVariableHelper.resolveVariables(parseArgs(cmd));
            this.replicatedLeafCommandSpec = replicateLeafCommandSpecWithParents(rootCommandLine.parseArgs(this.resolvedArgs));
        }

        public final Result execute() {
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
            return new CommandLine(replicatedLeafCommandSpec.root()).execute(resolvedArgs);
        }

        public final boolean canCollectRecords() {
            return getLeafCommand(IOutputHelperSupplier.class).isPresent();
        }
        
        private final <T> Optional<T> getLeafCommand(Class<T> type) {
            return JavaHelper.as(replicatedLeafCommandSpec.userObject(), type);
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
        
        // We want to replicate the CommandSpec with new command instances, at least for the 
        // leaf command, to make sure that each invocation uses a separate instance of the 
        // leaf command. Otherwise, instance variables might have the wrong values, somewhat
        // similar to similar to https://vulncat.fortify.com/en/detail?category=Race%20Condition&subcategory=Singleton%20Member%20Field#Java%2fJS
        private static final CommandSpec replicateLeafCommandSpecWithParents(ParseResult parseResult) {
            // This is the safest approach, but causes picocli to recreate the
            // full command tree through reflection, which is far from optimal
            // as we already know which command to execute.
            //return CommandSpec.forAnnotatedObject(parseResult.commandSpec().userObject().getClass());
            
            // More optimized approach, just walking the requested command tree 
            CommandSpec replicatedSpec = null;
            for ( var pr = parseResult ; pr!=null ; pr = pr.subcommand() ) {
                var newSpec = replicateSpecForSubcommand(pr);
                if ( replicatedSpec!=null ) {
                    replicatedSpec.addSubcommand(newSpec.name(), newSpec);
                }
                replicatedSpec = newSpec;
            }
            return replicatedSpec;
        }

        private static CommandSpec replicateSpecForSubcommand(ParseResult pr) {
            var orgSpec = pr.commandSpec();
            if ( !pr.hasSubcommand() ) {
                // Create new spec from leaf command class
                return CommandSpec.forAnnotatedObject(orgSpec.userObject().getClass());
            } else {
                // Create shallow copy of container command spec
                var newSpec = CommandSpec.wrapWithoutInspection(orgSpec.userObject());
                newSpec.name(orgSpec.name());
                newSpec.resourceBundle(orgSpec.resourceBundle());
                return newSpec;
            }
        }
        
        private static final String[] parseArgs(String args) {
            List<String> argsList = new ArrayList<String>();
            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(args);
            while (m.find()) { argsList.add(m.group(1).replace("\"", "")); }
            return argsList.toArray(String[]::new);
        }
    }
}
