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
package com.fortify.cli.ssc.access_control.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.access_control.helper.SSCUserSpecPredicate.MatchMode;

import kong.unirest.UnirestInstance;

public final class SSCUserHelper {
    private final UnirestInstance unirest;
    private ArrayNode allAuthEntities;
    
    public SSCUserHelper(UnirestInstance unirest) {
        this.unirest = unirest;
    }
    
    public final ArrayNode getAllAuthEntities() {
        if ( allAuthEntities==null ) {
            allAuthEntities = (ArrayNode)unirest.get(SSCUrls.AUTH_ENTITIES)
                    .queryString("limit","-1").asObject(JsonNode.class).getBody().get("data");
        }
        return allAuthEntities;
    }
    
    public final ArrayNode getAuthEntities(boolean allowMultipleMatches, boolean failOnUnmatched, String... authEntitySpecs) {
        SSCUserSpecPredicate predicate = new SSCUserSpecPredicate(authEntitySpecs, MatchMode.INCLUDE, allowMultipleMatches);
        ArrayNode result = JsonHelper.stream(getAllAuthEntities())
                    .filter(predicate)
                    .collect(JsonHelper.arrayNodeCollector());
        if ( failOnUnmatched ) {
            predicate.checkUnmatched();
        }
        return result;
    }
}
