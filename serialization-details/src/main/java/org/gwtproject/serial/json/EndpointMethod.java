package org.gwtproject.serial.json;

import java.util.List;

public class EndpointMethod {
    private String name;
    private List<EndpointMethodParameter> parameters;
    private EndpointMethodCallback callback;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<EndpointMethodParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<EndpointMethodParameter> parameters) {
        this.parameters = parameters;
    }

    public EndpointMethodCallback getCallback() {
        return callback;
    }

    public void setCallback(EndpointMethodCallback callback) {
        this.callback = callback;
    }
}
