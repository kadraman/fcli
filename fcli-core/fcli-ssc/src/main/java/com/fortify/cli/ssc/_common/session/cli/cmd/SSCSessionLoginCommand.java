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
package com.fortify.cli.ssc._common.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCAndScanCentralSessionLoginOptions;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCSessionNameArgGroup;
import com.fortify.cli.ssc._common.session.helper.ISSCAndScanCentralCredentialsConfig;
import com.fortify.cli.ssc._common.session.helper.ISSCAndScanCentralUrlConfig;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionDescriptor;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionHelper;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class SSCSessionLoginCommand extends AbstractSessionLoginCommand<SSCAndScanCentralSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Login outputHelper;
    @Getter private SSCAndScanCentralSessionHelper sessionHelper = SSCAndScanCentralSessionHelper.instance();
    @Mixin private SSCAndScanCentralSessionLoginOptions sessionLoginOptions;
    @Getter @ArgGroup(headingKey = "ssc.session.name.arggroup") 
    private SSCSessionNameArgGroup sessionNameSupplier;
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, SSCAndScanCentralSessionDescriptor sessionDescriptor) {
        sessionDescriptor.logout(sessionLoginOptions.getSscAndScanCentralCredentialConfigOptions().getSscUserCredentialsConfig());
    }
    
    @Override
    protected SSCAndScanCentralSessionDescriptor login(String sessionName) {
        ISSCAndScanCentralUrlConfig urlConfig = sessionLoginOptions.getSscAndScanCentralUrlConfigOptions();
        ISSCAndScanCentralCredentialsConfig credentialsConfig = sessionLoginOptions.getSscAndScanCentralCredentialConfigOptions();
        return SSCAndScanCentralSessionDescriptor.create(urlConfig, credentialsConfig);
    }
}
