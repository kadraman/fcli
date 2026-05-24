/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.guardrails._main.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class GuardrailsCommandTest {
    @Test
    void buildRulepackArgsAddsRulepackSwitch() {
        var args = GuardrailsCommand.buildRulepackArgs(Path.of("/tmp/rules.zip"));
        assertEquals(List.of("--rulepack", "/tmp/rules.zip"), args);
    }
}
