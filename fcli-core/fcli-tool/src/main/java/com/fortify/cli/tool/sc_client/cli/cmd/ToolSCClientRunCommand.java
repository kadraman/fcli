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
package com.fortify.cli.tool.sc_client.cli.cmd;

import java.nio.file.Files;
import java.util.List;

import com.fortify.cli.tool._common.cli.cmd.AbstractToolRunShellOrJavaCommand;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;

@Command(name = "run")
public class ToolSCClientRunCommand extends AbstractToolRunShellOrJavaCommand {
    @Getter private String toolName = ToolSCClientCommands.TOOL_NAME;

    @Override
    protected List<String> getShellBaseCommand(ToolInstallationDescriptor descriptor) {
        var ext = ToolPlatformHelper.isWindows() ? ".bat" : "";
        return List.of(descriptor.getBinPath().resolve("scancentral"+ext).toString());
    }
    
    @Override
    protected List<String> getJavaHomeEnvVarNames() {
        return List.of("SCANCENTRAL_JAVA_HOME", "JAVA_HOME");
    }
    
    @Override @SneakyThrows
    protected String getJar(ToolInstallationDescriptor descriptor) {
        var coreLibPath = descriptor.getInstallPath().resolve("Core/lib");
        return Files.find(coreLibPath, 1, (path, basicFileAttributes) -> path.getFileName().toString().startsWith("scancentral-launcher"))
                .findFirst().orElseThrow(()->new IllegalStateException("Can't find ScanCentral Client launcher jar"))
                .toString();
    }
}
