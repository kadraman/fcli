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
package com.fortify.cli.config.truststore.cli.cmd;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigDescriptor;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.config.truststore.helper.TrustStoreOutputHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name=OutputHelperMixins.Set.CMD_NAME)
public class TrustStoreSetCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier, IRecordTransformer {
    @Mixin @Getter private OutputHelperMixins.Set outputHelper;
    
    @Mixin CommonOptionMixins.RequiredFile fileMixin;
    
    @Option(names = {"-p", "--password"})
    private String trustStorePassword;
    
    @Option(names = {"-t", "--type"}, defaultValue = "jks")
    private String trustStoreType;
    
    @Override
    public JsonNode getJsonNode() {
    	Path absolutePath = fileMixin.getFile().toPath().toAbsolutePath();
    	if ( !Files.exists(absolutePath) ) {
    		throw new FcliSimpleException("Trust store cannot be found: "+absolutePath);
    	}
		String absolutePathString = absolutePath.toString();
    	TrustStoreConfigDescriptor descriptor = TrustStoreConfigDescriptor.builder()
    		.path(absolutePathString)
    		.type(trustStoreType)
    		.password(trustStorePassword)
    		.build();
        TrustStoreConfigHelper.setTrustStoreConfig(descriptor);
        return descriptor.asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public String getActionCommandResult() {
        return "CONFIGURED";
    }
    
    @Override
    public JsonNode transformRecord(JsonNode input) {
        return TrustStoreOutputHelper.transformRecord(input);
    }
}
