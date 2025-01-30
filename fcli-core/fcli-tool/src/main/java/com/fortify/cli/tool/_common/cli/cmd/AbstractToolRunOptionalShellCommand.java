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

import java.util.List;

import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;

import picocli.CommandLine.Option;

/**
 *
 * @author Ruud Senden
 */
public abstract class AbstractToolRunOptionalShellCommand extends AbstractToolRunCommand {
    @Option(names={"--use-shell"}, required = false, defaultValue="auto", descriptionKey="fcli.tool.run.use-shell")
    private UseShell useShell;
    
    @Override
    protected final List<List<String>> getBaseCommands(ToolInstallationDescriptor descriptor) {
        if ( useShell==UseShell.yes ) {
            return List.of(getShellBaseCommand(descriptor));
        } else if ( useShell==UseShell.no ) {
            return List.of(getNonShellBaseCommand(descriptor));
        } else {
            return List.of(getShellBaseCommand(descriptor), getNonShellBaseCommand(descriptor));
        }
    }
    
    public enum UseShell {
        no, yes, auto
    }
    
    protected abstract List<String> getShellBaseCommand(ToolInstallationDescriptor descriptor);
    protected abstract List<String> getNonShellBaseCommand(ToolInstallationDescriptor descriptor);
}
