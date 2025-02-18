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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableClass;
import com.fortify.cli.common.action.model.ActionStep;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.exception.FcliBugException;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorSteps extends AbstractActionStepProcessorListEntries<ActionStep> {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final List<ActionStep> list;

    // Note that if-handling and logging is handled by AbstractActionStepProcessorListEntries
    protected final void process(ActionStep step) {
        var stepValue = step.getStepValue();
        ActionStepProcessorFactoryHelper.get(stepValue.getKey())
                .create(ctx, vars, stepValue.getValue())
                .process();
    }
    
    private static final class ActionStepProcessorFactoryHelper {
        private static final Map<String, IActionStepProcessorFactory> actionStepProcessorFactories = createActionStepProcessorFactories();
        
        public static final IActionStepProcessorFactory get(String jsonPropertyName) {
            return actionStepProcessorFactories.get(jsonPropertyName);
        }
        
        private static final Map<String, IActionStepProcessorFactory> createActionStepProcessorFactories() {
            return ActionStep.getPropertyTypes().entrySet().stream().collect(
                    HashMap::new, (map,e)->map.put(e.getKey(), createStepProcessorFactory(e)), Map::putAll);
        }
        
        private static final IActionStepProcessorFactory createStepProcessorFactory(Entry<String, Class<?>> propertyNameAndType) {
            var processorClazz = getProcessorClazz(propertyNameAndType.getKey());
            var valueType = propertyNameAndType.getValue();
            try {
                return (IActionStepProcessorFactory)MethodHandleProxies.asInterfaceInstance(
                    IActionStepProcessorFactory.class,
                    findConstructor(processorClazz, valueType));
            } catch (IllegalAccessException e) {
                throw new FcliBugException("Can't instantiate step processor", e);
            }
        }

        private static MethodHandle findConstructor(Class<? extends IActionStepProcessor> processorClazz, Class<?> valueType)
                throws IllegalAccessException {
            var currentType = valueType;
            while ( currentType!=null ) {
                try {
                    return MethodHandles.lookup().findConstructor(processorClazz,
                        MethodType.methodType(Void.TYPE, ActionRunnerContext.class, ActionRunnerVars.class, currentType));
                } catch (NoSuchMethodException e) {
                    currentType = currentType.getSuperclass();
                }
            }
            throw new FcliBugException(String.format("Step processor %s doesn't provide required constructor(ActionRunnercontext, ActionRunnerVars, %s)", processorClazz.getSimpleName(), valueType.getSimpleName()));
        }
    
        @SuppressWarnings("unchecked")
        private static final Class<? extends IActionStepProcessor> getProcessorClazz(String jsonPropertyName) {
            var clazzSuffix = getProcessorClazzSuffix(jsonPropertyName);
            String processorClazzName = String.format("%s.ActionStepProcessor%s", ActionStepProcessorSteps.class.getPackageName(), clazzSuffix);
            try {
                return (Class<? extends IActionStepProcessor>)Class.forName(processorClazzName);
            } catch (ClassNotFoundException e) {
                throw new FcliBugException("Can't find class "+processorClazzName);
            }
        }
            
        private static final String getProcessorClazzSuffix(String jsonPropertyName) {
            var elts = jsonPropertyName.split("\\W+");
            return Arrays.stream(elts).map(StringUtils::capitalize).collect(Collectors.joining());
        }
    }
    
    @FunctionalInterface // @Reflectable doesnt't work on interfaces
    @ReflectableClass(className=IActionStepProcessorFactory.class)
    public static interface IActionStepProcessorFactory {
        IActionStepProcessor create(ActionRunnerContext ctx, ActionRunnerVars vars, Object value);
    }
}
