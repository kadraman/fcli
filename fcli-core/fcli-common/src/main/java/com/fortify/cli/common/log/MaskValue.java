package com.fortify.cli.common.log;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.fortify.cli.common.log.LogMaskHelper.LogSensitivityLevel;

@Retention(RUNTIME)
@Target(FIELD)
public @interface MaskValue {
    public LogSensitivityLevel sensitivity() default LogSensitivityLevel.high;
    public String description() default "DATA";
    public String pattern() default "";
}
