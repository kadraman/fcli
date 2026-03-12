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
/*
 * Copyright 2021-2026 Open Text.
 */
package com.fortify.cli.common.cli.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnirestContext;

/**
 * Per-top-level execution context holding mutable execution-scoped state.
 */
public final class FcliExecutionContext {
    private final ObjectNode globalValues = JsonHelper.getObjectMapper().createObjectNode();
    private final UnirestContext unirestContext = new UnirestContext();

    public ObjectNode getGlobalValues() { return globalValues; }
    public UnirestContext getUnirestContext() { return unirestContext; }

    public String info() {
        return String.format("FcliExecutionContext@%s(%d) actionGlobalValues@%s(%d) unirestContext@%s(%s)",
                Integer.toHexString(System.identityHashCode(this)),
                FcliExecutionContextHolder.stackDepth(),
                Integer.toHexString(System.identityHashCode(globalValues)),
                globalValues.size(),
                Integer.toHexString(System.identityHashCode(unirestContext)),
                unirestContext.getCachedInstanceCount());
    }
}
