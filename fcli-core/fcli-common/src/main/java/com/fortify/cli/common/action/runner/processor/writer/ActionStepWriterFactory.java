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
import java.util.function.Function;

import com.fortify.cli.common.action.model.ActionStepWithWriter;
import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.processor.writer.record.IRecordWriter;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterCSV;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterConfig;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ActionStepWriterFactory {
    public static final IRecordWriter createWriter(ActionRunnerContext ctx, ActionRunnerVars vars, ActionStepWithWriter withWriter) {
        var type = vars.eval(withWriter.getType(), String.class);
        var config = ActionStepWriterConfigFactory.createRecordWriterConfig(ctx, vars, withWriter);
        return Arrays.stream(ActionStepWriterFactories.values())
                .filter(e->e.name().equals(type.toUpperCase()))
                .findFirst()
                .map(e->e.createWriter(config))
                .orElseThrow(()->new FcliActionValidationException("Unknown writer type: "+type));
    }
    
    @RequiredArgsConstructor
    private static enum ActionStepWriterFactories {
        CSV(c->new RecordWriterCSV(c));
    
        private final Function<RecordWriterConfig, IRecordWriter> factory;
        private IRecordWriter createWriter(RecordWriterConfig config) {
            return factory.apply(config);
        }
    }
}
