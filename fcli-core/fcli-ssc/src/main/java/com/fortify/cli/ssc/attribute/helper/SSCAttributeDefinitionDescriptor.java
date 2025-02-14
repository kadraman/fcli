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
package com.fortify.cli.ssc.attribute.helper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.ssc.attribute.domain.SSCAttributeDefinitionType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper=true)
public class SSCAttributeDefinitionDescriptor extends JsonNodeHolder {
    private String id;
    private String guid;
    private String name;
    private String category;
    private SSCAttributeDefinitionType type;
    private boolean required;
    private Map<String, SSCAttributeOptionDefinitionDescriptor> optionsByName = new LinkedHashMap<>();
    private Map<String, SSCAttributeOptionDefinitionDescriptor> optionsByGuid = new LinkedHashMap<>();
    @Accessors(fluent=true) private boolean hasDefault;
    
    @JsonProperty("options")
    public void setOptions(ArrayNode optionsNode) {
        if ( optionsNode!=null ) {
            JsonHelper.stream(optionsNode)
                .map(o->JsonHelper.treeToValue(o, SSCAttributeOptionDefinitionDescriptor.class))
                .forEach(this::addOption);
        }
    }
    
    private void addOption(SSCAttributeOptionDefinitionDescriptor descriptor) {
        optionsByName.put(descriptor.getName(), descriptor);
        optionsByGuid.put(descriptor.getGuid(), descriptor);
    }
    
    public JsonNode getOptionsAsJson() {
        return asJsonNode().get("options");
    }
    
    public String getFullName() {
        return category+":"+name;
    }
    
    public String getTypeName() {
        return type.name();
    }
    
    public void checkIsRequired() {
        if ( !required ) {
            throw new FcliSimpleException("SSC attribute "+name+" must be configured as required attribute");
        }
    }
    
    public void checkType(SSCAttributeDefinitionType requiredType) {
        if ( this.type!=requiredType ) {
            throw new FcliSimpleException("SSC attribute "+name+" must be configured as type "+requiredType.name());
        }
    }
    
    public void checkOptionNames(String... requiredNames) {
        var names = optionsByName.keySet();
        var requiredNamesList = Arrays.asList(requiredNames);
        if ( optionsByName.keySet().size()!=requiredNames.length || !names.containsAll(requiredNamesList) ) {
            throw new FcliSimpleException("SSC attribute "+name+" must be configured to have exactly these options: "+requiredNamesList);
        }                
    }
    
    public void check(boolean required, SSCAttributeDefinitionType requiredType, String... requiredOptionNames) {
        if ( required ) { checkIsRequired(); }
        if ( requiredType!=null ) { checkType(requiredType); }
        if ( requiredOptionNames!=null ) { checkOptionNames(requiredOptionNames); }
    }
}
