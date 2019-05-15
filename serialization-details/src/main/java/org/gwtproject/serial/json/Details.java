package org.gwtproject.serial.json;

import org.dominokit.jacksonapt.ObjectMapper;
import org.dominokit.jacksonapt.annotation.JSONMapper;

import java.util.List;
import java.util.Map;

public class Details {
    @JSONMapper
    public static interface TypeSerializationDetailsMapper extends ObjectMapper<Details> {
    }
    public static final TypeSerializationDetailsMapper INSTANCE = new Details_TypeSerializationDetailsMapperImpl();

    private String serializerPackage;
    private String serializerInterface;
    private Map<String, Type> serializableTypes;

    public String getSerializerPackage() {
        return serializerPackage;
    }

    public void setSerializerPackage(String serializerPackage) {
        this.serializerPackage = serializerPackage;
    }

    public String getSerializerInterface() {
        return serializerInterface;
    }

    public void setSerializerInterface(String serializerInterface) {
        this.serializerInterface = serializerInterface;
    }

    public Map<String, Type> getSerializableTypes() {
        return serializableTypes;
    }

    public void setSerializableTypes(Map<String, Type> serializableTypes) {
        this.serializableTypes = serializableTypes;
    }
}
