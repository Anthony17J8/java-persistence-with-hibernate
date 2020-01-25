package com.ico.ltd.hibernateinaction2nd.domain.manytomany;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Category {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    protected String name;

    @ElementCollection
    @CollectionTable(
            name = "CATEGORY_ITEM",
            joinColumns = @JoinColumn(name = "CATEGORY_ID")
    )
    protected Set<CategorizedItem> categorizedItems = new HashSet<>();

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

    public Set<CategorizedItem> getCategorizedItems() {
        return categorizedItems;
    }

    public void setCategorizedItems(Set<CategorizedItem> categorizedItems) {
        this.categorizedItems = categorizedItems;
    }
}
