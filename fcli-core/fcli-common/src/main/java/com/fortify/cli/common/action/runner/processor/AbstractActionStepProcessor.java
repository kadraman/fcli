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
package com.fortify.cli.common.action.runner.processor;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.AbstractActionElementForEachRecord;
import com.fortify.cli.common.action.model.ActionConfig.ActionConfigOutput;
import com.fortify.cli.common.action.model.ActionStep;
import com.fortify.cli.common.action.model.IActionStepIfSupplier;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.util.StringUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @Data @Reflectable
public abstract class AbstractActionStepProcessor implements IActionStepProcessor {
    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
    
    protected final String asString(Object o) {
        if ( o instanceof TextNode ) {
            return ((TextNode)o).asText();
        } else if ( o instanceof JsonNode ) {
            return ((JsonNode)o).toPrettyString();
        } else {
            return o.toString();
        }
    }  
    
    protected final void processSteps(ArrayList<ActionStep> steps) {
        new ActionStepsProcessor(getCtx(), getVars(), steps).process();
    }
    
    protected boolean processForEachStepNode(AbstractActionElementForEachRecord forEachRecord, JsonNode node) {
        if ( forEachRecord==null ) { return false; }
        var breakIf = forEachRecord.getBreakIf();
        getVars().set(forEachRecord.getVarName(), node);
        if ( breakIf!=null && getVars().eval(breakIf, Boolean.class) ) {
            return false;
        }
        if ( _if(forEachRecord) ) {
            processSteps(forEachRecord.get_do());
        }
        return true;
    }
    
    protected final boolean _if(Object o) {
        if (getCtx().isExitRequested() ) {
            LOG.debug("SKIPPED due to exit requested:\n"+getEntryAsString(o));
            return false; 
        }
        if ( o==null ) { return true; } // TODO Was false
        if (o instanceof Map.Entry<?,?>) {
            var e = (Map.Entry<?,?>)o;
            return _if(e.getKey()) && _if(e.getValue());
        }
        if (o instanceof IActionStepIfSupplier ) {
            var _if = ((IActionStepIfSupplier) o).get_if();
            if ( _if!=null ) {
                var result = getVars().eval(_if, Boolean.class);
                if ( !result ) { LOG.debug("SKIPPED due to 'if' evaluating to false:\n"+getEntryAsString(o)); }
                return result;
            }
        }
        return true;
    }
    
    protected final String getEntryAsString(Object value) {
        if ( value==null ) { return null; }
        try {
            return String.format("%s:\n%s", 
                StringUtils.indent(value.getClass().getCanonicalName(), "  "),
                StringUtils.indent(getCtx().getYamlObjectMapper().valueToTree(value).toString(), "    "));
        } catch ( Exception e ) {
            return StringUtils.indent(value.toString(), "  ");
        }
    }
    
    protected final void writeImmediateOrDelayed(PrintStream out, String value) {
        if ( getCtx().getConfig().getAction().getConfig().getOutput()==ActionConfigOutput.delayed ) {
            getCtx().getDelayedConsoleWriterRunnables().add(createRunner(out, value));
        } else {
            out.print(value);
        }
    }
    
    private final Runnable createRunner(PrintStream out, String output) {
        return ()->out.print(output);
    }
    
    public abstract ActionRunnerContext getCtx();
    public abstract ActionRunnerVars getVars();
}
