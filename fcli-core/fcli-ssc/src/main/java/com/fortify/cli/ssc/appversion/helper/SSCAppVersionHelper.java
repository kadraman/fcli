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
package com.fortify.cli.ssc.appversion.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.system_state.helper.SSCJobDescriptor;
import com.fortify.cli.ssc.system_state.helper.SSCJobHelper;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Data;

public class SSCAppVersionHelper {
    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[] {"project:application"}).transform(record);
    }
    
    public static final SSCAppVersionDescriptor getRequiredAppVersion(UnirestInstance unirest, String appVersionNameOrId, String delimiter, String... fields) {
        SSCAppVersionDescriptor descriptor = getOptionalAppVersion(unirest, appVersionNameOrId, delimiter, fields);
        if ( descriptor==null ) {
            throw new FcliSimpleException("No application version found for application version name or id: "+appVersionNameOrId);
        }
        return descriptor;
    }
    
    public static final SSCAppVersionDescriptor getOptionalAppVersion(UnirestInstance unirest, String appVersionNameOrId, String delimiter, String... fields) {
        try {
            int versionId = Integer.parseInt(appVersionNameOrId);
            return getOptionalAppVersionFromId(unirest, versionId, fields);
        } catch (NumberFormatException nfe) {
            return getOptionalAppVersionFromAppAndVersionName(unirest, SSCAppAndVersionNameDescriptor.fromCombinedAppAndVersionName(appVersionNameOrId, delimiter), fields);
        }
    }
    
    public static final SSCAppVersionDescriptor getOptionalAppVersionFromId(UnirestInstance unirest, int versionId, String... fields) {
        GetRequest request = getBaseRequest(unirest, fields).queryString("q", String.format("id:%d", versionId));
        return getOptionalDescriptor(request);
    }
    
    public static final SSCAppVersionDescriptor getOptionalAppVersionFromAppAndVersionName(UnirestInstance unirest, SSCAppAndVersionNameDescriptor appAndVersionNameDescriptor, String... fields) {
        GetRequest request = getBaseRequest(unirest, fields);
        request = request.queryString("q", String.format("project.name:\"%s\",name:\"%s\"", appAndVersionNameDescriptor.getAppName(), appAndVersionNameDescriptor.getVersionName()));
        return getOptionalDescriptor(request);
    }

    private static GetRequest getBaseRequest(UnirestInstance unirest, String... fields) {
        GetRequest request = unirest.get("/api/v1/projectVersions?includeInactive=true&limit=2");
        if ( fields!=null && fields.length>0 ) {
            request.queryString("fields", String.join(",", fields));
        }
        return request;
    }

    private static final SSCAppVersionDescriptor getOptionalDescriptor(GetRequest request) {
        ObjectNode body = request.asObject(ObjectNode.class).getBody();
        JsonNode versions = body==null ? null : body.get("data");
        if ( versions.size()>1 ) {
            throw new FcliSimpleException("Multiple application versions found");
        }
        return versions==null || versions.size()==0 ? null : JsonHelper.treeToValue(versions.get(0), SSCAppVersionDescriptor.class);
    }
    
    public static final JsonNode getAttributes(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        return unirest.get(SSCUrls.PROJECT_VERSION_ATTRIBUTES(descriptor.getVersionId()))
                .asObject(ObjectNode.class)
                .getBody()
                .get("data");
    }
    
    public static final JsonNode getUsers(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        return unirest.get(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(descriptor.getVersionId()))
                .asObject(ObjectNode.class)
                .getBody()
                .get("data");
    }
    
    public static final SSCJobDescriptor refreshMetrics(UnirestInstance unirest, SSCAppVersionDescriptor descriptor) {
        if ( !descriptor.isRefreshRequired() ) {
            return null;
        } else {
            JsonNode response = unirest.post(SSCUrls.PROJECT_VERSIONS_ACTION_REFRESH)
                .body(new SSCAppVersionRefreshRequest(descriptor.getVersionId()))
                .asObject(ObjectNode.class)
                .getBody();
            return SSCJobHelper.getJobDescriptor(unirest, response.get("data").get("data").get("id").textValue());
        }
    }
    
    @Data
    @Reflectable @AllArgsConstructor
    private static final class SSCAppVersionRefreshRequest {
        private final String[] projectVersionIds;
        
        public SSCAppVersionRefreshRequest(String appVersionId) {
            this.projectVersionIds = new String[] {appVersionId};
        }
    }
}
