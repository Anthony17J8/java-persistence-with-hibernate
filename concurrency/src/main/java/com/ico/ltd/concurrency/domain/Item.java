package com.ico.ltd.concurrency.domain;

import com.ico.ltd.concurrency.Constants;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Item implements Serializable {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    /**
     * If you don’t want to increment the version of the entity instance when a particular property’s value has changed,
     * annotate the property with @org.hibernate.annotations.OptimisticLock(excluded = true)
     */
    @Version
    protected long version;

    @NotNull
    protected String name;

    protected BigDecimal buyNowPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    protected Category category;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "IMAGE",
            joinColumns = @JoinColumn(name = "ITEM_ID"))
    @Column(name = "FILENAME")
    protected Set<String> images = new HashSet<String>();

    protected Item() {
    }

    public Item(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBuyNowPrice() {
        return buyNowPrice;
    }

    public void setBuyNowPrice(BigDecimal buyNowPrice) {
        this.buyNowPrice = buyNowPrice;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Set<String> getImages() {
        return images;
    }
}
