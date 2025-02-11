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
package com.fortify.cli.sc_sast.scan.helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper=true) 
@JsonIgnoreProperties(ignoreUnknown = true)
public class SCSastScanJobDescriptor extends JsonNodeHolder {
    private String jobToken;
    private String scanState; // Original property name: state, renamed in SCSastControllerScanJobHelper
    private boolean hasFiles;
    private String publishState; // Original property name: sscUploadState, renamed in SCSastControllerScanJobHelper
    private String scaProgress;
    private String sscArtifactState;
    private int endpointVersion;
    private boolean publishRequested;
}
