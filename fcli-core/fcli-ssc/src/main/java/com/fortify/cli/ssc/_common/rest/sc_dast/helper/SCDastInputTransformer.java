/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.ssc._common.rest.sc_dast.helper;

import com.fasterxml.jackson.databind.JsonNode;

public class SCDastInputTransformer {
    public static final JsonNode getItems(JsonNode input) {
        if ( input.has("items") ) { return input.get("items"); }
        if ( input.has("item") ) { return input.get("item"); }
        return input;
    }
}
