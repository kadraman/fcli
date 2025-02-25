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
package com.fortify.cli.license.ncd_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportCommitDescriptor;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportRepositoryDescriptor;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;

public final class NcdReportCommitsByRepositoryWriter implements INcdReportCommitsByRepositoryWriter {
    private final IRecordWriter recordWriter;
    
    public NcdReportCommitsByRepositoryWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(RecordWriterFactory.csv, "details/commits-by-repository.csv", false, null);
    }
    
    @Override
    public void writeRepositoryCommit(INcdReportRepositoryDescriptor repositoryDescriptor, INcdReportCommitDescriptor commitDescriptor, NcdReportProcessedAuthorDescriptor authorDescriptor) {
        recordWriter.append(authorDescriptor.updateReportRecord(
                JsonHelper.getObjectMapper().createObjectNode()
                    .put("repositoryUrl", repositoryDescriptor.getUrl())
                    .put("repositoryName", repositoryDescriptor.getFullName())
                    .put("commitId", commitDescriptor.getId())
                    .put("commitDate", commitDescriptor.getDate().toString())
                    .put("commitMessage", commitDescriptor.getMessage().split("\\R",2)[0])));
    }
}
