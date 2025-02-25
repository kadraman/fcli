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

import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.license.msp_report.collector.MspReportAppScanCollector.MspReportProcessedScanDescriptor;

public final class MspReportScansWriter implements IMspReportScansWriter {
    private final IRecordWriter processedArtifactsWriter;
    private final IRecordWriter scansInReportingPeriodWriter;
    private final IRecordWriter scansConsumingEntitlementWriter;
    
    public MspReportScansWriter(IReportWriter reportWriter) {
        this.processedArtifactsWriter = reportWriter.recordWriter(RecordWriterFactory.csv, "details/scans.csv", false, null);
        this.scansInReportingPeriodWriter = reportWriter.recordWriter(RecordWriterFactory.csv, "details/scans-in-reporting-period.csv", false, null);
        this.scansConsumingEntitlementWriter = reportWriter.recordWriter(RecordWriterFactory.csv, "details/scans-consuming-entitlements.csv", false, null);
    }
    
    @Override
    public void writeProcessed(MspReportProcessedScanDescriptor descriptor) {
        processedArtifactsWriter.append(descriptor.getReportNode());
    }
    
    @Override
    public void writeInReportingPeriod(MspReportProcessedScanDescriptor descriptor) {
        scansInReportingPeriodWriter.append(descriptor.getReportNode());
    }
    
    @Override
    public void writeEntitlementConsuming(MspReportProcessedScanDescriptor descriptor) {
        scansConsumingEntitlementWriter.append(descriptor.getReportNode());
    }
}
