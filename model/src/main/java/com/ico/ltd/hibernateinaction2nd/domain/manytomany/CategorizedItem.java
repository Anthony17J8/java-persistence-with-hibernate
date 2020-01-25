package com.ico.ltd.hibernateinaction2nd.domain.manytomany;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "CATEGORY_ITEM")
@org.hibernate.annotations.Immutable
public class CategorizedItem {

    public static class Id implements Serializable {  // encapsulate composite key

        @Column(name = "CATEGORY_ID")
        protected Long categoryId;

        @Column(name = "ITEM_ID")
        protected Long itemId;

        public Id() {
        }

        public Id(Long categoryId, Long itemId) {
            this.categoryId = categoryId;
            this.itemId = itemId;
        }

        public boolean equals(Object o) {
            if (o != null && o instanceof Id) {
                Id that = (Id) o;
                return this.categoryId.equals(that.categoryId)
                        && this.itemId.equals(that.itemId);
            }
            return false;
        }

        public int hashCode() {
            return categoryId.hashCode() + itemId.hashCode();
        }
    }

    @EmbeddedId
    protected Id id = new Id(); // maps identifier property and composite key columns

    @Column(updatable = false)
    @NotNull
    protected String addedBy; // maps username

    @Column(updatable = false)
    @NotNull
    protected Date addedOn = new Date(); // maps timestamp

    @ManyToOne
    @JoinColumn(
            name = "CATEGORY_ID",
            insertable = false, updatable = false
    )
    protected Category category; // maps category

    @ManyToOne
    @JoinColumn(
            name = "ITEM_ID",
            insertable = false, updatable = false
    )
    protected Item item; // maps item

    public CategorizedItem() {
    }

    public CategorizedItem(String addedByUsername, Category category, Item item) {
        this.addedBy = addedByUsername;
        this.category = category;
        this.item = item;

        this.id.categoryId = category.getId();  // sets identifier values
        this.id.itemId = item.getId();

        category.getCategorizedItems().add(this); // Guarantees referential integrity if made bidirectional
        item.getCategorizedItems().add(this);
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public Date getAddedOn() {
        return addedOn;
    }

    public void setAddedOn(Date addedOn) {
        this.addedOn = addedOn;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
}
