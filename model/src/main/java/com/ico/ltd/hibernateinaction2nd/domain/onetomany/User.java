package com.ico.ltd.hibernateinaction2nd.domain.onetomany;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Uni-directional One to many association in an embeddable class with join table
 * <p>
 * << Table >>         << Table >>                              << Table >>
 * USERS               DELIVERIES                               SHIPMENT
 * ID << PK >>         USER_ID << PK >> << FK >>                ID << PK >>
 * STREET              SHIPMENT_ID << PK >> << FK >>            CREATEDON
 * ZIPCODE
 * CITY
 */
@Entity
@Table(name = "USERS")
public class User {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    public Long getId() {
        return id;
    }

    protected String username;

    protected Address shippingAddress;

    public User() {
    }

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}
