/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.action.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonHelper.JsonNodeDeepCopyWalker;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author Ruud Senden
 */
public final class ActionRunnerHelper {
    public static final JsonNode fmt(ActionRunnerContext ctx, String formatterName, JsonNode input) {
        var format = ctx.getConfig().getAction().getFormatters().get(formatterName);
        return new JsonNodeSpelEvaluatorWalker(ctx, input).walk(format);
    }
    
    @RequiredArgsConstructor
    private static final class JsonNodeSpelEvaluatorWalker extends JsonNodeDeepCopyWalker {
        private final ActionRunnerContext ctx;
        private final JsonNode input;
        @Override
        protected JsonNode copyValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
            if ( node instanceof POJONode ) {
                var pojoValue = ((POJONode)node).getPojo();
                if ( pojoValue instanceof TemplateExpression ) {
                    var rawResult = ctx.getSpelEvaluator().evaluate((TemplateExpression)pojoValue, input, Object.class);
                    if ( rawResult instanceof CharSequence ) {
                        rawResult = new TextNode(((String)rawResult).replace("\\n", "\n"));
                    }
                    return JsonHelper.getObjectMapper().valueToTree(rawResult);
                }
            }
            return super.copyValue(state, path, parent, node);
        }
    }

}
