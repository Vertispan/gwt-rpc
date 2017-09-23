package com.vertispan.sample.shared;

import java.io.Serializable;
import java.util.ArrayList;

public class Invoice implements Serializable {
    private ArrayList<LineItem> lineItems;

    private Company client;

    public Company getClient() {
        return client;
    }

    public void setClient(Company client) {
        this.client = client;
    }

    public ArrayList<LineItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(ArrayList<LineItem> lineItems) {
        this.lineItems = lineItems;
    }
}
