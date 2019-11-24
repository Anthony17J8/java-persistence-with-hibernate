package com.ico.ltd.hibernateinaction2nd.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@org.hibernate.annotations.Immutable
public class Bid {

    @Id
    @GeneratedValue(generator = "ID_GENERATOR")
    protected Long id;

    @ManyToOne
    protected Item item;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Long getId() {
        return id;
    }
}
