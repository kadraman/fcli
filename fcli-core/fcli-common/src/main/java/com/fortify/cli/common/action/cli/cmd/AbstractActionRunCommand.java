/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.action.cli.cmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fortify.cli.common.action.cli.mixin.ActionResolverMixin;
import com.fortify.cli.common.action.cli.mixin.ActionValidationMixin;
import com.fortify.cli.common.action.runner.ActionParameterHelper;
import com.fortify.cli.common.action.runner.ActionRunner;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.cli.util.FcliCommandExecutor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionsParseResult;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.progress.helper.ProgressWriterType;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.common.util.EnvHelper;

import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Unmatched;

public abstract class AbstractActionRunCommand extends AbstractRunnableCommand {
    @Mixin private ActionResolverMixin.RequiredParameter actionResolver;
    @DisableTest({TestType.MULTI_OPT_SPLIT, TestType.MULTI_OPT_PLURAL_NAME, TestType.OPT_LONG_NAME, TestType.OPT_LONG_NAME_COUNT, TestType.OPT_NAME_FORMAT, TestType.OPT_ARITY_PRESENT})
    @Option(names="action-parameters", arity="0", descriptionKey="fcli.action.run.action-parameter") 
    private List<String> dummyForSynopsis;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    @Mixin private CommandHelperMixin commandHelper;
    @Mixin private ActionValidationMixin actionValidationMixin;
    @Unmatched private String[] actionArgs;
    private boolean currentActionRunInitializedSessionFromEnv = false;
    
    @Override @SneakyThrows
    public final Integer call() {
        initMixins();
        var action = actionResolver.loadAction(getType(), actionValidationMixin.getActionValidationHandler());
        Callable<Integer> delayedConsoleWriter = null;
        try ( var progressWriter = progressWriterFactory.overrideAutoIfNoConsole(ProgressWriterType.none) ) {
            try ( var actionRunner = ActionRunner.builder()
                .onValidationErrors(this::onValidationErrors)
                .action(action)
                .progressWriter(progressWriter)
                .rootCommandLine(getRootCommandLine())
                .build() ) 
            {
                delayedConsoleWriter = run(actionRunner, progressWriter);
            }
        }
        return delayedConsoleWriter.call();
    }

    private final CommandLine getRootCommandLine() {
        return commandHelper.getRootCommandLine();
    }
    
    private final Callable<Integer> run(ActionRunner actionRunner, IProgressWriterI18n progressWriter) {
        try {
            initializeSession();
            actionRunner.getSpelEvaluator().configure(context->configure(actionRunner, context));
            progressWriter.writeProgress("Executing action %s", actionRunner.getAction().getMetadata().getName());
            // We need to set the FCLI_DEFAULT_<module>_SESSION environment variable to allow fcli: statements to 
            // pick up the current session name, and (although probably not needed currently), reset the default
            // session name to the previous value once the action completes.
            Map<String, String> orgProperties = new HashMap<>();
            try {
                var sessionName = getSessionName();
                for ( var module : getSharedSessionModules() ) {
                    var sessionEnvName = String.format("%s_%s_SESSION", System.getProperty("fcli.env.default.prefix", "FCLI_DEFAULT"), module.toUpperCase());
                    var sessionPropertyName = EnvHelper.envSystemPropertyName(sessionEnvName);
                    orgProperties.put(sessionPropertyName, EnvHelper.env(sessionEnvName));
                    setOrClearSystemProperty(sessionPropertyName, sessionName);
                }
                return actionRunner.run(actionArgs);
            } finally {
                for ( var entry : orgProperties.entrySet() ) {
                    setOrClearSystemProperty(entry.getKey(), entry.getValue());
                }
            }
        } finally {
            terminateSession();
        }
    }

    private final void initializeSession() {
        // Initialize session if session name equals 'from-env' and session wasn't initialized yet
        // during current fcli invocation.
        if ( "from-env".equals(getSessionName()) && !hasInitializedSessionFromEnv() ) {
            var baseCmd = getSessionFromEnvLoginCommand();
            new FcliCommandExecutor(getRootCommandLine(), String.format("%s --session from-env", baseCmd)).execute();
            currentActionRunInitializedSessionFromEnv = true;
            System.setProperty("fcli.action.initializedSessionFromEnv", "true");
        }
    }
    
    private boolean hasInitializedSessionFromEnv() {
        return "true".equals(System.getProperty("fcli.action.initializedSessionFromEnv"));
    }

    private final void terminateSession() {
        // Terminate session only if it was initialized from the current 'action run' invocation. This allows
        // for fcli actions to invoke other 'fcli * action run' commands, initializing and cleaning up the 
        // session only on the first 'fcli * action run' invocation.
        if ( currentActionRunInitializedSessionFromEnv ) {
            try {
                var baseCmd = getSessionFromEnvLogoutCommand();
                new FcliCommandExecutor(getRootCommandLine(), String.format("%s --session from-env", baseCmd)).execute();
            } finally {
                currentActionRunInitializedSessionFromEnv = false;
                System.setProperty("fcli.action.initializedSessionFromEnv", "false");
            }
        }
    }

    private void setOrClearSystemProperty(String name, String value) {
        if ( value==null ) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
    
    private ParameterException onValidationErrors(OptionsParseResult optionsParseResult) {
        var errorsString = String.join("\n ", optionsParseResult.getValidationErrors());
        var supportedOptionsString = ActionParameterHelper.getSupportedOptionsTable(optionsParseResult.getOptions());
        var msg = String.format("Option errors:\n %s\nSupported options:\n%s\n", errorsString, supportedOptionsString);
        return new ParameterException(commandHelper.getCommandSpec().commandLine(), msg);
    }

    /** By default, this method returns the single module name as defined through {@link #getType()}. If sessions
     *  are shared between multiple modules (i.e., SSC/SC-SAST/SC-DAST), this method should be overridden to
     *  return all module names that share the same session. */ 
    protected String[] getSharedSessionModules() {
        return new String[] {getType()};
    }
    protected abstract String getType();
    protected abstract String getSessionName();
    protected abstract String getSessionFromEnvLoginCommand();
    protected abstract String getSessionFromEnvLogoutCommand();
    protected abstract void configure(ActionRunner actionRunner, SimpleEvaluationContext context);
}
