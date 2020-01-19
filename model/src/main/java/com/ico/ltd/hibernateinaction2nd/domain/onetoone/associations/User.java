package com.ico.ltd.hibernateinaction2nd.domain.onetoone.associations;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Uni-directional associations with foreign key join column
 * <p>
 * << Table >>                                          << Table >>
 * USERS                                                ADDRESS
 * ID << PK >>                                          ID << PK >>
 * SHIPPINGADDRESS_ID << FK >> << UNIQUE >>             STREET
 * USERNAME                                             ZIPCODE
 * FIRSTNAME                                            CITY
 * LASTNAME
 */
@Entity
@Table(name = "USERS")
public class User {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    protected String username;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false,  // must have shippingAddress
            cascade = CascadeType.PERSIST
    )
    @JoinColumn(unique = true) // defaults to SHIPPINGADDRESS_ID
    protected Address shippingAddress;

    public User() {
    }

    public User(String username) {
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
