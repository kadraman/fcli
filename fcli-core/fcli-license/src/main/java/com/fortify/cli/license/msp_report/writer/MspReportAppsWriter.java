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
package com.fortify.cli.license.msp_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCProcessedAppDescriptor;

public final class MspReportAppsWriter implements IMspReportAppsWriter {
    private final IRecordWriter recordWriter;
    
    public MspReportAppsWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(RecordWriterFactory.csv, "details/applications.csv", false, null);
    }
    
    @Override
    public void write(IUrlConfig urlConfig, MspReportSSCProcessedAppDescriptor descriptor) {
        recordWriter.append(
                descriptor.updateReportRecord(
                        JsonHelper.getObjectMapper().createObjectNode()
                        .put("url", urlConfig.getUrl())));
        
    }
}
