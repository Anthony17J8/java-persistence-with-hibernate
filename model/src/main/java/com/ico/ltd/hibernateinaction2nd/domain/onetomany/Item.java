package com.ico.ltd.hibernateinaction2nd.domain.onetomany;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Item {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    protected String name;

    @OneToMany
    @JoinColumn(
            name = "ITEM_ID",
            nullable = false
    )
    @OrderColumn(
            name = "BID_POSITION", // Defaults to BIDS_ORDER
            nullable = false
    )
    public List<Bid> bids = new ArrayList<>();

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

    public List<Bid> getBids() {
        return bids;
    }
}
