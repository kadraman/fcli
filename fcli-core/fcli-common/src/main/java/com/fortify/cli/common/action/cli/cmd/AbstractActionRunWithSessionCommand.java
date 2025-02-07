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
import java.util.Map;

import com.fortify.cli.common.action.model.ActionConfig.ActionConfigSessionFromEnvOutput;
import com.fortify.cli.common.action.runner.ActionRunnerConfig;
import com.fortify.cli.common.cli.util.FcliCommandExecutor;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;

import picocli.CommandLine;

public abstract class AbstractActionRunWithSessionCommand extends AbstractActionRunCommand<Map<String,String>> {
    private boolean currentActionRunInitializedSessionFromEnv = false;
    
    @Override
    protected Map<String, String> preRun(ActionRunnerConfig config) {
        initializeSession(config);
        return initializeSessionProperties();
    }
    
    @Override
    protected void postRun(ActionRunnerConfig config, Map<String, String> preRunOutput) {
        resetSessionProperties(preRunOutput);
        terminateSession(config);
    }

    private Map<String, String> initializeSessionProperties() {
        Map<String, String> orgProperties = new HashMap<>();
        var sessionName = getSessionName();
        for ( var module : getSharedSessionModules() ) {
            var sessionEnvName = String.format("%s_%s_SESSION", System.getProperty("fcli.env.default.prefix", "FCLI_DEFAULT"), module.toUpperCase());
            var sessionPropertyName = EnvHelper.envSystemPropertyName(sessionEnvName);
            orgProperties.put(sessionPropertyName, EnvHelper.env(sessionEnvName));
            setOrClearSystemProperty(sessionPropertyName, sessionName);
        }
        return orgProperties;
    }
    
    private void resetSessionProperties(Map<String, String> preRunOutput) {
        if ( preRunOutput!=null ) {
            for ( var entry : preRunOutput.entrySet() ) {
                setOrClearSystemProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    private final void initializeSession(ActionRunnerConfig config) {
        // Initialize session if session name equals 'from-env' and session wasn't initialized yet
        // during current fcli invocation.
        if ( "from-env".equals(getSessionName()) && !hasInitializedSessionFromEnv() ) {
            var showStdout = config.getAction().getConfig().getSessionFromEnvOutput()==ActionConfigSessionFromEnvOutput.show;
            var baseCmd = getSessionFromEnvLoginCommand();
            runSessionCmd(config.getRootCommandLine(), showStdout, baseCmd);
            currentActionRunInitializedSessionFromEnv = true;
            System.setProperty("fcli.action.initializedSessionFromEnv", "true");
        }
    }
    
    private boolean hasInitializedSessionFromEnv() {
        return "true".equals(System.getProperty("fcli.action.initializedSessionFromEnv"));
    }

    private final void terminateSession(ActionRunnerConfig config) {
        // Terminate session only if it was initialized from the current 'action run' invocation. This allows
        // for fcli actions to invoke other 'fcli * action run' commands, initializing and cleaning up the 
        // session only on the first 'fcli * action run' invocation.
        if ( currentActionRunInitializedSessionFromEnv ) {
            try {
                var showStdout = config.getAction().getConfig().getSessionFromEnvOutput()==ActionConfigSessionFromEnvOutput.show;
                var baseCmd = getSessionFromEnvLogoutCommand();
                runSessionCmd(config.getRootCommandLine(), showStdout, baseCmd);
            } finally {
                currentActionRunInitializedSessionFromEnv = false;
                System.setProperty("fcli.action.initializedSessionFromEnv", "false");
            }
        }
    }

    private void runSessionCmd(CommandLine rootCommandLine, boolean showStdout, String baseCmd) {
        FcliCommandExecutor.builder()
            .rootCommandLine(rootCommandLine)
            .cmd(String.format("%s --session from-env", baseCmd))
            .stdoutOutputType(showStdout ? OutputType.show : OutputType.suppress)
            .build()
            .execute();
    }

    private void setOrClearSystemProperty(String name, String value) {
        if ( value==null ) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
    
    /** By default, this method returns the single module name as defined through {@link #getType()}. If sessions
     *  are shared between multiple modules (i.e., SSC/SC-SAST/SC-DAST), this method should be overridden to
     *  return all module names that share the same session. */ 
    protected String[] getSharedSessionModules() {
        return new String[] {getType()};
    }
    protected abstract String getSessionName();
    protected abstract String getSessionFromEnvLoginCommand();
    protected abstract String getSessionFromEnvLogoutCommand(); 
}
