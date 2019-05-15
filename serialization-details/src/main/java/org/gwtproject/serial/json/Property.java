package org.gwtproject.serial.json;

public class Property {
    private String name;
    private String typeId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    @Override
    public String toString() {
        return "Property{" +
                "name='" + name + '\'' +
                ", typeId='" + typeId + '\'' +
                '}';
    }
}
