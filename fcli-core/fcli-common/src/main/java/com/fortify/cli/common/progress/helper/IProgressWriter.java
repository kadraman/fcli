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
package com.fortify.cli.common.progress.helper;

public interface IProgressWriter extends AutoCloseable {
    boolean isMultiLineSupported();
    void writeProgress(String message, Object... args);
    void writeInfo(String message, Object... args);
    void writeWarning(String message, Object... args);
    void clearProgress();
    void close();
    String type();
}