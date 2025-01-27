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
package com.fortify.cli.ssc.artifact.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;

import kong.unirest.HttpRequest;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SSCArtifactListCommand extends AbstractSSCArtifactOutputCommand implements IBaseRequestSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    
    @Override
    public HttpRequest<?> getBaseRequest() {
        var unirest = getUnirestInstance();
        return unirest.get(SSCUrls.PROJECT_VERSION_ARTIFACTS(parentResolver.getAppVersionId(unirest)))
                .queryString("embed","scans");
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
