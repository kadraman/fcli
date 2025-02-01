/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.progress.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.progress.helper.IProgressWriterFactory;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.progress.helper.ProgressWriterI18n;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class ProgressWriterFactoryMixin implements IProgressWriterFactory {
    @Mixin private CommandHelperMixin commandHelper;
    @Getter @Option(names="--progress", defaultValue = "auto") 
    private ProgressWriterType type;
    
    /** Create a progress writer for the configured type */
    @Override
    public final IProgressWriterI18n create() {
        return create(this.type);
    }

    /** Create a progress writer for the given type, ignoring the configured type */
    @Override
    public final IProgressWriterI18n create(ProgressWriterType progressWriterType) {
        return new ProgressWriterI18n(progressWriterType, commandHelper.getMessageResolver());
    }
}
