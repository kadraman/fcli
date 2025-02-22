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
package com.fortify.cli.common.session.cli.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.session.cli.mixin.ISessionNameSupplier;
import com.fortify.cli.common.session.helper.FcliSessionLogoutException;
import com.fortify.cli.common.session.helper.ISessionDescriptor;

public abstract class AbstractSessionLogoutCommand<D extends ISessionDescriptor> extends AbstractSessionCommand<D> implements IActionCommandResultSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSessionLogoutCommand.class);
    
    @Override
    public JsonNode getJsonNode() {
        var sessionNameSupplier = getSessionNameSupplier();
        String sessionName = sessionNameSupplier==null?"default":sessionNameSupplier.getSessionName();
        JsonNode result = null;
        var sessionHelper = getSessionHelper();
        if ( sessionHelper.exists(sessionName) ) {
        	result = sessionHelper.sessionSummaryAsObjectNode(sessionName);
            try {
                logout(sessionName, sessionHelper.get(sessionName, false));
                getSessionHelper().destroy(sessionName);
            } catch (Exception e) {
                if ( e instanceof FcliSessionLogoutException && !((FcliSessionLogoutException)e).isDestroySession() ) {
                    throw e;
                } else {
                    LOG.warn("Logout failed");
                    LOG.debug("Exception details:", e);
                    getSessionHelper().destroy(sessionName);
                }
            }
        }
        return result;
    }
    
    @Override
    public String getActionCommandResult() {
    	return "TERMINATED";
    }
    
    @Override
    public boolean isSingular() {
    	return false;
    }

    public abstract ISessionNameSupplier getSessionNameSupplier();
    /*******************************************************************************
    * This method will always be invoked on existing sessions, independent of whether the session has expired
    * This is to ensure cleanup of the local session directory and tokens stored in ssc (if the token has already been cleaned up by ssc this should not result in an error)
    *******************************************************************************/
    protected abstract void logout(String sessionName, D sessionDescriptor);
}
