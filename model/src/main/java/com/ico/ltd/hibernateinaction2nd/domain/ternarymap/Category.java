package com.ico.ltd.hibernateinaction2nd.domain.ternarymap;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyJoinColumn;
import java.util.HashMap;
import java.util.Map;

@Entity
public class Category {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    protected String name;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @MapKeyJoinColumn(name = "ITEM_ID") // Defaults to ITEMADDEDBY_KEY
    @JoinTable(
        name = "CATEGORY_ITEM",
        joinColumns = @JoinColumn(name = "CATEGORY_ID"),
        inverseJoinColumns = @JoinColumn(name = "USER_ID")
    )
    protected Map<Item, User> itemAddedBy = new HashMap<>();

    public Category() {
    }

    public Category(String name) {
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

    public Map<Item, User> getItemAddedBy() {
        return itemAddedBy;
    }
}
