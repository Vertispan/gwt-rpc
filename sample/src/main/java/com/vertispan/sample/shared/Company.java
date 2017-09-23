package com.vertispan.sample.shared;

import java.io.Serializable;

public class Company implements HasAddress, Serializable {

    private Address address;

    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public void setAddress(Address address) {
        this.address = address;
    }
}
