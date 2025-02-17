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

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStep;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepsProcessor extends AbstractActionStepProcessorListEntries<ActionStep> {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final ArrayList<ActionStep> list;

    protected final void process(ActionStep step) {
        step.getActionStepField().createActionStepProcessor(ctx, vars).process();
    }
}
