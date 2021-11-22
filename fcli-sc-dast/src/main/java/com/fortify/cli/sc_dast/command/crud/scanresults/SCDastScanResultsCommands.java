package com.fortify.cli.sc_dast.command.crud.scanresults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.sc_dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.command.crud.SCDastCrudRootCommands;
import com.fortify.cli.sc_dast.command.crud.SCDastCrudRootCommands.SCDastGetCommand;
import com.fortify.cli.sc_dast.command.crud.scanresults.actions.SCDastScanResultsActionsHandler;
import com.fortify.cli.sc_dast.command.crud.scanresults.options.SCDastScanResultsOptions;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

public class SCDastScanResultsCommands {
    private static final String NAME = "scan-results";
    private static final String DESC = "DAST scan results";

    private static final String _getDefaultOutputColumns() {
        return  "lowCount:Low#" +
                "mediumCount:Medium#" +
                "highCount:High#" +
                "criticalCount:Critical";
    }

    @ReflectiveAccess
    @SubcommandOf(SCDastCrudRootCommands.SCDastGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand implements IOutputConfigSupplier {

        @ArgGroup(exclusive = false, heading = "Get results from a specific scan:%n", order = 1)
        @Getter private SCDastScanResultsOptions scanResultsOptions;

        @Mixin
        @Getter private OutputMixin outputMixin;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest){
            SCDastScanResultsActionsHandler actionsHandler = new SCDastScanResultsActionsHandler(unirest);

            if(scanResultsOptions.isWaitCompletion()) {
                if (scanResultsOptions.isDetailed()){
                    actionsHandler.waitCompletionWithDetails(scanResultsOptions.getScanId(), scanResultsOptions.getWaitInterval());
                } else {
                    actionsHandler.waitCompletion(scanResultsOptions.getScanId(), scanResultsOptions.getWaitInterval());
                }
            }

            JsonNode response = actionsHandler.getScanResults(scanResultsOptions.getScanId());

            outputMixin.write(response);

            return null;
        }

        @Override
		public OutputConfig getOutputOptionsWriterConfig() {
			return SCDastGetCommand.defaultOutputConfig().defaultColumns(_getDefaultOutputColumns());
		}
    }
}

