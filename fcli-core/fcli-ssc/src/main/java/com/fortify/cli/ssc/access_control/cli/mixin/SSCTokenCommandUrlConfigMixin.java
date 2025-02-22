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
package com.fortify.cli.ssc.access_control.cli.mixin;

import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUrlConfigSupplier;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionHelper;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class SSCTokenCommandUrlConfigMixin implements IUrlConfigSupplier {
    @ArgGroup(exclusive=true)
    private SSCUrlConfigOrSessionName options = new SSCUrlConfigOrSessionName();
    
    private static final class SSCUrlConfigOrSessionName {
        @ArgGroup(exclusive=false) private UrlConfigOptions urlConfig = new UrlConfigOptions();
        @Option(names="--ssc-session", defaultValue="default", required=false) private String sessionName;
    } 
    
    @Override
    public IUrlConfig getUrlConfig() {
        if ( options!=null && options.urlConfig!=null && options.urlConfig.hasUrlConfig() ) {
            return options.urlConfig;
        } else if ( options!=null && options.sessionName!=null ) {
            return SSCAndScanCentralSessionHelper.instance().get(options.sessionName, true).getSscUrlConfig();
        } else {
            return SSCAndScanCentralSessionHelper.instance().get("default", true).getSscUrlConfig();
        }
    }
}
