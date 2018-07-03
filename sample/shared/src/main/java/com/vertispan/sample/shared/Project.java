package com.vertispan.sample.shared;

import java.util.Set;

public class Project extends LineItem {

    private double scopeInHours;
    private Set<Person> assignees;

    public double getScopeInHours() {
        return scopeInHours;
    }

    public void setScopeInHours(double scopeInHours) {
        this.scopeInHours = scopeInHours;
    }

    public Set<Person> getAssignees() {
        return assignees;
    }

    public void setAssignees(Set<Person> assignees) {
        this.assignees = assignees;
    }
}
