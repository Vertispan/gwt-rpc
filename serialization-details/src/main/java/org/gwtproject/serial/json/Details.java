package org.gwtproject.serial.json;

import org.dominokit.jacksonapt.ObjectMapper;
import org.dominokit.jacksonapt.annotation.JSONMapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class Details {
    @JSONMapper
    public static interface TypeSerializationDetailsMapper extends ObjectMapper<Details> {
    }
    public static final TypeSerializationDetailsMapper INSTANCE = new Details_TypeSerializationDetailsMapperImpl();

    private String serializerPackage;
    private String serializerInterface;

    private Map<String, Type> serializableTypes;

    private String serializerHash;

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

    public String getSerializerHash() {
        return serializerHash;
    }

    public void setSerializerHash(String serializerHash) {
        this.serializerHash = serializerHash;
    }

    public void computeHash() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Can't use SHA-1?", e);
        }

        Charset charset = Charset.forName("UTF-8");

        for (Map.Entry<String, Type> stringTypeEntry : serializableTypes.entrySet()) {
            digest.update(stringTypeEntry.getKey().getBytes(charset));

            Type value = stringTypeEntry.getValue();

            digest.update(value.getName().getBytes(charset));

            digest.update((byte)value.getKind().ordinal());
            switch (value.getKind()) {
                case COMPOSITE:
                    if (value.getCustomFieldSerializer() != null) {
                        digest.update(value.getCustomFieldSerializer().getBytes(charset));
                    }
                    if (value.getSuperTypeId() != null) {
                        digest.update(value.getSuperTypeId().getBytes(charset));
                    }

                    for (String interfaceTypeId : value.getInterfaceTypeIds()) {
                        digest.update(interfaceTypeId.getBytes(charset));
                    }

                    for (Property property : value.getProperties()) {
                        digest.update(property.getName().getBytes(charset));
                        digest.update(property.getTypeId().getBytes(charset));
                    }
                    break;
                case ARRAY:
                    digest.update(value.getComponentTypeId().getBytes(charset));
                    break;
                case ENUM:
                    for (String enumValue : value.getEnumValues()) {
                        digest.update(enumValue.getBytes(charset));
                    }
                    break;
            }
        }

        setSerializerHash(new BigInteger(digest.digest()).toString(16));
    }
}
