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

import java.io.Writer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.runner.processor.writer.record.IRecordWriter;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterConfig;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterStyles;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterStyles.RecordWriterStyle;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class RecordWriterJson implements IRecordWriter {
    private final RecordWriterStyles styles;
    private final RecordWriterConfig config;
    @Getter(value = AccessLevel.PRIVATE, lazy=true) private final Writer writer = createWriter();
    private JsonGenerator generator;
    
    @Override @SneakyThrows
    public void append(ObjectNode record) {
        // TODO Handle flattening
        styles.has(RecordWriterStyle.FLATTEN); // Just remove warning about unused field for now
        getGenerator(record).writeTree(record);
    }
    
    @SneakyThrows
    private JsonGenerator getGenerator(ObjectNode record) {
        if ( generator==null ) {
            PrettyPrinter pp = !config.isPretty() ? null : new DefaultPrettyPrinter(); 
            this.generator = JsonFactory.builder().
                    build().createGenerator(getWriter())
                    .setPrettyPrinter(pp)
                    .setCodec(new ObjectMapper());
            if ( !config.isSingular() ) {
                generator.writeStartArray();
            }
        }
        return generator;
    }

    @Override @SneakyThrows
    public void close() {
        if ( generator!=null) {
            if ( !config.isSingular() ) { generator.writeEndArray(); }
            generator.close();
        }
    }
    
    @SneakyThrows
    private final Writer createWriter() {
        return config.getWriterSupplier().get();
    }
}
