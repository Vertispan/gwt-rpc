package com.vertispan.sample.shared;

import java.io.Serializable;

public class Company implements HasAddress, Serializable {

    private String name;
    private Person owner;
    private Address address;

    int[] numbers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Person getOwner() {
        return owner;
    }

    public void setOwner(Person owner) {
        this.owner = owner;
    }

    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public void setAddress(Address address) {
        this.address = address;
    }
}
