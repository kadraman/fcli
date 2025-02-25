/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.common.action.runner.processor.writer.record.impl;

import java.io.IOException;
import java.io.Writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterConfig;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordWriterYaml extends AbstractRecordWriterJackson<YAMLGenerator> {
    @Getter private final RecordWriterConfig config;
    
    @Override
    protected YAMLGenerator createGenerator(Writer writer, ObjectNode formattedRecord) throws IOException {
        YAMLFactory factory = new YAMLFactory();
        var result = (YAMLGenerator)factory.createGenerator(writer);
        result.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
                .setCodec(new ObjectMapper());
        if ( config.getStyles().isPretty() ) {
            result = result.useDefaultPrettyPrinter();
        }
        return result;
    }
    
    @Override
    protected void writeStart(YAMLGenerator out) throws IOException {
        if ( config.getStyles().isArray() ) { out.writeStartArray(); }
    }
    
    @Override
    protected void writeEnd(YAMLGenerator out) throws IOException {
        if ( getConfig().getStyles().isArray() ) { out.writeEndArray(); }
    }
}
