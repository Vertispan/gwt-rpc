package com.vertispan.sample.shared;

import java.io.Serializable;

public class Address implements Serializable {
    String line1;

    String city;
    String region;
    String country;

    String postalCode;

    Address() {
        
    }

    public Address(String line1, String city, String region, String country, String postalCode) {
        this.line1 = line1;
        this.city = city;
        this.region = region;
        this.country = country;
        this.postalCode = postalCode;
    }

    public String getLine1() {
        return line1;
    }

    public String getCity() {
        return city;
    }

    public String getRegion() {
        return region;
    }

    public String getCountry() {
        return country;
    }

    public String getPostalCode() {
        return postalCode;
    }
}
