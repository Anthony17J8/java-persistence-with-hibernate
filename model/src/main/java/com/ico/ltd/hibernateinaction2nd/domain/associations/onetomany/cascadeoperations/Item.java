package com.ico.ltd.hibernateinaction2nd.domain.associations.onetomany.cascadeoperations;


import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Item {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    protected String name;


    // The orphanRemoval=true argument tells Hibernate that you want to permanently
    // remove a Bid when it’s removed from the collection.
    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.PERSIST,
            orphanRemoval = true // this includes CascadeType.REMOVE
    )
    protected Set<Bid> bids = new HashSet<>();

    public Item() {
    }

    public Item(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Bid> getBids() {
        return bids;
    }

    public void setBids(Set<Bid> bids) {
        this.bids = bids;
    }
}
