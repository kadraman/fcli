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

package com.fortify.cli.fod.sast_scan.cli.cmd;

import java.util.Properties;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanStartCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDRemediationScanPreferenceTypeMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.sast.FoDScanSastHelper;
import com.fortify.cli.fod._common.scan.helper.sast.FoDScanSastStartRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.sast_scan.helper.FoDScanConfigSastDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Start.CMD_NAME, hidden = false)
public class FoDSastScanStartCommand extends AbstractFoDScanStartCommand {
    @Getter @Mixin private OutputHelperMixins.Start outputHelper;

    @Option(names = {"--notes"})
    private String notes;
    @Mixin private CommonOptionMixins.RequiredFile scanFileMixin;

    @Mixin private FoDRemediationScanPreferenceTypeMixins.OptionalOption remediationScanType;
    
    @Override
    protected FoDScanDescriptor startScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        Properties fcliProperties = FcliBuildPropertiesHelper.getBuildProperties();
        String relId = releaseDescriptor.getReleaseId();
        Boolean isRemediation = false;

        // if we have requested remediation scan use it to find appropriate assessment type
        if (remediationScanType != null && remediationScanType.getRemediationScanPreferenceType() != null) {
            if (remediationScanType.getRemediationScanPreferenceType().equals(FoDEnums.RemediationScanPreferenceType.RemediationScanIfAvailable) ||
                    remediationScanType.getRemediationScanPreferenceType().equals(FoDEnums.RemediationScanPreferenceType.RemediationScanOnly)) {
                isRemediation = true;
            }
        }

        validateScanSetup(unirest, relId);

        FoDScanSastStartRequest startScanRequest = FoDScanSastStartRequest.builder()
                .isRemediationScan(isRemediation)
                .scanMethodType("Other")
                .notes(notes != null && !notes.isEmpty() ? notes : "")
                .scanTool(fcliProperties.getProperty("projectName", "fcli"))
                .scanToolVersion(fcliProperties.getProperty("projectVersion", "unknown"))
                .build();

        return FoDScanSastHelper.startScanWithDefaults(unirest, releaseDescriptor, startScanRequest, scanFileMixin.getFile());
    }

    private void validateScanSetup(UnirestInstance unirest, String relId) {
        // get current setup and check if its valid
        FoDScanConfigSastDescriptor currentSetup = FoDScanSastHelper.getSetupDescriptor(unirest, relId);
        if (validateEntitlement) {
            if (currentSetup.getEntitlementId() == null || currentSetup.getEntitlementId() <= 0) {
                throw new FcliSimpleException("The static scan configuration for release with id '" + relId +
                        "' has not been setup correctly - 'Entitlement' is missing or empty.");
            }
        }
        if (StringUtils.isBlank(currentSetup.getTechnologyStack())) {
            throw new FcliSimpleException("The static scan configuration for release with id '" + relId +
                    "' has not been setup correctly - 'Technology Stack/Language Level' is missing or empty.");
        }
    }

}
