package com.ico.ltd.hibernateinaction2nd.domain.associations.onetoone;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * Unidirectional shared primary key one-to-one association mapping, from User to Address.
 * <p>
 * << Table >>              << Table >>
 * USERS                    ADDRESS
 * ID << PK >> << FK >>     ID << PK >>
 * USERNAME                 STREET
 * FIRSTNAME                ZIPCODE
 * LASTNAME                 CITY
 */
@Entity
@Table(name = "USERS")
public class User {

    @Id
    protected Long id; // uses application-assigned identifier value

    protected String username;

    @OneToOne(
            fetch = FetchType.LAZY, // defaults to EAGER
            optional = false // User must have a shippingAddress
    )
    @PrimaryKeyJoinColumn // selects shared primary key strategy
    protected Address shippingAddress;

    protected User() {
    }

    public User(Long id, String username) { // identifier required
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
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
