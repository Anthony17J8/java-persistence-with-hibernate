package com.ico.ltd.hibernateinaction2nd.domain.onetoone.associations;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Bidirectional entity association with foreign key generator.
 * <p>
 * << Table >>              << Table >>
 * USERS                    ADDRESS
 * ID << PK >>              ID << PK >> << FK >>
 * USERNAME                 STREET
 * FIRSTNAME                ZIPCODE
 * LASTNAME                 CITY
 */
@Entity
@Table(name = "USERS")
public class User {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    protected String username;

    @OneToOne(mappedBy = "user", cascade = CascadeType.PERSIST)
    protected Address shippingAddress;

    public User() {
    }

    public User(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address address) {
        this.shippingAddress = address;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
