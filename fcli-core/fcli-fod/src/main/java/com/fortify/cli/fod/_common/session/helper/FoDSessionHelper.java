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
package com.fortify.cli.fod._common.session.helper;

import com.fortify.cli.common.session.helper.AbstractSessionHelper;

public class FoDSessionHelper extends AbstractSessionHelper<FoDSessionDescriptor> {
    private static final FoDSessionHelper INSTANCE = new FoDSessionHelper();
    
    private FoDSessionHelper() {}
    
    @Override
    public String getType() {
        return "FoD";
    }
    
    @Override
    protected String getLoginCmd() {
        return "fcli fod session login";
    }

    @Override
    protected Class<FoDSessionDescriptor> getSessionDescriptorType() {
        return FoDSessionDescriptor.class;
    }
    
    public static final FoDSessionHelper instance() {
        return INSTANCE;
    }
}
