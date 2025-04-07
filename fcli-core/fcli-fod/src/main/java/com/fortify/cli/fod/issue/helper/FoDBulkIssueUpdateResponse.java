/*******************************************************************************
 * Copyright 2021, 2025 Open Text.
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

 package com.fortify.cli.fod.issue.helper;

 import java.util.ArrayList;
 import com.formkiq.graalvm.annotations.Reflectable;
 import com.fortify.cli.common.json.JsonNodeHolder;
 import lombok.Data;
 import lombok.EqualsAndHashCode;
 import lombok.NoArgsConstructor;
 
 @Reflectable @NoArgsConstructor
 @Data @EqualsAndHashCode(callSuper = true)
 public class FoDBulkIssueUpdateResponse extends JsonNodeHolder {
     private ArrayList<VulnerabilityBulkUpdateResult> results;
     private long issueCount;
     private long errorCount;
 
     @Reflectable @NoArgsConstructor
     @Data
     public static final class VulnerabilityBulkUpdateResult {
         private String vulnerabilityId;
         private Integer errorCode;
     }
 }