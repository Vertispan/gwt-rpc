package com.vertispan.sample.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Invoice implements Serializable {
    private ArrayList<LineItem> lineItems;

    public ArrayList<LineItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(ArrayList<LineItem> lineItems) {
        this.lineItems = lineItems;
    }
}
