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

package com.fortify.cli.fod.mast_scan.helper;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor @AllArgsConstructor
@Data @Builder
public class FoDScanConfigMobileSetupRequest {
    private Integer assessmentTypeId;
    private String auditPreferenceType;
    private String entitlementFrequencyType;
    private String frameworkType;
    private String platformType;
    private String timeZone;
}
