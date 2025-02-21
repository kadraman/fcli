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
package com.fortify.cli.common.action.runner.processor.writer.record;

import java.util.function.BiFunction;

import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterStyles.RecordWriterStyle;
import com.fortify.cli.common.action.runner.processor.writer.record.impl.RecordWriterCsv;
import com.fortify.cli.common.action.runner.processor.writer.record.impl.RecordWriterJson;
import com.fortify.cli.common.output.OutputFormat.OutputStructure;
import com.fortify.cli.common.output.writer.record.expr.ExprRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.json_properties.JsonPropertiesRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.table.TableRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.table.TableRecordWriter.TableType;
import com.fortify.cli.common.output.writer.record.tree.TreeRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.xml.XmlRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.yaml.YamlRecordWriterFactory;

import lombok.RequiredArgsConstructor;

// Eventually, this would replace and moved to the output.writer.record package, using a single record writers
// implementation for both actions and general fcli output framework.
@RequiredArgsConstructor 
public enum RecordWriterFactory {
    csv(RecordWriterCsv::new, RecordWriterStyles.apply(RecordWriterStyle.FLATTEN, RecordWriterStyle.SHOW_HEADERS)),
    csv_plain(RecordWriterCsv::new, RecordWriterStyles.apply(RecordWriterStyle.FLATTEN)),
    json(RecordWriterJson::new, RecordWriterStyles.none());
    // The below are not yet implemented
    // json_flat(RecordWriterJson::new, RecordWriterStyles.apply(RecordWriterStyle.FLATTEN))
    // table(RecordWriterTable::new, RecordWriterStyles.apply(RecordWriterStyle.FLATTEN, RecordWriterStyle.SHOW_HEADERS)) 
    // table_plain(RecordWriterTable::new, RecordWriterStyles.apply(RecordWriterStyle.FLATTEN))
    // tree(RecordWriterTree::new, RecordWriterStyles.none()) 
    // tree_flat(RecordWriterTree::new, RecordWriterStyles.apply(RecordWriterStyle.FLATTEN))
    // xml(RecordWriterXml::new, RecordWriterStyles.none()) 
    // xml_flat(RecordWriterXml::new, RecordWriterStyles.apply(RecordWriterStyle.FLATTEN))
    // yaml(RecordWriterYaml::new, RecordWriterStyles.none())
    // yaml_flat(RecordWriterYaml::new, RecordWriterStyles.apply(RecordWriterStyle.FLATTEN))
    // expr(RecordWriterExpr::new, RecordWriterStyles.none())
    // json_properties(RecordWriterJsonProperties::new, RecordWriterStyles.none())

    private final BiFunction<RecordWriterStyles,RecordWriterConfig,IRecordWriter> factory;
    private final RecordWriterStyles styles;
    public IRecordWriter createWriter(RecordWriterConfig config) {
        return factory.apply(styles, config);
    }
    
    @Override
    public String toString() {
        return name().replace('_', '-');
    }
}