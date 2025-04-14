package com.fortify.cli.aviator.core.model;

import lombok.Data;

import java.io.File;

@Data
public class FPRAuditResult {
    private File updatedFile;
    private String status;
    private String message;
    private int issuesSuccessfullyAudited;
    private int totalIssuesToAudit;

    public FPRAuditResult(File updatedFile, String status, String message,
                          int issuesSuccessfullyAudited, int totalIssuesToAudit) {
        this.updatedFile = updatedFile;
        this.status = status;
        this.message = message;
        this.issuesSuccessfullyAudited = issuesSuccessfullyAudited;
        this.totalIssuesToAudit = totalIssuesToAudit;
    }
}