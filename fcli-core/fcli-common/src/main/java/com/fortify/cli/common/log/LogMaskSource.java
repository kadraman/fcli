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
package com.fortify.cli.common.log;

/**
 * The values in this enum are used to identify where a masked value originated from.
 *
 * @author Ruud Senden
 */
public enum LogMaskSource {
    SESSION, CLI_OPTION, ENV_VAR, HTTP_RESPONSE, GRPC_RESPONSE;
}
