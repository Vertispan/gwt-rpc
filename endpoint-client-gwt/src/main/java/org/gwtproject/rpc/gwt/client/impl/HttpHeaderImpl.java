package org.gwtproject.rpc.gwt.client.impl;

import org.gwtproject.rpc.gwt.client.HttpHeader;
/**
 * A simple static value implementation of an HttpHeader.
 *
 */
public class HttpHeaderImpl extends HttpHeader {
    private String value;

    public HttpHeaderImpl(String name, String value) {
        super(name);
        this.value = value;
    }
    
    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "HttpHeaderImpl [value=" + value + "]";
    }

}
