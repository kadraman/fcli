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
package com.fortify.cli.aviator._common.session.admin.cli.mixin;

import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionHelper;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionDescriptorSupplierMixin;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;

public class AviatorAdminSessionDescriptorSupplier extends AbstractSessionDescriptorSupplierMixin<AviatorAdminSessionDescriptor> {
    @Getter @ArgGroup(headingKey = "aviator.admin-session.name.arggroup") 
    private AviatorAdminSessionNameArgGroup sessionNameSupplier;
    
    @Override
    public final AviatorAdminSessionDescriptor getSessionDescriptor(String sessionName) {
        return AviatorAdminSessionHelper.instance().get(sessionName, true);
    }
}
