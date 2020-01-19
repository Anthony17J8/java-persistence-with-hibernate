package com.ico.ltd.hibernateinaction2nd.domain.onetoone.associations;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
public class Shipment {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    @NotNull
    protected Date createdOn = new Date();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ITEM_SHIPMENT", // required
            joinColumns = @JoinColumn(name = "SHIPMENT_ID"), // defaults to ID
            inverseJoinColumns = @JoinColumn(
                    name = "ITEM_ID",
                    nullable = false,
                    unique = true) // defaults to AUCTION_ID
    )
    protected Item auction;

    public Shipment() {
    }

    public Shipment(Item auction) {
        this.auction = auction;
    }

    public Long getId() {
        return id;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public Item getAuction() {
        return auction;
    }

    public void setAuction(Item auction) {
        this.auction = auction;
    }
}
