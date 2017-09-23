package com.vertispan.sample.shared;

import java.util.HashSet;

public class Project extends LineItem {

    private double scopeInHours;
    private HashSet<Person> assignees;

    public double getScopeInHours() {
        return scopeInHours;
    }

    public void setScopeInHours(double scopeInHours) {
        this.scopeInHours = scopeInHours;
    }

    public HashSet<Person> getAssignees() {
        return assignees;
    }

    public void setAssignees(HashSet<Person> assignees) {
        this.assignees = assignees;
    }
}
