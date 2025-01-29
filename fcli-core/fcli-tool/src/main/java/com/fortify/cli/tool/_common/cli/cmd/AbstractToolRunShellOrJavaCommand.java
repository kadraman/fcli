/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.tool._common.cli.cmd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;

public abstract class AbstractToolRunShellOrJavaCommand extends AbstractToolRunOptionalShellCommand {
    @Override
    protected final List<String> getNonShellBaseCommand(ToolInstallationDescriptor descriptor) {
        return List.of(getJavaCommand(descriptor), "-jar", getJar(descriptor));
    }
    
    protected final String getJavaCommand(ToolInstallationDescriptor descriptor) {
        var baseJavaCmd = ToolPlatformHelper.isWindows() ? "java.exe" : "java";
        var embeddedJavaCmdPath = descriptor.getInstallPath().resolve("jre/bin").resolve(baseJavaCmd);
        if ( Files.exists(embeddedJavaCmdPath) ) { // Look for java command in embedded JRE
            return embeddedJavaCmdPath.toString();
        } else {
            for ( var javaHomeEnvVarName : getJavaHomeEnvVarNames() ) { // Look for java command in env vars
                var javaHome = EnvHelper.env(javaHomeEnvVarName);
                var javaCmdPathFromEnv = javaHome==null ? null :  Path.of(javaHome, "bin", baseJavaCmd);
                if ( javaCmdPathFromEnv!=null && Files.exists(javaCmdPathFromEnv) ) {
                    return javaCmdPathFromEnv.toString();
                }
            }
        }
        return "java"; // Fallback to use java command from path
    }
    
    protected List<String> getJavaHomeEnvVarNames() {
        return List.of(getToolName().toUpperCase()+"JAVA_HOME", "JAVA_HOME");
    }
    protected abstract String getJar(ToolInstallationDescriptor descriptor);
}
