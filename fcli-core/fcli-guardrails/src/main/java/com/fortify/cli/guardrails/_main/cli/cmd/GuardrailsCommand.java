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

import static com.fortify.cli.common.cli.util.FcliModuleCategories.UTIL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliModuleCategory;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.EnvHelper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@FcliModuleCategory(UTIL)
@Command(name = "guardrails", resourceBundle = "com.fortify.cli.guardrails.i18n.GuardrailsMessages")
public class GuardrailsCommand extends AbstractRunnableCommand {
    private static final String DEFAULT_SCAN_ENGINE = "fortify-local-scan-engine";

    @Option(names = { "-r", "--rulepack" }, required = true, descriptionKey = "fcli.guardrails.rulepack")
    private Path rulepackPath;

    @Option(names = { "-s", "--source" }, required = true, descriptionKey = "fcli.guardrails.source")
    private Path sourcePath;

    @Option(names = { "-o", "--output" }, defaultValue = "guardrails.fpr", descriptionKey = "fcli.guardrails.output")
    private Path outputPath;

    @Option(names = { "-d", "--workdir" }, defaultValue = "${sys:user.dir}", descriptionKey = "fcli.guardrails.workdir")
    private Path workDir;

    @Option(names = { "--scan-engine" }, descriptionKey = "fcli.guardrails.scan-engine")
    private Path scanEnginePath;

    @Option(names = { "--scan-engine-args" }, split = ",", descriptionKey = "fcli.guardrails.scan-engine-args")
    private List<String> scanEngineArgs = List.of();

    @Override
    public Integer call() throws Exception {
        var normalizedWorkDir = normalizePath(workDir, Path.of(System.getProperty("user.dir")));
        validateDirectory(normalizedWorkDir, "--workdir");

        var normalizedSourcePath = normalizePath(sourcePath, normalizedWorkDir);
        if (!Files.exists(normalizedSourcePath)) {
            throw new FcliSimpleException("Source path doesn't exist: " + normalizedSourcePath);
        }

        var normalizedRulepackPath = normalizePath(rulepackPath, normalizedWorkDir);
        if (!Files.exists(normalizedRulepackPath)) {
            throw new FcliSimpleException("Rulepack path doesn't exist: " + normalizedRulepackPath);
        }

        var normalizedOutputPath = normalizePath(outputPath, normalizedWorkDir);
        var outputParent = normalizedOutputPath.getParent();
        if (outputParent != null) {
            Files.createDirectories(outputParent);
        }

        var scanEngineExecutable = resolveScanEngineExecutable();
        var scanExitCode = executeCommand(buildScanCommand(
                scanEngineExecutable,
                normalizedRulepackPath,
                normalizedSourcePath,
                normalizedOutputPath), normalizedWorkDir);
        if (scanExitCode == 0) {
            if (!Files.exists(normalizedOutputPath)) {
                throw new FcliSimpleException("Guardrails scan completed without generating expected output file: " + normalizedOutputPath);
            }
            System.out.printf("Guardrails scan completed, report written to %s%n", normalizedOutputPath);
        }
        return scanExitCode;
    }

    static List<String> buildRulepackArgs(Path rulepackPath) {
        return List.of("--rulepack", rulepackPath.toString());
    }

    List<String> buildScanCommand(
            String scanEngineExecutable,
            Path normalizedRulepackPath,
            Path normalizedSourcePath,
            Path normalizedOutputPath) {
        var command = new ArrayList<String>();
        command.add(scanEngineExecutable);
        command.addAll(buildRulepackArgs(normalizedRulepackPath));
        command.add("--source");
        command.add(normalizedSourcePath.toString());
        command.add("--output");
        command.add(normalizedOutputPath.toString());
        command.addAll(scanEngineArgs);
        return command;
    }

    private int executeCommand(List<String> command, Path normalizedWorkDir) throws IOException, InterruptedException {
        var process = new ProcessBuilder(command)
                .directory(normalizedWorkDir.toFile())
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        process.waitFor();
        return process.exitValue();
    }

    private String resolveScanEngineExecutable() {
        if (scanEnginePath != null) {
            return scanEnginePath.toString();
        }
        return EnvHelper.envOrDefault("FORTIFY_LOCAL_SCAN_ENGINE", DEFAULT_SCAN_ENGINE);
    }

    private static Path normalizePath(Path path, Path basePath) {
        return path.isAbsolute() ? path.normalize() : basePath.resolve(path).normalize();
    }

    private static void validateDirectory(Path path, String optionName) {
        if (!Files.exists(path)) {
            throw new FcliSimpleException(optionName + " path doesn't exist: " + path);
        }
        if (!Files.isDirectory(path)) {
            throw new FcliSimpleException(optionName + " path is not a directory: " + path);
        }
    }
}
