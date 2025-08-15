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
package com.fortify.cli.common.http.proxy.helper;

import java.net.URI;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data @EqualsAndHashCode(callSuper = false) 
@Builder
@Reflectable @NoArgsConstructor @AllArgsConstructor 
public class ProxyDescriptor extends JsonNodeHolder {
    private String name;
    private int priority;
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private char[] proxyPassword;
    private Set<String> modules;
    private ProxyMatchMode modulesMatchMode;
    private Set<String> targetHostNames;
    private ProxyMatchMode targetHostNamesMatchMode;
    
    public String getName() {
        return name!=null ? name : (proxyHost+":"+proxyPort); 
    }
    
    @JsonIgnore
    public String getProxyPasswordAsString() {
        return proxyPassword==null ? null : String.valueOf(proxyPassword);
    }
    
    @JsonIgnore
    public String getProxyHostAndPort() {
        return String.format("%s:%s", proxyHost, proxyPort);
    }
    
    public boolean matches(String module, String url) {
        return matchesModule(module) && matchesHost(URI.create(url).getHost());
    }
    
    public static enum ProxyMatchMode {
        include, exclude;
    }
    
    private boolean matchesModule(String module) {
        return modules==null || modules.contains(module)==ProxyMatchMode.include.equals(modulesMatchMode);
    }
    
    private boolean matchesHost(String host) {
        boolean matching = targetHostNames==null 
                ? false
                : targetHostNames.stream()
                    .anyMatch(hostPattern->matchesHost(hostPattern, host));
        return matching==ProxyMatchMode.include.equals(targetHostNamesMatchMode);
    }
    
    private boolean matchesHost(String hostPattern, String host) {
        String regex = hostPattern.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").replaceAll("\\?", ".");
        return host.matches(regex);
    }
    
    public static final class ProxyDescriptorBuilder {
        public ProxyDescriptorBuilder proxyHostAndPort(String proxyHostAndPort) {
            return proxyHost(StringUtils.substringBefore(proxyHostAndPort, ":"))
                    .proxyPort(Integer.parseInt(StringUtils.substringAfter(proxyHostAndPort, ":")));
        }
    }
}
