/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.common.util;

import java.util.function.Supplier;

import com.fortify.cli.common.exception.FcliSimpleException;

public final class EnvHelper {
    private static final String PFX = "FCLI";
    private EnvHelper() {}
    
    public static final String getUserHome() {
        return envOrDefault(PFX+"_USER_HOME", ()->System.getProperty("user.home"));
    }
    
    public static final void checkSecondaryWithoutPrimary(String secondaryEnvName, String primaryEnvName) {
        if ( env(primaryEnvName)==null && env(secondaryEnvName)!=null ) {
            throw new FcliSimpleException("Environment variable "+secondaryEnvName+" requires "+primaryEnvName+" to be set as well");
        }
    }
    
    public static final void checkBothOrNone(String envName1, String envName2) {
        checkSecondaryWithoutPrimary(envName1, envName2);
        checkSecondaryWithoutPrimary(envName2, envName1);
    }
    
    public static final void checkExclusive(String envName1, String envName2) {
        if ( env(envName1)!=null && env(envName2)!=null ) {
            throw new FcliSimpleException("Only one of "+envName1+" and "+envName2+" environment variables may be configured");
        }
    }
    
    public static final String envNameWithOrWithoutProductEnvId(String productEnvId, String suffix) {
        String envNameWithProductName = envName(productEnvId, suffix);
        String envValueWithProductName = env(envNameWithProductName);
        String envNameWithoutProductName = envName(null, suffix);
        String envValueWithoutProductName = env(envNameWithoutProductName);
        boolean hasEnvValueWithProductName = StringUtils.isNotBlank(envValueWithProductName);
        boolean hasEnvValueWithoutProductName = StringUtils.isNotBlank(envValueWithoutProductName);
        return hasEnvValueWithProductName || !hasEnvValueWithoutProductName
                ? envNameWithProductName
                : envNameWithoutProductName;
    }
    
    public static final String envName(String productEnvId, String suffix) {
        return StringUtils.isBlank(productEnvId)
                ? String.format("%s_%s", PFX, suffix)
                : String.format("%s_%s_%s", PFX, productEnvId, suffix);
    }
    
    public static final String envOrDefault(String name, Supplier<String> defaultSupplier) {
        var value = env(name);
        return StringUtils.isNotBlank(value) ? value : defaultSupplier.get();    
    }
    
    public static final String envOrDefault(String name, String defaultValue) {
        return envOrDefault(name, ()->defaultValue);    
    }
    
    public static final String requiredEnv(String name, String message) {
        return envOrDefault(name, ()->{throw new FcliSimpleException(message);});
    }
    
    public static final String requiredEnv(String name) {
        return requiredEnv(name, String.format("Required environment variable %s not defined", name));
    }
    
    /**
     * Get the value of the environment variable with the given name.
     * This method allows environment variables to be overridden through
     * system properties named 'fcli.env.VAR_NAME', which is mainly 
     * useful for unit/functional testing, but may also be useful for
     * other purposes.
     */
    public static final String env(String name) {
        return System.getProperty(envSystemPropertyName(name), System.getenv(name));
    }

    public static String envSystemPropertyName(String envName) {
        return "fcli.env."+envName;
    }

    public static final Boolean asBoolean(String s) {
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    public static final char[] asCharArray(String s) {
        return s==null ? null : s.toCharArray();
    }

    public static final Integer asInteger(String s) {
        return s==null ? null : Integer.parseInt(s);
    }
}
