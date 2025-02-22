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
package com.fortify.cli.aviator._common.session.cli.cmd;

import com.fortify.cli.aviator._common.session.cli.mixin.AviatorSessionLoginOptions;
import com.fortify.cli.aviator._common.session.cli.mixin.AviatorSessionNameArgGroup;
import com.fortify.cli.aviator._common.session.helper.AviatorSessionDescriptor;
import com.fortify.cli.aviator._common.session.helper.AviatorSessionHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class AviatorSessionLoginCommand extends AbstractSessionLoginCommand<AviatorSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.Login outputHelper;
    @Getter private AviatorSessionHelper sessionHelper = AviatorSessionHelper.instance();
    @Mixin private AviatorSessionLoginOptions sessionLoginOptions = new AviatorSessionLoginOptions();
    @Getter @ArgGroup(headingKey = "aviator.session.name.arggroup") 
    private AviatorSessionNameArgGroup sessionNameSupplier;
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, AviatorSessionDescriptor sessionDescriptor) {
        // Nothing to do
    }
    
    @Override
    protected AviatorSessionDescriptor login(String sessionName) {
        return new AviatorSessionDescriptor(sessionLoginOptions.getAviatorUrl(), sessionLoginOptions.getAviatorToken());
    }
}
