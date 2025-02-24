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
package com.fortify.cli.common.action.runner.processor.writer.record.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterConfig;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// TODO Do proper exception handling instead of @SneakyThrows
@RequiredArgsConstructor
public class RecordWriterCsv extends AbstractRecordWriter<CsvGenerator> {
    @Getter private final RecordWriterConfig config;
    
    @Override
    protected void append(CsvGenerator out, ObjectNode formattedRecord) throws IOException {
        out.writeTree(formattedRecord);
    }
    
    @Override
    protected Function<ObjectNode, ObjectNode> createRecordFormatter(ObjectNode objectNode) throws IOException {
        // For CSV, we always flatten the output
        return createObjectNodePropertiesTransformer().andThen(createFlattenTransformer());
    }   
    
    @Override
    protected void close(CsvGenerator out) throws IOException {
        if ( config.getStyles().isArray() ) { out.writeEndArray(); }
        out.close();
    }
    
    @Override
    protected CsvGenerator createOut(Writer writer, ObjectNode formattedRecord) throws IOException {
        if ( formattedRecord==null ) { return null; }
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        formattedRecord.fieldNames().forEachRemaining(schemaBuilder::addColumn);
        CsvSchema schema = schemaBuilder.build().withUseHeader(config.getStyles().withHeaders());
        var result = (CsvGenerator)CsvFactory.builder().
                build().createGenerator(writer)
                .setCodec(new ObjectMapper())
                .enable(Feature.IGNORE_UNKNOWN);
        result.setSchema(schema);
        if ( config.getStyles().isArray() ) {
            result.writeStartArray();
        }
        return result;
    }
}
