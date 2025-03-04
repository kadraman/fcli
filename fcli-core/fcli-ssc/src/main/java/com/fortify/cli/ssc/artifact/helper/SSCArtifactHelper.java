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
package com.fortify.cli.ssc.artifact.helper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class SSCArtifactHelper {
    public static final int DEFAULT_POLL_INTERVAL_SECONDS = 1;
    
    private SSCArtifactHelper() {}
    
    public static final SSCArtifactDescriptor getArtifactDescriptor(UnirestInstance unirest, String artifactId) {
        return getDescriptor(getArtifactJsonNode(unirest, artifactId));
    }

    private static JsonNode getArtifactJsonNode(UnirestInstance unirest, String artifactId) {
        return unirest.get(SSCUrls.ARTIFACT(artifactId))
                .queryString("embed","scans")
                .asObject(JsonNode.class).getBody().get("data");
    }
    
    public static final SSCArtifactDescriptor delete(UnirestInstance unirest, SSCArtifactDescriptor descriptor) {
        unirest.delete(SSCUrls.ARTIFACT(descriptor.getId())).asObject(JsonNode.class).getBody();
        return descriptor;
    }
    
    public static final SSCArtifactDescriptor purge(UnirestInstance unirest, SSCArtifactDescriptor descriptor) {
        unirest.post(SSCUrls.ARTIFACTS_ACTION_PURGE)
            .body(new SSCAppVersionArtifactPurgeByIdRequest(new String[] {descriptor.getId()}))
            .asObject(JsonNode.class).getBody();
        return descriptor;
    }
    
    public static final JsonNode purge(UnirestInstance unirest, SSCAppVersionArtifactPurgeByDateRequest purgeRequest) {
        return unirest.post(SSCUrls.PROJECT_VERSIONS_ACTION_PURGE)
                .body(purgeRequest).asObject(JsonNode.class).getBody();
    }
    
    public static final JsonNode approve(UnirestInstance unirest, String artifactId, String message){
        int[] artifactIds = {Integer.parseInt(artifactId)};

        JsonNode jsonNode = new ObjectMapper().createObjectNode()
                .putPOJO("artifactIds", artifactIds)
                .put("comment", message);

        return unirest.post(SSCUrls.ARTIFACTS_ACTION_APPROVE)
                .body(jsonNode)
                .asObject(JsonNode.class).getBody();
    }
    
    public static final String getArtifactStatus(UnirestInstance unirest, String artifactId){
        return JsonHelper.evaluateSpelExpression(
                unirest.get(SSCUrls.ARTIFACT(artifactId)).asObject(JsonNode.class).getBody(),
                "data.status",
                String.class
        );
    }
    
    @Data 
    @Reflectable @NoArgsConstructor @AllArgsConstructor
    private static final class SSCAppVersionArtifactPurgeByIdRequest {
        private String[] artifactIds;
    }
    
    @Data @Builder
    @Reflectable @NoArgsConstructor @AllArgsConstructor
    public static final class SSCAppVersionArtifactPurgeByDateRequest {
        private String[] projectVersionIds;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSxxx") 
        private OffsetDateTime purgeBefore;
    }
    
    private static final SSCArtifactDescriptor getDescriptor(JsonNode scanNode) {
        return JsonHelper.treeToValue(scanNode, SSCArtifactDescriptor.class);
    }

    public static JsonNode addScanTypes(JsonNode record) {
        if ( record instanceof ObjectNode && record.has("_embed") ) {
            JsonNode _embed = record.get("_embed");
            String scanTypesString = "";
            if ( _embed.has("scans") ) {
                // TODO Can we get rid of unchecked conversion warning?
                @SuppressWarnings("unchecked")
                ArrayList<String> scanTypes = JsonHelper.evaluateSpelExpression(_embed, "scans?.![type]", ArrayList.class);
                scanTypesString = scanTypes.stream().collect(Collectors.joining(", "));   
            }
            record = ((ObjectNode)record).put("scanTypes", scanTypesString);
        }
        return record;
    }
}
