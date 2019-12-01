package com.ico.ltd.hibernateinaction2nd.domain;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class Address {

    // Ignored for DDL generation
    @NotNull
    @Column(nullable = false)
    protected String street;

    @NotNull
    @AttributeOverrides(
            @AttributeOverride(
                    name = "name",
                    column = @Column(name = "CITY", nullable = false)
            )
    )
    protected City city;

    protected Address() {
    }

    public Address(String street, City city) {
        this.street = street;
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }
}
