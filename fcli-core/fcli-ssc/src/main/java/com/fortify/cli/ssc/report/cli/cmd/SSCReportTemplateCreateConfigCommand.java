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
package com.fortify.cli.ssc.report.cli.cmd;

import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.output.cli.cmd.AbstractGenerateConfigCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@MCPExclude
@Command(name = OutputHelperMixins.CreateTemplateConfig.CMD_NAME) @CommandGroup("template")
public class SSCReportTemplateCreateConfigCommand extends AbstractGenerateConfigCommand {
    @Getter @Mixin private OutputHelperMixins.CreateTemplateConfig outputHelper;
    @Getter private final String resourceFileName = "com/fortify/cli/ssc/report/ReportTemplateConfig.yml";

    @Option(names = {"-c", "--config"}, defaultValue = "ReportTemplateConfig.yml") 
    @Getter private String outputFileName;
}
