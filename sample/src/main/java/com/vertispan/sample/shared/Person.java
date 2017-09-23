package com.vertispan.sample.shared;

import java.util.Date;

public class Person implements HasAddress {
    private String name;
    private Date birthday;
    private Address address;

    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public void setAddress(Address address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }
}
