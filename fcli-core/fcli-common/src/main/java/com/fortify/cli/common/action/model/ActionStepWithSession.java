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
package com.fortify.cli.common.action.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'with:session' step, making a session available for the
 * steps specified in the do-block, and cleaning up the session afterwards.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStepWithSession extends AbstractActionElementIf {
    @JsonPropertyDescription("""
        The session login command to run before running the steps specified in the do-block.
        """)
    @JsonProperty(value = "login", required = true) private TemplateExpression loginCommand;
    
    @JsonPropertyDescription("""
        The session logout command to run after the steps in the do-block have been completed \
        either successfully or with failure.
        """)
    @JsonProperty(value = "logout", required = true) private TemplateExpression logoutCommand;
    
    @Override
    public void postLoad(Action action) {
        Action.checkNotNull("login", this, loginCommand);
        Action.checkNotNull("logout", this, logoutCommand);
    }
    
    
}