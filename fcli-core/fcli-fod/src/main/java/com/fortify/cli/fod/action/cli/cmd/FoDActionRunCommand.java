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
package com.fortify.cli.fod.action.cli.cmd;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.cli.cmd.AbstractActionRunCommand;
import com.fortify.cli.common.action.runner.ActionRunner;
import com.fortify.cli.common.action.runner.ActionRunner.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.action.runner.ActionRunner.ParameterTypeConverterArgs;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "run")
public class FoDActionRunCommand extends AbstractActionRunCommand {
    @Getter @Mixin private FoDUnirestInstanceSupplierMixin unirestInstanceSupplier;
    
    @Override
    protected final String getType() {
        return "FoD";
    }
    
    @Override
    protected String getSessionName() {
        return unirestInstanceSupplier.getSessionName();
    }
    
    @Override
    protected String getSessionFromEnvLoginCommand() {
        var fodUrl = EnvHelper.requiredEnv("FOD_URL");
        var fodTenant = EnvHelper.envOrDefault("FOD_TENANT", "");
        var fodUser = EnvHelper.envOrDefault("FOD_USER", "");
        var fodPwd = EnvHelper.envOrDefault("FOD_PASSWORD", "");
        var fodClientId = EnvHelper.envOrDefault("FOD_CLIENT_ID", "");
        var fodClientSecret = EnvHelper.envOrDefault("FOD_CLIENT_SECRET", "");
        var extraOpts = EnvHelper.envOrDefault("FOD_LOGIN_EXTRA_OPTS", "");
        String fodCredentialArgs;
        if ( StringUtils.isNotBlank(fodTenant) && StringUtils.isNotBlank(fodUser) && StringUtils.isNotBlank(fodPwd) ) {
            fodCredentialArgs = String.format("-t \"%s\" -u \"%s\" -p \"%s\"", fodTenant, fodUser, fodPwd);
        } else if ( StringUtils.isNotBlank(fodClientId) && StringUtils.isNotBlank(fodClientSecret) ) {
            fodCredentialArgs = String.format("--client-id \"%s\" --client-secret \\\"%s\\\"", fodClientId, fodClientSecret);
        } else {
            throw new IllegalStateException("Either FOD_TENANT, FOD_USER, and FOD_PASSWORD, or FOD_CLIENT_ID and FOD_CLIENT_SECRET environment variables must be defined");
        }
        return String.format(
                "fod session login --url \"%s\" %s -c \"%s\" %s",
                fodUrl, fodCredentialArgs, extraOpts);
    }
    
    @Override
    protected String getSessionFromEnvLogoutCommand() {
        return String.format("fod session logout");
    }
    
    @Override
    protected void configure(ActionRunner templateRunner, SimpleEvaluationContext context) {
        templateRunner
            .addParameterConverter("release_single", this::loadRelease)
            .addRequestHelper("fod", new FoDDataExtractRequestHelper(unirestInstanceSupplier::getUnirestInstance, FoDProductHelper.INSTANCE));
        context.setVariable("fod", new FoDSpelFunctions(templateRunner));
    }
    
    @RequiredArgsConstructor @Reflectable
    public final class FoDSpelFunctions {
        private final ActionRunner templateRunner;
        public String issueBrowserUrl(ObjectNode issue) {
            var deepLinkExpression = baseUrl()
                    +"/redirect/Issues/${vulnId}";
            return templateRunner.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), issue, String.class);
        }
        public String releaseBrowserUrl(ObjectNode appversion) {
            var deepLinkExpression = baseUrl()
                    +"/redirect/Releases/${releaseId}";
            return templateRunner.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), appversion, String.class);
        }
        public String appBrowserUrl(ObjectNode appversion) {
            var deepLinkExpression = baseUrl()
                    +"/redirect/Applications/${applicationId}";
            return templateRunner.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), appversion, String.class);
        }
        private String baseUrl() {
            return FoDProductHelper.INSTANCE.getBrowserUrl(unirestInstanceSupplier.getSessionDescriptor().getUrlConfig().getUrl());
        }
    }
    
    private final JsonNode loadRelease(String nameOrId, ParameterTypeConverterArgs args) {
        args.getProgressWriter().writeProgress("Loading release %s", nameOrId);
        var result = FoDReleaseHelper.getReleaseDescriptor(unirestInstanceSupplier.getUnirestInstance(), nameOrId, ":", true);
        args.getProgressWriter().writeProgress("Loaded release %s", result.getQualifiedName());
        return result.asJsonNode();
    }
    
    private static final class FoDDataExtractRequestHelper extends BasicActionRequestHelper {
        public FoDDataExtractRequestHelper(IUnirestInstanceSupplier unirestInstanceSupplier, IProductHelper productHelper) {
            super(unirestInstanceSupplier, productHelper);
        }
    }
}
