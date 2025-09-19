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
package com.fortify.cli.ssc.plugin.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.common.mcp.MCPExclude;

import picocli.CommandLine.Command;

@MCPExclude
@Command(
        name = "plugin",
        subcommands = {
                SSCPluginInstallCommand.class,
                SSCPluginUninstallCommand.class,
                SSCPluginEnableCommand.class,
                SSCPluginDisableCommand.class,
                SSCPluginGetCommand.class,
                SSCPluginListCommand.class
        }

)
public class SSCPluginCommands extends AbstractContainerCommand {
}
