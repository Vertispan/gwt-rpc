package org.gwtproject.serial.json;

import org.dominokit.jacksonapt.ObjectMapper;
import org.dominokit.jacksonapt.annotation.JSONMapper;

import java.util.List;

public class EndpointInterface {

    @JSONMapper
    public interface EndpointInterfacesMapper extends ObjectMapper<EndpointInterface> {
    }
    public static final EndpointInterfacesMapper INSTANCE = new EndpointInterface_EndpointInterfacesMapperImpl();

    private String endpointPackage;
    private String endpointInterface;
    private String endpointRemoteInterface;

    private List<EndpointMethod> methods;

    public String getEndpointPackage() {
        return endpointPackage;
    }

    public void setEndpointPackage(String endpointPackage) {
        this.endpointPackage = endpointPackage;
    }

    public String getEndpointInterface() {
        return endpointInterface;
    }

    public void setEndpointInterface(String endpointInterface) {
        this.endpointInterface = endpointInterface;
    }

    public String getEndpointRemoteInterface() {
        return endpointRemoteInterface;
    }

    public void setEndpointRemoteInterface(String endpointRemoteInterface) {
        this.endpointRemoteInterface = endpointRemoteInterface;
    }

    public List<EndpointMethod> getMethods() {
        return methods;
    }

    public void setMethods(List<EndpointMethod> methods) {
        this.methods = methods;
    }
}
