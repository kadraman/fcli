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

import com.fortify.cli.common.action.model.ActionRequestTarget;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerData;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ActionAddRequestTargetsProcessor {
    private final ActionRunnerContext ctx;
    private final ActionRunnerData data;
    public final void addRequestTargets() {
        var requestTargets = ctx.getConfig().getAction().getAddRequestTargets();
        if ( requestTargets!=null ) {
            requestTargets.forEach(this::addRequestTarget);
        }
    }
    private void addRequestTarget(ActionRequestTarget descriptor) {
        ctx.getConfig().getRequestHelpers().put(descriptor.getName(), createBasicRequestHelper(descriptor));
    }
    
    private IActionRequestHelper createBasicRequestHelper(ActionRequestTarget descriptor) {
        var name = descriptor.getName();
        var baseUrl = data.eval(descriptor.getBaseUrl(), String.class);
        var headers = data.eval(descriptor.getHeaders(), String.class);
        IUnirestInstanceSupplier unirestInstanceSupplier = () -> GenericUnirestFactory.getUnirestInstance(name, u->{
            u.config().defaultBaseUrl(baseUrl).getDefaultHeaders().add(headers);
            UnirestUnexpectedHttpResponseConfigurer.configure(u);
            UnirestJsonHeaderConfigurer.configure(u);
        });
        return new BasicActionRequestHelper(unirestInstanceSupplier, null);
    }
}