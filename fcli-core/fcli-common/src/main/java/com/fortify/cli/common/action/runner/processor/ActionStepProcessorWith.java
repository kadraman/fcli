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
package com.fortify.cli.common.action.runner.processor;

import java.util.ArrayList;
import java.util.List;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepWith;
import com.fortify.cli.common.action.model.ActionStepWithCleanup;
import com.fortify.cli.common.action.model.ActionStepWithSession;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorWith extends AbstractActionStepProcessor {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final ActionStepWith withStep;

    @Override
    public void process() {
        // TODO Add support for installing shutdown handler to even run cleanup steps
        //      on System.exit(), Ctrl-C, ...
        var shutdownHandlers = new ArrayList<Runnable>();
        addWithStepShutdownHandlers(shutdownHandlers);
        var shutdownThread = new Thread(()->shutdownHandlers.forEach(Runnable::run));
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        try {
            processWithStepInitialization(shutdownHandlers);
            processSteps(withStep.get_do());
        } finally {
            processWithStepCleanup();
            Runtime.getRuntime().removeShutdownHook(shutdownThread);
        }
    }
    
    private void addWithStepShutdownHandlers(ArrayList<Runnable> shutdownHandlers) {
        addWithCleanupStepShutdownHandlers(shutdownHandlers, withStep.getCleanup());
        addWithSessionStepShutdownHandlers(shutdownHandlers, withStep.getSessions());
    }

    private void addWithCleanupStepShutdownHandlers(ArrayList<Runnable> shutdownHandlers, ActionStepWithCleanup cleanup) {
        if ( cleanup!=null ) {
            // TODO Make this optional through property on ActionStepWithCleanup
            shutdownHandlers.add(()->processWithCleanupStepCleanup(cleanup));
        }
    }

    private void addWithSessionStepShutdownHandlers(ArrayList<Runnable> shutdownHandlers, List<ActionStepWithSession> sessions) {
        if ( sessions!=null ) {
            sessions.forEach(s->shutdownHandlers.add(()->processWithSessionStepCleanup(s)));
        }
    }

    private void processWithStepInitialization(ArrayList<Runnable> shutdownHandlers) {
        processWithCleanupStepInitialization(shutdownHandlers, withStep.getCleanup());
        processWithSessionStepInitialization(shutdownHandlers, withStep.getSessions());
    }

    private void processWithCleanupStepInitialization(ArrayList<Runnable> shutdownHandlers, ActionStepWithCleanup cleanup) {
        if ( cleanup!=null ) {
            processSteps(cleanup.getInitSteps());
        }
    }

    private void processWithSessionStepInitialization(ArrayList<Runnable> shutdownHandlers, List<ActionStepWithSession> sessions) {
        if ( sessions!=null ) {
            sessions.forEach(s->processFcliSessionCmd(s.getLoginCommand()));
        }
    }

    private void processWithStepCleanup() {
        processWithCleanupStepCleanup(withStep.getCleanup());
        processWithSessionStepCleanup(withStep.getSessions());
    }

    private void processWithCleanupStepCleanup(ActionStepWithCleanup cleanup) {
        if ( cleanup!=null ) {
            processSteps(cleanup.getCleanupSteps());
        }
    }

    private void processWithSessionStepCleanup(List<ActionStepWithSession> sessions) {
        if ( sessions != null ) {
            sessions.forEach(this::processWithSessionStepCleanup);
        }
    }
    
    private void processWithSessionStepCleanup(ActionStepWithSession session) {
        processFcliSessionCmd(session.getLogoutCommand());
    }

    private void processFcliSessionCmd(TemplateExpression cmd) {
        FcliCommandExecutorFactory.builder()
            .cmd(vars.eval(cmd, String.class))
        .build().create().execute();
    }
}
