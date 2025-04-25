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
package com.fortify.cli.config.proxy.cli.mixin;

import java.util.Set;
import java.util.function.Function;

import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor.ProxyDescriptorBuilder;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor.ProxyMatchMode;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;

import lombok.RequiredArgsConstructor;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public abstract class AbstractProxyOptions {
    @Option(names = {"--user", "-u"}, descriptionKey = "fcli.config.proxy.user")
    @MaskValue(sensitivity = LogSensitivityLevel.medium, description = "PROXY USER")
    private String proxyUser;
    @Option(names = {"--password", "-p"}, interactive = true, echo = false, arity = "0..1", descriptionKey = "fcli.config.proxy.password")
    @MaskValue(sensitivity = LogSensitivityLevel.high, description = "PROXY PASSWORD")
    private char[] proxyPassword;
    @Option(names = {"--priority"}, descriptionKey = "fcli.config.proxy.priority") 
    private Integer priority;
    @Option(names = {"--modules", "-m"}, split = ",", descriptionKey = "fcli.config.proxy.modules") 
    private Set<String> modules;
    @ArgGroup(exclusive=true) private ProxyTargetHostsArgGroup targetHostsArgGroup = new ProxyTargetHostsArgGroup();
    
    public static final class ProxyTargetHostsArgGroup {
        @Option(names={"--include-hosts", "-i"}, split = ",", descriptionKey = "fcli.config.proxy.include-hosts") 
        private Set<String> includedHosts;
        @Option(names={"--exclude-hosts", "-e"}, split = ",", descriptionKey = "fcli.config.proxy.exclude-hosts") 
        private Set<String> excludedHosts;
        
        public Set<String> getTargetHosts() {
            return includedHosts!=null ? includedHosts : excludedHosts; 
        }
        
        public ProxyMatchMode getTargetHostMatchMode() {
            return includedHosts!=null 
                    ? ProxyMatchMode.include 
                    : excludedHosts!=null ? ProxyMatchMode.exclude : null;
        }
    }
    
    protected ProxyDescriptorBuilder getProxyDescriptorBuilder(ProxyDescriptor originalDescriptor) {
        var getter = new ProxyDescriptorGetter(originalDescriptor);
        return ProxyDescriptor.builder()
                .name(getter.get(getName(), ProxyDescriptor::getName, getProxyHostAndPort()))
                .proxyHostAndPort(getter.get(getProxyHostAndPort(), ProxyDescriptor::getProxyHostAndPort))
                .proxyUser(getter.get(proxyUser, ProxyDescriptor::getProxyUser))
                .proxyPassword(getter.get(proxyPassword, ProxyDescriptor::getProxyPassword))
                .priority(getter.get(priority, ProxyDescriptor::getPriority, 0))
                .modules(getter.get(modules, ProxyDescriptor::getModules))
                .modulesMatchMode(ProxyMatchMode.include)
                .targetHostNames(getter.get(targetHostsArgGroup.getTargetHosts(), ProxyDescriptor::getTargetHostNames))
                .targetHostNamesMatchMode(getter.get(targetHostsArgGroup.getTargetHostMatchMode(), ProxyDescriptor::getTargetHostNamesMatchMode));
    }
    
    protected abstract String getName();
    protected abstract String getProxyHostAndPort();
    
    @RequiredArgsConstructor
    private static final class ProxyDescriptorGetter {
        private final ProxyDescriptor originalDescriptor;
        private final <T> T get(T optionValue, Function<ProxyDescriptor, T> f) {
            return get(optionValue, f, null);
        }
        private final <T> T get(T optionValue, Function<ProxyDescriptor, T> f, T defaultValue) {
            return optionValue!=null 
                ? optionValue
                : originalDescriptor==null ? defaultValue : f.apply(originalDescriptor);
        }
    }
}
