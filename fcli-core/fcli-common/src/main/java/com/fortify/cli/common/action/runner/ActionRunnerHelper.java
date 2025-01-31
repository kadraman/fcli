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

import org.springframework.expression.spel.SpelEvaluationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fortify.cli.common.action.model.ActionFormatter;
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
        var formatter = ctx.getConfig().getAction().getFormattersByName().get(formatterName);
        return new JsonNodeOutputWalker(ctx, formatter, input).walk(formatter.getContents());
    }
    
    @RequiredArgsConstructor
    private static final class JsonNodeOutputWalker extends JsonNodeDeepCopyWalker {
        private final ActionRunnerContext ctx;
        private final ActionFormatter outputDescriptor;
        private final JsonNode input;
        @Override
        protected JsonNode copyValue(JsonNode state, String path, JsonNode parent, ValueNode node) {
            if ( !(node instanceof TextNode) ) {
                return super.copyValue(state, path, parent, node);
            } else {
                TemplateExpression expression = outputDescriptor.getValueExpressions().get(path);
                if ( expression==null ) { throw new RuntimeException("No expression for "+path); }
                try {
                    var rawResult = ctx.getSpelEvaluator().evaluate(expression, input, Object.class);
                    if ( rawResult instanceof CharSequence ) {
                        rawResult = new TextNode(((String)rawResult).replace("\\n", "\n"));
                    }
                    return JsonHelper.getObjectMapper().valueToTree(rawResult);
                } catch ( SpelEvaluationException e ) {
                    throw new RuntimeException("Error evaluating action expression "+expression.getExpressionString(), e);
                }
            }
        }
    }

}
