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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fortify.cli.common.action.model.ActionStepWithWriter;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.FcliActionStepException;
import com.fortify.cli.common.action.runner.processor.writer.record.IRecordWriter;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ActionStepWriterFactory {
    public static final IRecordWriter createWriter(ActionRunnerContext ctx, ActionRunnerVars vars, ActionStepWithWriter withWriter) {
        var type = vars.eval(withWriter.getType(), String.class);
        var to = vars.eval(withWriter.getTo(), String.class);
        return createStandardWriter(ctx, vars, withWriter, type, to);
    }

    private static final IRecordWriter createStandardWriter(ActionRunnerContext ctx, ActionRunnerVars vars,
            ActionStepWithWriter withWriter, String type, String to) {
        Map<String,String> options = withWriter.getOptions()==null ? null : withWriter.getOptions().entrySet().stream()
                .collect(HashMap::new, (map,e)->map.put(e.getKey(), vars.eval(e.getValue(), String.class)), HashMap::putAll);
        var config = ActionStepWriterConfigFactory.createRecordWriterConfig(ctx, vars, to, options);
        return Arrays.stream(RecordWriterFactory.values())
                .filter(e->e.toString().equalsIgnoreCase(type))
                .findFirst()
                .map(e->e.createWriter(config))
                .orElseThrow(()->new FcliActionStepException("Unknown writer type: "+type));
    }
}
