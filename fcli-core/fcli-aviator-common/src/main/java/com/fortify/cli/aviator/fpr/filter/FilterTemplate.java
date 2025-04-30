package com.fortify.cli.aviator.fpr.filter;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilterTemplate {
    private String version;
    private boolean disableEdit;
    private String id;
    private int objectVersion;
    private int publishVersion;
    private String name;
    private String description;
    private List<FolderDefinition> folderDefinitions;
    private String defaultFolder;
    private List<TagDefinition> tagDefinitions;
    private PrimaryTag primaryTag;
    private List<FilterSet> filterSets;
}
