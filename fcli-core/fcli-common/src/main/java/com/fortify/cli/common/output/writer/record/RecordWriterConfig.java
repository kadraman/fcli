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
package com.fortify.cli.common.output.writer.record;

import java.io.Writer;
import java.util.function.Supplier;

import lombok.Builder;
import lombok.Getter;

@Builder
public class RecordWriterConfig {
    @Getter private final Supplier<Writer> writerSupplier;
    @Getter private final RecordWriterStyle style;
    @Getter private final String args;
}
