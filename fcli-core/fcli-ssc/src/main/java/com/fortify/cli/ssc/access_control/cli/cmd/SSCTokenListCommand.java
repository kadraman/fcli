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
package com.fortify.cli.ssc.access_control.cli.cmd;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;
import com.fortify.cli.ssc.access_control.helper.SSCTokenHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "list-tokens", aliases = "lst") @CommandGroup("token")
public class SSCTokenListCommand extends AbstractSSCTokenCommand implements IRecordTransformer {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;
    // TODO This won't work as we don't extend from AbstractSSCOutputCommand
    /*
    private final SSCQParamGenerator qParamGenerator = 
            new SSCQParamGenerator()
            .add("userName", SSCQParamValueGenerators::wrapInQuotes)
            .add("type", SSCQParamValueGenerators::wrapInQuotes);
    */
    @Override
    protected JsonNode getJsonNode(IUrlConfig urlConfig, IUserCredentialsConfig userCredentialsConfig) {
        return SSCTokenHelper.listTokens(urlConfig, userCredentialsConfig, getQueryParams());
    }
    
    @Override
    public JsonNode transformRecord(JsonNode input) {
    	return SSCTokenHelper.transformTokenRecord(input);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
    
    private Map<String, Object> getQueryParams() {
        Map<String, Object> queryParams = new HashMap<String, Object>();
        String qParamValue = null; /*qParamGenerator.getQParamValue(outputHelper.getOutputWriterFactory().getOutputQueries());*/
        if ( StringUtils.isNotBlank(qParamValue) ) {
            queryParams.put("q", qParamValue);
        }
        return queryParams;
    }
}
