/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.tool._common.helper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class represents a single tool installation, containing information about the 
 * installation location. It doesn't include the actual tool name or version, as this 
 * is represented by the directory name (tool name) and file name (version) where the 
 * serialized installation descriptors are stored. 
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@Reflectable @NoArgsConstructor @AllArgsConstructor
@Data
public class ToolInstallationDescriptor {
    private String installDir;
    private String binDir;
    private String globalBinDir;
    
    public ToolInstallationDescriptor(Path installPath, Path binPath, Path globalBinPath) {
        this.installDir = installPath==null ? null : installPath.toAbsolutePath().normalize().toString();
        this.binDir = binPath==null ? null : binPath.toAbsolutePath().normalize().toString();
        this.globalBinDir = globalBinPath==null ? null : globalBinPath.toAbsolutePath().normalize().toString();
    }
    
    public static final ToolInstallationDescriptor load(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        var result = FcliDataHelper.readFile(getInstallDescriptorPath(toolName, versionDescriptor.getVersion()), ToolInstallationDescriptor.class, false);
        // Check for stale descriptor
        if ( result!=null && !Files.exists(result.getInstallPath()) ) {
            delete(toolName, versionDescriptor);
            result = null;
        }
        return result;
    }
    
    public static final ToolInstallationDescriptor loadLastModified(String toolName) {
        var installDescriptorsDir = getInstallDescriptorsDirPath(toolName).toFile();
        ToolInstallationDescriptor result = null;
        while ( result==null ) { // The load method may delete stale descriptors, in which case we need to look for the next one
            Optional<File> lastModifiedFile = Arrays.stream(installDescriptorsDir.listFiles(File::isFile))
                .max((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            result = lastModifiedFile.map(File::toPath).map(ToolInstallationDescriptor::load).orElse(null);
        }
        return result;
    }
    
    public static final void delete(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        delete(getInstallDescriptorPath(toolName, versionDescriptor.getVersion()));
    }
    
    public final void save(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        FcliDataHelper.saveFile(getInstallDescriptorPath(toolName, versionDescriptor.getVersion()), this, true);
    }
    
    public Path getInstallPath() {
        return asPath(installDir);
    }
    
    public Path getBinPath() {
        return asPath(binDir);
    }
    
    public Path getGlobalBinPath() {
        return asPath(binDir);
    }
    
    private static final ToolInstallationDescriptor load(Path descriptorPath) {
        var result = FcliDataHelper.readFile(descriptorPath, ToolInstallationDescriptor.class, false);
        // Check for stale descriptor
        if ( result!=null && !Files.exists(result.getInstallPath()) ) {
            delete(descriptorPath);
            result = null;
        }
        return result;
    }
    
    private static final void delete(Path descriptorPath) {
        FcliDataHelper.deleteFile(descriptorPath, true);
    }
    
    private static final Path asPath(String dir) {
        return StringUtils.isNotBlank(dir) ? Paths.get(dir) : null;
    }
    
    private static final Path getInstallDescriptorPath(String toolName, String version) {
        return getInstallDescriptorsDirPath(toolName).resolve(version);
    }

    private static Path getInstallDescriptorsDirPath(String toolName) {
        return ToolInstallationHelper.getToolsStatePath().resolve(toolName);
    }

}
