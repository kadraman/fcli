/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.fod._common.session.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionListCommand;
import com.fortify.cli.common.session.cli.mixin.ValidateSessionOptionMixin;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.FoDSessionHelper;
import com.fortify.cli.fod._common.session.helper.FoDSessionValidationHelper;
import com.fortify.cli.fod._common.session.helper.FoDSessionValidationHelper.SessionStatus;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME, sortOptions = false)
public class FoDSessionListCommand extends AbstractSessionListCommand<FoDSessionDescriptor> {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private ValidateSessionOptionMixin validateSessionOption;
    @Getter private FoDSessionHelper sessionHelper = FoDSessionHelper.instance();

    @Override
    public JsonNode getJsonNode() {
        var result = (ArrayNode)super.getJsonNode();
        validateSessionOption.validateIfNeeded(result, this::enrichSessionNode);
        return result;
    }

    private void enrichSessionNode(JsonNode sessionNode) {
        if ( sessionNode instanceof ObjectNode session ) {
            var sessionName = session.path("name").asText(null);
            var descriptor = sessionName==null ? null : getSessionHelper().get(sessionName, false);
            var tokenResponse = descriptor==null ? null : descriptor.getCachedTokenResponse();
            var accessToken = tokenResponse==null ? null : tokenResponse.getAccessToken();
            if ( descriptor==null || accessToken==null ) {
                applySessionStatus(session, new SessionStatus(false, null));
                return;
            }
            try {
                var status = FoDSessionValidationHelper.checkTokenStatus(descriptor.getUrlConfig(), accessToken);
                applySessionStatus(session, status);
            } catch ( FcliSimpleException e ) {
                session.put("expired", "Unknown");
                session.put("expires", "Unknown");
            }
        }
    }

    private void applySessionStatus(ObjectNode session, SessionStatus status) {
        session.put("expired", status.valid() ? "No" : "Yes");
        if ( !status.valid() ) {
            session.put("expires", "N/A");
        } else {
            session.put("expires", session.has("expires") ? session.get("expires").asText() : "Unknown");
        }
    }
}
