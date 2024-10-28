package org.gwtproject.rpc.gwt.client;

/**
 * Represents an HttpHeader. Hashcode and equals are based on the name of the header.
 * 
 * A subclass can override getValue making it possible to resolve the value based on a function this deferring
 * value resolution to the moment it is required.
 *
 */
public abstract class HttpHeader {
	private String name;
	
	public HttpHeader(String name) {
		super();
		if(name == null) {
		    throw new IllegalArgumentException("Http header name cannot be null");
		}
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public abstract String getValue();

	
	@Override
    public String toString() {
        return "HttpHeader [name=" + name + "]";
    }

    /**
	 * Based on header name
	 */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Two headers are consider equal if their names are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        HttpHeader other = (HttpHeader) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
	
}
