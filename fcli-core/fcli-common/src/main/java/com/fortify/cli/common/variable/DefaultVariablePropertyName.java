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
package com.fortify.cli.common.variable;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.fortify.cli.common.output.writer.output.standard.StandardOutputWriter;

/**
 * <p>When an fcli command is invoked with the `--store` option, {@link StandardOutputWriter} looks for this 
 * annotation on the current command, and any of its parent commands. If found, it will store the given
 * default property name alongside other variable metadata, allowing this default property to be resolved
 * if the user doesn't specify a property name in a variable reference.</p>
 * 
 * <p>There may be cases where some command outputs different data than most other commands within a command
 * tree, and thus shouldn't 'inherit' the default property name from a parent command. In such cases, the 
 * sub-command can 'override' the default property name by adding this annotation, either with an alternative
 * default property name, or with an empty property name to indicate that there is no default property name.</p>   
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
public @interface DefaultVariablePropertyName {
    String value();
}
