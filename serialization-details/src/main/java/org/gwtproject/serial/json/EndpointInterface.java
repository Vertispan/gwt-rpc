package org.gwtproject.serial.json;

import org.dominokit.jacksonapt.ObjectMapper;
import org.dominokit.jacksonapt.annotation.JSONMapper;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class EndpointInterface {

    @JSONMapper
    public interface EndpointInterfacesMapper extends ObjectMapper<EndpointInterface> {
    }
    public static final EndpointInterfacesMapper INSTANCE = new EndpointInterface_EndpointInterfacesMapperImpl();

    private String endpointPackage;
    private String endpointInterface;
    private String endpointRemoteInterface;

    private List<EndpointMethod> methods;

    private String endpointHash;

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

    public String getEndpointHash() {
        return endpointHash;
    }

    public void setEndpointHash(String endpointHash) {
        this.endpointHash = endpointHash;
    }

    public void computeHash(EndpointInterface remote) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Can't use SHA-1?", e);
        }

        Charset charset = Charset.forName("UTF-8");

        Stream.of(this, remote)
                .sorted(Comparator.comparing(EndpointInterface::getEndpointInterface))
                .forEach(e -> {
                    for (EndpointMethod method : e.methods) {
                        digest.update(method.getName().getBytes(charset));
                        for (EndpointMethodParameter parameter : method.getParameters()) {
                            digest.update(parameter.getName().getBytes(charset));
                            digest.update(parameter.getTypeId().getBytes(charset));
                        }
                        if (method.getCallback() != null) {
                            digest.update(method.getCallback().getSuccessTypeId().getBytes(charset));
                            digest.update(method.getCallback().getFailureTypeId().getBytes(charset));
                        }
                    }
                });

        setEndpointHash(new BigInteger(digest.digest()).toString(16));
    }
}
