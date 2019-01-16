package com.example.rpcsample.shared;

import java.io.Serializable;

public class LineItem implements Serializable {
    private double price;//yes yes i know.
    private String name;

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
