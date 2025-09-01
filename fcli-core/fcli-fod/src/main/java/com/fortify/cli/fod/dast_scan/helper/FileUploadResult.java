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
package com.fortify.cli.fod.dast_scan.helper;

import lombok.Getter;

import java.util.ArrayList;

@Getter
public class FileUploadResult {
    private final int fileId;
    private final ArrayList<String> hosts;

    public FileUploadResult(int fileId, ArrayList<String> hosts) {
        this.fileId = fileId;
        this.hosts = hosts;
    }

}