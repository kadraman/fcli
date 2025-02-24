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
package com.fortify.cli.aviator._common.session.user.cli.cmd;

import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionNameArgGroup;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLogoutCommand;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Logout.CMD_NAME, sortOptions = false)
public class AviatorUserSessionLogoutCommand extends AbstractSessionLogoutCommand<AviatorUserSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Logout outputHelper;
    @Getter private AviatorUserSessionHelper sessionHelper = AviatorUserSessionHelper.instance();
    @Getter @ArgGroup(headingKey = "aviator.user-session.name.arggroup") 
    private AviatorUserSessionNameArgGroup sessionNameSupplier;
    
    @Override
    protected void logout(String sessionName, AviatorUserSessionDescriptor sessionDescriptor) {
       // Nothing to do
    }
}
