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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.runner.processor.writer.record.IRecordWriter;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterConfig;
import com.fortify.cli.common.output.transform.fields.SelectedFieldsTransformer;
import com.fortify.cli.common.output.transform.flatten.FlattenTransformer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

//TODO Do proper exception handling instead of @SneakyThrows
@RequiredArgsConstructor
public abstract class AbstractRecordWriter<T> implements IRecordWriter {
    @Getter(value = AccessLevel.PRIVATE, lazy=true) private final Writer writer = createWriter();
    private T out;
    private Function<ObjectNode, ObjectNode> recordFormatter;
    
    public abstract RecordWriterConfig getConfig();
    protected abstract Function<ObjectNode, ObjectNode> createRecordFormatter(ObjectNode objectNode) throws IOException;
    protected abstract T createOut(Writer writer, ObjectNode formattedRecord)  throws IOException;
    protected abstract void append(T out, ObjectNode formattedRecord)  throws IOException;
    protected abstract void close(T out)  throws IOException; 
    
    @Override @SneakyThrows
    public final void append(ObjectNode record) {
        var formattedRecord = getRecordFormatter(record).apply(record);
        append(getOut(formattedRecord), formattedRecord);
    }
    
    @Override @SneakyThrows
    public final void close() {
        if ( out!=null) {
            close(out);
        }
    }
    
    protected final Function<ObjectNode, ObjectNode> createFlattenTransformer() {
        return new FlattenTransformer(Function.identity(), ".", false)::transformObjectNode;
    }

    protected final Function<ObjectNode, ObjectNode> createSelectedFieldsTransformer() {
        return new SelectedFieldsTransformer(getConfig().getOptions(), false)::transformObjectNode;
    }
    
    @SneakyThrows
    private final Function<ObjectNode, ObjectNode> getRecordFormatter(ObjectNode record) {
        if ( recordFormatter==null ) {
            recordFormatter = createRecordFormatter(record);
        }
        return recordFormatter;
    }
    
    @SneakyThrows
    private final T getOut(ObjectNode formattedRecord) {
        if ( out==null ) {
            out = createOut(getWriter(), formattedRecord);
        }
        return out;
    }
    
    @SneakyThrows
    private final Writer createWriter() {
        return getConfig().getWriterSupplier().get();
    }
}
