package com.ico.ltd.hibernateinaction2nd.domain.onetoone.associations;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.validation.constraints.NotNull;

@Entity
public class Address {

    @Id
    @GeneratedValue(generator = "addressKeyGenerator")
    @org.hibernate.annotations.GenericGenerator( // defines a primary key value generator
            name = "addressKeyGenerator",
            strategy = "foreign",
            parameters =
            @org.hibernate.annotations.Parameter(
                    name = "property", value = "user"
            )
    )
    protected Long id;

    @NotNull
    protected String street;

    @NotNull
    protected String zipcode;

    @NotNull
    protected String city;

    @OneToOne(optional = false) // creates foreign key constraint (possible lazy loading)
    @PrimaryKeyJoinColumn // Address must have a reference to a User
    protected User user;

    protected Address() {
    }

    public Address(User user) {
        this.user = user;
    }

    //public constructors of Address now require a User instance.
    public Address(User user, String street, String zipcode, String city) {
        this.user = user;
        this.street = street;
        this.zipcode = zipcode;
        this.city = city;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
