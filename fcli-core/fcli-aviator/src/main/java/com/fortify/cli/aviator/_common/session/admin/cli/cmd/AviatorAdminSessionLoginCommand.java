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
package com.fortify.cli.aviator._common.session.admin.cli.cmd;

import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionLoginOptions;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionNameArgGroup;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class AviatorAdminSessionLoginCommand extends AbstractSessionLoginCommand<AviatorAdminSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Login outputHelper;
    @Getter private AviatorAdminSessionHelper sessionHelper = AviatorAdminSessionHelper.instance();
    @Mixin private AviatorAdminSessionLoginOptions sessionLoginOptions = new AviatorAdminSessionLoginOptions();
    @Getter @ArgGroup(headingKey = "aviator.admin-session.name.arggroup") 
    private AviatorAdminSessionNameArgGroup sessionNameSupplier;
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, AviatorAdminSessionDescriptor sessionDescriptor) {
        // Nothing to do
    }
    
    @Override
    protected AviatorAdminSessionDescriptor login(String sessionName) {
        return new AviatorAdminSessionDescriptor(sessionLoginOptions.getAviatorUrl(), sessionLoginOptions.getTenant(), sessionLoginOptions.getPrivateKeyFile());
    }
}
