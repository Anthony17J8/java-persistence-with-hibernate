package com.ico.ltd.hibernateinaction2nd.domain.onetomany;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Uni-directional One to many associations in an embeddable class with join column
 *
 * <p>
 * << Table >>                              << Table >>
 * USERS                                    SHIPMENT
 * ID << PK >>                              ID << PK >>
 * STREET                                   CREATEDON
 * ZIPCODE                                  DELIVERY_ADDRESS_USER_ID << FK >>
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
