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
package com.fortify.cli.ssc._common.rest.ssc.cli.mixin;

import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.ssc._common.rest.cli.mixin.SSCAndScanCentralUnirestInstanceSupplierMixin;

import kong.unirest.UnirestInstance;

public final class SSCUnirestInstanceSupplierMixin extends SSCAndScanCentralUnirestInstanceSupplierMixin implements IUnirestInstanceSupplier {
    @Override
    public UnirestInstance getUnirestInstance() {
        return getSscUnirestInstance();
    }
}
