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

package com.fortify.cli.fod.mast_scan.cli.cmd;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDEntitlementFrequencyTypeMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.mast_scan.helper.FoDScanConfigMobileDescriptor;
import com.fortify.cli.fod.mast_scan.helper.FoDScanConfigMobileHelper;
import com.fortify.cli.fod.mast_scan.helper.FoDScanConfigMobileSetupRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Setup.CMD_NAME, hidden = false) @CommandGroup("*-scan-setup")
@DisableTest(TestType.CMD_DEFAULT_TABLE_OPTIONS_PRESENT)
public class FoDMastScanSetupCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    private static final Log LOG = LogFactory.getLog(FoDMastScanStartCommand.class);
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    @Getter @Mixin private OutputHelperMixins.Start outputHelper;

    @Mixin
    private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin
    private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;

    @Option(names = {"--assessment-type"}, required = true)
    private String mobileAssessmentType; // Plain text name as custom assessment types can be created
    @Option(names = {"--entitlement-id"})
    private Integer entitlementId;
    @Mixin private FoDEntitlementFrequencyTypeMixins.RequiredOption entitlementFrequencyTypeMixin;
    private enum MobileFrameworks { iOS, Android }
    @Option(names = {"--framework"}, required = true)
    private MobileFrameworks mobileFramework;
    @Option(names = {"--timezone"}, defaultValue = "UTC")
    private String timezone;
    private enum MobileAuditPreferenceTypes { Manual, None }
    @Option(names = {"--audit-preference"}, defaultValue = "None")
    private MobileAuditPreferenceTypes auditPreferenceType;
    private enum MobilePlatforms { Phone, Tablet, Both }
    @Option(names = {"--platform"}, required = true)
    private MobilePlatforms mobilePlatform;
    @Option(names={"--skip-if-exists"})
    private Boolean skipIfExists = false;
 
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try (var progressWriter = progressWriterFactory.create()) {
            var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
            var setupDescriptor = FoDScanConfigMobileHelper.getSetupDescriptor(unirest, releaseDescriptor.getReleaseId());
            if ( skipIfExists && setupDescriptor.getAssessmentTypeId()!=0 ) {
                return setupDescriptor.asObjectNode().put("__action__", "SKIPPED_EXISTING");
            } else {
                return setup(unirest, releaseDescriptor, setupDescriptor).asObjectNode();
            }
        }
    }

    private FoDScanConfigMobileDescriptor setup(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanConfigMobileDescriptor currentSetup) {
        var relId = releaseDescriptor.getReleaseId();

        LOG.info("Finding appropriate entitlement to use.");

        var atd = getAssessmentTypeDescriptor(unirest, relId);
        Integer assessmentTypeId = atd.getAssessmentTypeId();
        Integer entitlementIdToUse = atd.getEntitlementId();

        validateEntitlement(currentSetup, entitlementIdToUse, relId, atd);
        LOG.info("Release will be usig entitlement " + entitlementIdToUse);

        FoDScanConfigMobileSetupRequest setupMastScanRequest = FoDScanConfigMobileSetupRequest.builder()
                .assessmentTypeId(assessmentTypeId)
                .frameworkType(mobileFramework.name())
                .platformType(mobilePlatform.name())
                .auditPreferenceType(auditPreferenceType.name())
                .timeZone(timezone).build();

        return FoDScanConfigMobileHelper.setupScan(unirest, releaseDescriptor, setupMastScanRequest);
    }

    private void validateEntitlement(FoDScanConfigMobileDescriptor currentSetup, Integer entitlementIdToUse, String relId, FoDReleaseAssessmentTypeDescriptor atd) {
        // validate entitlement specified or currently in use against assessment type found
        if (entitlementId != null && entitlementId > 0) {
            // check if "entitlement id" explicitly matches what has been found
            if (!Objects.equals(entitlementIdToUse, entitlementId)) {
                throw new IllegalArgumentException("Cannot find appropriate assessment type for use with entitlement: " + entitlementId + "=" + entitlementIdToUse);
            }
        } else {
            if (currentSetup.getEntitlementId() != null && currentSetup.getEntitlementId() > 0) {
                // check if "entitlement id" is already configured
                if (!Objects.equals(entitlementIdToUse, currentSetup.getEntitlementId())) {
                    LOG.warn("Changing current release entitlement from " + currentSetup.getEntitlementId());
                }
            }
        }
        // check if the entitlement is still valid
        FoDReleaseAssessmentTypeHelper.validateEntitlement(relId, atd);
    }

    private FoDReleaseAssessmentTypeDescriptor getAssessmentTypeDescriptor(UnirestInstance unirest, String relId) {
        // find an appropriate assessment type to use
        Optional<FoDReleaseAssessmentTypeDescriptor> atd = Arrays.stream(
                        FoDReleaseAssessmentTypeHelper.getAssessmentTypes(unirest,
                                relId, FoDScanType.Mobile,
                                entitlementFrequencyTypeMixin.getEntitlementFrequencyType(),
                                false, true)
                ).filter(n -> n.getName().equals(mobileAssessmentType))
                .findFirst();
        return atd.orElseThrow(()->new IllegalArgumentException("Cannot find appropriate assessment type for specified options."));
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(getUnirestInstance());
        return ((ObjectNode)record)
            .put("scanType", mobileAssessmentType)
            .put("setupType", auditPreferenceType.name())
            .put("applicationName", releaseDescriptor.getApplicationName())
            .put("releaseName", releaseDescriptor.getReleaseName())
            .put("microserviceName", releaseDescriptor.getMicroserviceName());
    }

    @Override
    public String getActionCommandResult() {
        return "SETUP";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
    /*
    @Override
    protected FoDScanDescriptor startScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        try ( var progressWriter = progressWriterFactory.create() ) {
            Properties fcliProperties = FcliBuildPropertiesHelper.getBuildProperties();
            String relId = releaseDescriptor.getReleaseId();
            Integer entitlementIdToUse = 0;
            Integer assessmentTypeId = 0;
            Boolean isRemediation = false;

            // if we have requested remediation scan use it to find appropriate assessment type
            if (remediationScanType != null && remediationScanType.getRemediationScanPreferenceType() != null) {
                if (remediationScanType.getRemediationScanPreferenceType().equals(FoDEnums.RemediationScanPreferenceType.RemediationScanIfAvailable) ||
                        remediationScanType.getRemediationScanPreferenceType().equals(FoDEnums.RemediationScanPreferenceType.RemediationScanOnly)) {
                    isRemediation = true;
                }
            }

            // get current setup
            // NOTE: there is currently no GET method for retrieving scan setup so the following cannot be used:
            // FoDMobileScanSetupDescriptor foDMobileScanSetupDescriptor = FoDMobileScanHelper.getSetupDescriptor(unirest, relId);

            LOG.info("Finding appropriate entitlement to use.");

            // find an appropriate assessment type to use
            Optional<FoDReleaseAssessmentTypeDescriptor> atd = Arrays.stream(
                            FoDReleaseAssessmentTypeHelper.getAssessmentTypes(unirest,
                                    relId, FoDScanType.Mobile,
                                    entitlementFrequencyTypeMixin.getEntitlementFrequencyType(),
                                    isRemediation, true)
                    ).filter(n -> n.getName().equals(mobileAssessmentType))
                    .findFirst();
            if (atd.isEmpty()) {
                throw new IllegalArgumentException("Cannot find appropriate assessment type for specified options.");
            }
            assessmentTypeId = atd.get().getAssessmentTypeId();
            entitlementIdToUse = atd.get().getEntitlementId();

            // validate entitlement specified or currently in use against assessment type found
            if (entitlementId != null && entitlementId > 0) {
                // check if "entitlement id" explicitly matches what has been found
                if (!Objects.equals(entitlementIdToUse, entitlementId)) {
                    throw new IllegalArgumentException("Cannot find appropriate assessment type with entitlement: " + entitlementId);
                }
            } else {
                // NOTE: there is currently no GET method for retrieving scan setup so the following cannot be used:
                //if (currentSetup.getEntitlementId() != null && currentSetup.getEntitlementId() > 0) {
                //    // check if "entitlement id" is already configured
                //    if (!Objects.equals(entitlementIdToUse, currentSetup.getEntitlementId())) {
                //        progressWriter.writeI18nWarning("fcli.fod.scan-config.setup-mast.changing-entitlement");
                //    }
                // }
            }
            LOG.info("Configuring release to use entitlement " + entitlementIdToUse);

            // check if the entitlement is still valid
            FoDReleaseAssessmentTypeHelper.validateEntitlement(relId, atd.get());
            LOG.info("The entitlement " + entitlementIdToUse + " is valid");

            // validate timezone (if specified)
            String timeZoneToUse = FoDScanHelper.validateTimezone(unirest, timezone);

            String startDateStr = (startDate == null || startDate.isEmpty())
                    ? LocalDateTime.now().format(dtf)
                    : LocalDateTime.parse(startDate, dtf).toString();

            FoDScanMobileStartRequest startScanRequest = FoDScanMobileStartRequest.builder()
                    .startDate(startDateStr)
                    .assessmentTypeId(assessmentTypeId)
                    .entitlementId(entitlementIdToUse)
                    .entitlementFrequencyType(entitlementFrequencyTypeMixin.getEntitlementFrequencyType().name())
                    .timeZone(timeZoneToUse)
                    .frameworkType(mobileFramework.name())
                    .platformType(mobilePlatform.name())
                    .scanMethodType("Other")
                    .notes(notes != null && !notes.isEmpty() ? notes : "")
                    .scanTool(fcliProperties.getProperty("projectName", "fcli"))
                    .scanToolVersion(fcliProperties.getProperty("projectVersion", "unknown")).build();

            return FoDScanMobileHelper.startScan(unirest, progressWriter, releaseDescriptor, startScanRequest, scanFileMixin.getFile());
        }
    }*/
}
