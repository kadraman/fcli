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
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterConfig;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordWriterJson extends AbstractRecordWriter<JsonGenerator> {
    @Getter private final RecordWriterConfig config;
    
    @Override
    protected void append(JsonGenerator out, ObjectNode formattedRecord) throws IOException {
        out.writeTree(formattedRecord);
    }
    
    @Override
    protected Function<ObjectNode, ObjectNode> createRecordFormatter(ObjectNode objectNode) throws IOException {
        return createSelectedFieldsTransformer()
                .andThen(config.getStyles().isFlat()?createFlattenTransformer():Function.identity());
    }
    
    @Override
    protected void close(JsonGenerator out) throws IOException {
        if ( config.getStyles().isArray() ) { out.writeEndArray(); }
        out.close();
    }
    
    @Override
    protected JsonGenerator createOut(Writer writer, ObjectNode formattedRecord) throws IOException {
        if ( formattedRecord==null ) { return null; }
        PrettyPrinter pp = !config.getStyles().isPretty() ? null : new DefaultPrettyPrinter(); 
        var result = JsonFactory.builder().
            build().createGenerator(writer)
            .setPrettyPrinter(pp)
            .setCodec(new ObjectMapper());
        if ( config.getStyles().isArray() ) {
            result.writeStartArray();
        }
        return result;
    }
}
