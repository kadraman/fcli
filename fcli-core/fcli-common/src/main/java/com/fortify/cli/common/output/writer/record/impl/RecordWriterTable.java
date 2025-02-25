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
package com.fortify.cli.common.output.writer.record.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.output.writer.record.impl.RecordWriterTable.TableWriter;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordWriterTable extends AbstractRecordWriter<TableWriter> {
    @Getter private final RecordWriterConfig config;
    
    @Override
    protected void append(TableWriter out, ObjectNode formattedRecord) throws IOException {
        out.append(formattedRecord);
    }
    
    @Override
    protected Function<ObjectNode, ObjectNode> createRecordFormatter(ObjectNode objectNode) throws IOException {
        // For tables, we always flatten, keeping the original dot-separated property path as headers
        return createStructuredOutputTransformer(true, Function.identity());
    }   
    
    @Override
    protected void close(TableWriter out) throws IOException {
        out.close();
    }
    
    @Override
    protected void closeWithNoData(Writer writer) throws IOException {
        writer.write("No data");
    }
    
    @Override
    protected TableWriter createOut(Writer writer, ObjectNode formattedRecord) throws IOException {
        if ( formattedRecord==null ) { return null; }
        return new TableWriter(writer, formattedRecord.properties().stream().map(e->e.getKey()).toList());
    }
    
    @RequiredArgsConstructor
    protected final class TableWriter implements Closeable { 
        private final Writer writer;
        private final List<String> headers; 
        private final List<String[]> rows = new ArrayList<>();
        
        public void append(ObjectNode formattedRecord) {
            rows.add(asColumnArray(formattedRecord));
        }
        
        private final String[] asColumnArray(ObjectNode formattedRecord) {
            return headers.stream().map(h->getColumnValue(formattedRecord, h)).toArray(String[]::new);
        }

        private final String getColumnValue(ObjectNode formattedRecord, String property) {
            var node = formattedRecord.get(property);
            return node==null || node.isNull() ? "N/A" : node.asText();
        }

        @Override
        public void close() throws IOException {
            var table = asTable();
            writer.write(table);
            writer.flush();
            writer.close();
        }
        
        private final String asTable() {
            if ( rows.isEmpty() ) {
                return "No data"; // TODO This shouldn't happen, as we're only called if at least one row has been appended
            } else {
                Column[] columns = headers.stream()
                    .map(h->new Column()
                            .dataAlign(HorizontalAlign.LEFT)
                            .headerAlign(HorizontalAlign.LEFT)
                            .header(config.getStyle().withHeaders() ? h : null))
                    .toArray(Column[]::new);
                return AsciiTable.getTable(AsciiTable.NO_BORDERS, columns, rows.toArray(String[][]::new)); 
            }
        }
    }
}
