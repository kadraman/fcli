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
package com.fortify.cli.common.log;

import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableClass;
import com.formkiq.graalvm.annotations.ReflectableClass.ReflectableClasses;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.pattern.FormattingConverter;

/**
 *
 * @author Ruud Senden
 */
@Reflectable
@ReflectableClasses({
    @ReflectableClass(className=ClassicConverter.class), 
    @ReflectableClass(className=DynamicConverter.class),
    @ReflectableClass(className=FormattingConverter.class),
    @ReflectableClass(className=Converter.class)
    })
public abstract class FcliLogClassicConverter extends ClassicConverter {

}
