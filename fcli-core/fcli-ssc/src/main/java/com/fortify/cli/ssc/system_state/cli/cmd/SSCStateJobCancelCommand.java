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
package com.fortify.cli.ssc.system_state.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.system_state.cli.mixin.SSCJobResolverMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "cancel-job") @CommandGroup("job")
public class SSCStateJobCancelCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper; 
    @Mixin private SSCJobResolverMixin.PositionalParameter jobResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        String jobName = jobResolver.getJobName(unirest);
        ObjectNode cancelRequestData = new ObjectMapper().createObjectNode()
                .set("jobIds", JsonHelper.toArrayNode(new String[] {jobName}));
        unirest.post(SSCUrls.JOBS_ACTION_CANCEL).body(cancelRequestData).asObject(JsonNode.class).getBody();
        return jobResolver.getJobDescriptor(unirest).asJsonNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return "CANCEL_REQUESTED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
