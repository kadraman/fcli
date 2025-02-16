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

import java.util.Map;

import com.fortify.cli.common.action.model.ActionConfig.ActionConfigSessionFromEnvOutput;
import com.fortify.cli.common.action.runner.ActionRunnerConfig;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;

public abstract class AbstractActionRunWithSessionCommand extends AbstractActionRunCommand<Map<String,String>> {
    private boolean currentActionRunInitializedSessionFromEnv = false;
    
    @Override
    protected void preRun(ActionRunnerConfig config) {
        initializeSession(config);
    }
    
    @Override
    protected void postRun(ActionRunnerConfig config) {
        terminateSession(config);
    }

    private final void initializeSession(ActionRunnerConfig config) {
        // Initialize session if session name equals 'from-env' and session wasn't initialized yet
        // during current fcli invocation.
        if ( "from-env".equals(getSessionName()) && !hasInitializedSessionFromEnv() ) {
            runSessionCmd(isShowStdout(config), getSessionFromEnvLoginCommand(), SessionCmdType.LOGIN);
            currentActionRunInitializedSessionFromEnv = true;
            System.setProperty("fcli.action.initializedSessionFromEnv", "true");
        }
    }

    private final void terminateSession(ActionRunnerConfig config) {
        // Terminate session only if it was initialized from the current 'action run' invocation. This allows
        // for fcli actions to invoke other 'fcli * action run' commands, initializing and cleaning up the 
        // session only on the first 'fcli * action run' invocation.
        if ( currentActionRunInitializedSessionFromEnv ) {
            try {
                runSessionCmd(isShowStdout(config), getSessionFromEnvLogoutCommand(), SessionCmdType.LOGOUT);
            } finally {
                currentActionRunInitializedSessionFromEnv = false;
                System.setProperty("fcli.action.initializedSessionFromEnv", "false");
            }
        }
    }

    private void runSessionCmd(boolean showStdout, String baseCmd, SessionCmdType type) {
        var extraOptsEnvName = String.format("%s_%s_EXTRA_OPTS", getType().toUpperCase().replace('-', '_'), type.name());
        var extraOpts = EnvHelper.envOrDefault(extraOptsEnvName, "");
        FcliCommandExecutorFactory.builder()
            .cmd(String.format("%s --session from-env %s", baseCmd, extraOpts))
            .stdoutOutputType(showStdout ? OutputType.show : OutputType.suppress)
            .build()
            .create()
            .execute();
    }
    
    private boolean isShowStdout(ActionRunnerConfig config) {
        return config.getAction().getConfig().getSessionFromEnvOutput()==ActionConfigSessionFromEnvOutput.show;
    }
    
    private boolean hasInitializedSessionFromEnv() {
        return "true".equals(System.getProperty("fcli.action.initializedSessionFromEnv"));
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
    
    private static enum SessionCmdType {
        LOGIN, LOGOUT
    }
}
