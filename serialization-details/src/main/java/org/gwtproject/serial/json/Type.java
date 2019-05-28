package org.gwtproject.serial.json;

import java.util.List;

public class Type {
    public enum Kind { COMPOSITE, ARRAY, ENUM }

    private String name;
    private Kind kind;

    private String componentTypeId;

    private String customFieldSerializer;

    private List<String> enumValues;

    private String superTypeId;

    private List<String> interfaceTypeIds;

    private List<Property> properties;

    private boolean canInstantiate;
    private boolean canDeserialize;
    private boolean canSerialize;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getComponentTypeId() {
        return componentTypeId;
    }

    public void setComponentTypeId(String componentTypeId) {
        this.componentTypeId = componentTypeId;
    }

    public String getCustomFieldSerializer() {
        return customFieldSerializer;
    }

    public void setCustomFieldSerializer(String customFieldSerializer) {
        this.customFieldSerializer = customFieldSerializer;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }

    public String getSuperTypeId() {
        return superTypeId;
    }

    public void setSuperTypeId(String superTypeId) {
        this.superTypeId = superTypeId;
    }

    public List<String> getInterfaceTypeIds() {
        return interfaceTypeIds;
    }

    public void setInterfaceTypeIds(List<String> interfaceTypeIds) {
        this.interfaceTypeIds = interfaceTypeIds;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public boolean isCanInstantiate() {
        return canInstantiate;
    }

    public void setCanInstantiate(boolean canInstantiate) {
        this.canInstantiate = canInstantiate;
    }

    public boolean isCanDeserialize() {
        return canDeserialize;
    }

    public void setCanDeserialize(boolean canDeserialize) {
        this.canDeserialize = canDeserialize;
    }

    public boolean isCanSerialize() {
        return canSerialize;
    }

    public void setCanSerialize(boolean canSerialize) {
        this.canSerialize = canSerialize;
    }


    @Override
    public String toString() {
        return "Type{" +
                "name='" + name + '\'' +
                ", kind=" + kind +
                ", componentTypeId='" + componentTypeId + '\'' +
                ", customFieldSerializer='" + customFieldSerializer + '\'' +
                ", enumValues=" + enumValues +
                ", superTypeId='" + superTypeId + '\'' +
                ", properties=" + properties +
                ", canInstantiate=" + canInstantiate +
                ", canDeserialize=" + canDeserialize +
                ", canSerialize=" + canSerialize +
                '}';
    }
}
