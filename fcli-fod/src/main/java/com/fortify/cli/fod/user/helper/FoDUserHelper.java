/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.fod.user.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDescriptor;
import com.fortify.cli.fod.rest.FoDUrls;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

import javax.validation.ValidationException;

public class FoDUserHelper {
    public static final FoDUserDescriptor getUser(UnirestInstance unirestInstance, String userNameOrId, boolean failIfNotFound) {
        GetRequest request = unirestInstance.get(FoDUrls.USERS);
        try {
            int attrId = Integer.parseInt(userNameOrId);
            request = request.queryString("filters", String.format("userId:%d", attrId));
        } catch (NumberFormatException nfe) {
            request = request.queryString("filters", String.format("userName:%s", userNameOrId));
        }
        JsonNode attr = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && attr.size() == 0) {
            throw new ValidationException("No user found for name or id: " + userNameOrId);
        } else if (attr.size() > 1) {
            throw new ValidationException("Multiple users found for name or id: " + userNameOrId);
        }
        return attr.size() == 0 ? null : JsonHelper.treeToValue(attr.get(0), FoDUserDescriptor.class);
    }
}
