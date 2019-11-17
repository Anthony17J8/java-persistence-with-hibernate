package com.ico.ltd.hibernateinaction2nd.domain;

import javax.persistence.Entity;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Item {

    protected Set<Bid> bids = new HashSet<>();

    public Set<Bid> getBids() {
        return bids;
    }

    public void setBids(Set<Bid> bids) {
        this.bids = bids;
    }

    public void addBid(Bid bid) {
        if (bid == null) {
            throw new NullPointerException("Can't add null Bid");
        }

        if (bid.getItem() != null) {
            throw new IllegalStateException("Bid is already assigned to an Item");
        }

        getBids().add(bid);
        bid.setItem(this);
    }
}
