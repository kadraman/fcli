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
package com.fortify.cli.common.action.runner.processor.writer;

import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.fortify.cli.common.action.model.ActionStepWithWriter;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterConfig;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionStepWriterConfigFactory {
    public static final RecordWriterConfig createRecordWriterConfig(ActionRunnerContext ctx, ActionRunnerVars vars, ActionStepWithWriter withWriter) {
        var to = vars.eval(withWriter.getTo(), String.class);
        Map<String,String> options = withWriter.getOptions()==null ? null : withWriter.getOptions().entrySet().stream()
                .collect(HashMap::new, (map,e)->map.put(e.getKey(), vars.eval(e.getValue(), String.class)), HashMap::putAll);
        return RecordWriterConfig.builder()
                .writerSupplier(()->createFileWriter(to))
                .options(options)
                .build();
    }
    
    @SneakyThrows
    public static final Writer createFileWriter(String target) {
        return new FileWriter(target);
    }
}
