package com.ico.ltd.hibernateinaction2nd.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
// disable generation of INSERT and UPDATE SQL statements on startup
@org.hibernate.annotations.DynamicInsert
@org.hibernate.annotations.DynamicUpdate
public class Item {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    @NotNull
    @Size(
            min = 2,
            max = 255,
            message = "Name is required, maximum 255 characters."
    )
    protected String name;

    @NotNull
    protected String description;

    @Future
    protected Date auctionEnd;

    @Column(name = "START_PRICE", nullable = false)
    protected BigDecimal initialPrice;

    @OneToMany(mappedBy = "item")
    protected Set<Bid> bids = new HashSet<>();

    // executes only during SELECTs
    @org.hibernate.annotations.Formula(value = "substr(DESCRIPTION, 1, 12) || '...'")
    protected String shortDescription;

    @org.hibernate.annotations.Formula(value = "(select avg(b.AMOUNT) from BID b where b.ITEM_ID = ID)")
    protected BigDecimal averageBidAmount;

    // in Kilos, DB in Pounds
    @Column(name = "IMPERIALWEIGHT")
    @org.hibernate.annotations.ColumnTransformer(
            read = "IMPERIALWEIGHT / 2.20462",
            write = "? * 2.20462"
    )
    protected double metricWeight;

    public Item() {
    }

    public Item(String name) {
        this.name = name;
    }

    public Item(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = !name.startsWith("AUCTION ") ? "AUCTION " + name : name;
    }

    public Date getAuctionEnd() {
        return auctionEnd;
    }

    public void setAuctionEnd(Date auctionEnd) {
        this.auctionEnd = auctionEnd;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public BigDecimal getAverageBidAmount() {
        return averageBidAmount;
    }

    public void setAverageBidAmount(BigDecimal averageBidAmount) {
        this.averageBidAmount = averageBidAmount;
    }

    public double getMetricWeight() {
        return metricWeight;
    }

    public void setMetricWeight(double metriceWeight) {
        this.metricWeight = metriceWeight;
    }

    public BigDecimal getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(BigDecimal initialPrice) {
        this.initialPrice = initialPrice;
    }
}
