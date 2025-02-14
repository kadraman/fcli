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
package com.fortify.cli.tool._common.cli.cmd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This abstract base class allows for running a tool that was installed through the
 * corresponding 'install' command. Subclasses can provide the base command to run
 * by overriding the {@link #getBaseCommand(ToolInstallationDescriptor)} method.
 * Alternatively, is there are multiple ways to run the tool (for example shell
 * script or direct java call), subclasses can override the {@link #getBaseCommands(ToolInstallationDescriptor)}
 * method to provide one or more alternative/fallback base commands; if the first
 * base command fails, the second command will be tried, and so on. For the
 * example above (invocation through either shell script or direct invocation),
 * it's recommended to extend from either {@link AbstractToolRunOptionalShellCommand}
 * or {@link AbstractToolRunShellOrJavaCommand}.
 *
 * @author Ruud Senden
 */
public abstract class AbstractToolRunCommand extends AbstractRunnableCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractToolRunCommand.class);
    @Option(names={"-v", "--version"}, required = false, descriptionKey="fcli.tool.run.version")
    private String versionToRun;
    @Option(names={"-d", "--workdir"}, required = false, descriptionKey="fcli.tool.run.workdir")
    private String workDir = System.getProperty("user.dir");
    @Parameters(descriptionKey="fcli.tool.run.tool-args")
    private List<String> toolArgs;
    
    @Override
    public final Integer call() throws Exception {
        var descriptor = getToolInstallationDescriptor();
        var baseCommands = new ArrayList<>(getBaseCommands(descriptor));
        while (true) {
            try {
                return call(baseCommands.get(0));
            } catch ( Exception e ) {
                if ( baseCommands.size()==1) { throw e; } // No more base commands
                LOG.debug("Command execution failed; trying fallback command");
                baseCommands.remove(0);
            }
        }
    }
    
    private final Integer call(List<String> baseCmd) throws Exception {
        if ( baseCmd==null ) { throw new FcliBugException("Base command to execute may not be null"); }
        var fullCmd = Stream.of(baseCmd, toolArgs)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
        LOG.debug("Attempting to run "+fullCmd);
        var process = new ProcessBuilder()
                .command(fullCmd)
                .directory(new File(workDir))
                .inheritIO()
                .start();
        process.waitFor();
        return process.exitValue();
    }
    
    private final ToolInstallationDescriptor getToolInstallationDescriptor() {
        var toolName = getToolName();
        if ( StringUtils.isBlank(versionToRun) ) { 
            return checkNotNull(ToolInstallationDescriptor.loadLastModified(toolName), "No tool installations detected");
        } else {
            var versionDescriptor = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName).getVersion(versionToRun);
            return checkNotNull(ToolInstallationDescriptor.load(toolName, versionDescriptor), "No tool installation detected for version "+versionToRun);
        }
    }

    private ToolInstallationDescriptor checkNotNull(ToolInstallationDescriptor descriptor, String msg) {
        if ( descriptor==null ) {
            throw new FcliSimpleException(msg);
        }
        return descriptor;
    }
    
    protected abstract String getToolName();
    protected List<List<String>> getBaseCommands(ToolInstallationDescriptor descriptor) {
        return List.of(getBaseCommand(descriptor));
    }
    protected List<String> getBaseCommand(ToolInstallationDescriptor descriptor) {
        return null;
    }
    
}
