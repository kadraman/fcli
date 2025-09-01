/**
 * Copyright 2023 Open Text.
 * <p>
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.fod.dast_scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.fod._common.output.cli.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanAssessmentTypeDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedSetupBaseRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.dast_scan.helper.FileUploadResult;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedDescriptor;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Collections;

@Command(name = FoDOutputHelperMixins.SetupApi.CMD_NAME)
@CommandGroup("*-scan-setup")
public class FoDDastAutomatedScanSetupApiCommand extends AbstractFoDDastAutomatedScanSetupCommand {
    private static final Log LOG = LogFactory.getLog(FoDDastAutomatedScanSetupApiCommand.class);
    @Getter
    @Mixin
    private FoDOutputHelperMixins.SetupWorkflow outputHelper;

    @Option(names = {"--type"}, required = true)
    private FoDEnums.DastAutomatedApiTypes apiType;

    @Option(names = {"--file-id"})
    private Integer fileId;

    @Option(names = {"--url", "--api-url"})
    private String apiUrl;

    @Option(names = {"--key", "--api-key"})
    private String apiKey;
    @Option(names = {"--scheme-type"})
    private FoDEnums.ApiSchemeType apiSchemeType;
    @Option(names = {"--host"})
    private String apiHost;
    @Option(names = {"--service-path"})
    private String apiServicePath;

    @Option(names = {"--environment"}, defaultValue = "External")
    private FoDEnums.DynamicScanEnvironmentFacingType environmentFacingType;
    @Option(names = {"--timebox"})
    private Integer timebox;
    @Option(names = {"--timezone"})
    private String timezone;
    @Option(names = {"--network-auth-type"})
    private FoDEnums.DynamicScanNetworkAuthenticationType networkAuthenticationType;
    @Option(names = {"-u", "--network-username"})
    private String username;
    @Option(names = {"-p", "--network-password"})
    private String password;
    @Option(names = {"--false-positive-removal"})
    private Boolean requestFalsePositiveRemoval;
    @Option(names = {"--vpn"})
    private String fodConnectNetwork;

    @Override
    protected String getScanType() {
        return "DAST Automated";
    }

    @Override
    protected String getSetupType() {
        return "API";
    }

    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest, String relId) {
        return null;
    }

    @Override
    protected JsonNode setup(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanConfigDastAutomatedDescriptor currentSetup) {
        var relId = releaseDescriptor.getReleaseId();

        validate();

        LOG.info("Finding appropriate entitlement to use.");

        var atd = FoDReleaseAssessmentTypeHelper.getAssessmentTypeDescriptor(unirest, relId, FoDScanType.Dynamic,
                entitlementFrequencyTypeMixin.getEntitlementFrequencyType(), assessmentType);
        var assessmentTypeId = atd.getAssessmentTypeId();
        var entitlementIdToUse = atd.getEntitlementId();
        assessmentTypeName = atd.getName();

        if (currentSetup != null) validateEntitlement(currentSetup, entitlementIdToUse, relId, atd);
        LOG.info("Configuring release to use entitlement " + entitlementIdToUse);

        FoDEnums.DastAutomatedFileTypes dastFileType = apiType.getDastFileType();
        boolean requiresNetworkAuthentication = false;
        FileUploadResult fileUploadResult = null;

        if (uploadFileMixin != null && uploadFileMixin.getFile() != null) {
            fileUploadResult = uploadFileToUse(unirest, relId, FoDScanType.Dynamic, dastFileType != null ? dastFileType.name() : null);
        }
        FoDScanDastAutomatedSetupBaseRequest.NetworkAuthenticationType networkAuthenticationSettings = null;
        if (networkAuthenticationType != null) {
            requiresNetworkAuthentication = true;
            networkAuthenticationSettings = new FoDScanDastAutomatedSetupBaseRequest.NetworkAuthenticationType(networkAuthenticationType, username, password);
        }
        String timeZoneToUse = FoDScanHelper.validateTimezone(unirest, timezone);
        if (fodConnectNetwork != null) {
            // if Fortify Connect network site override environmentFacingType to Internal
            environmentFacingType = FoDEnums.DynamicScanEnvironmentFacingType.Internal;
        }

        FoDScanAssessmentTypeDescriptor assessmentTypeDescriptor = FoDScanHelper.getEntitlementToUse(unirest, relId, FoDScanType.Dynamic,
                assessmentType, entitlementFrequencyTypeMixin.getEntitlementFrequencyType(),
                entitlementId != null && entitlementId > 0 ? entitlementId : 0);
        entitlementId = assessmentTypeDescriptor.getEntitlementId();
        FoDScanDastAutomatedSetupBaseRequest setupBaseRequest = FoDScanDastAutomatedSetupBaseRequest.builder()
                .dynamicScanEnvironmentFacingType(environmentFacingType != null ?
                        environmentFacingType :
                        FoDEnums.DynamicScanEnvironmentFacingType.Internal)
                .requestFalsePositiveRemoval(requestFalsePositiveRemoval != null ? requestFalsePositiveRemoval : false)
                .timeZone(timeZoneToUse)
                .requiresNetworkAuthentication(requiresNetworkAuthentication)
                .networkAuthenticationSettings(networkAuthenticationSettings)
                .timeBoxInHours(timebox)
                .assessmentTypeId(assessmentTypeDescriptor.getAssessmentTypeId())
                .entitlementId(entitlementId)
                .entitlementFrequencyType(FoDEnums.EntitlementFrequencyType.valueOf(assessmentTypeDescriptor.getFrequencyType()))
                .networkName(fodConnectNetwork != null ? fodConnectNetwork : "")
                .build();

        if (apiType.equals(FoDEnums.DastAutomatedApiTypes.Postman)) {
            //ArrayList<Integer> collectionFileIds = new ArrayList<>(Collections.singletonList(fileIdToUse));
            return FoDScanConfigDastAutomatedHelper.setupPostmanScan(unirest, releaseDescriptor, setupBaseRequest,
                    new ArrayList<>(Collections.singletonList(fileUploadResult != null ? fileUploadResult.getFileId() : 0)));
        } else if (apiType.equals(FoDEnums.DastAutomatedApiTypes.OpenApi)) {
            return FoDScanConfigDastAutomatedHelper.setupOpenApiScan(unirest, releaseDescriptor, setupBaseRequest,
                    fileUploadResult, apiUrl, apiKey);
        } else if (apiType.equals(FoDEnums.DastAutomatedApiTypes.GraphQL)) {
            return FoDScanConfigDastAutomatedHelper.setupGraphQlScan(unirest, releaseDescriptor, setupBaseRequest,
                    fileUploadResult, apiUrl, apiSchemeType, apiHost, apiServicePath);
        } else if (apiType.equals(FoDEnums.DastAutomatedApiTypes.GRPC)) {
            return FoDScanConfigDastAutomatedHelper.setupGrpcScan(unirest, releaseDescriptor, setupBaseRequest,
                    fileUploadResult, apiSchemeType, apiHost, apiServicePath);
        } else {
            throw new FcliSimpleException("Unexpected DAST Automated API type: " + apiType);
        }
    }

    private void validate() {
        if (apiUrl != null && !apiUrl.isEmpty()) {
            if (!apiUrl.matches("^https://.*")) {
                throw new FcliSimpleException("The 'apiUrl' option must include SSL with hostname.");
            }
        }
    }

}
