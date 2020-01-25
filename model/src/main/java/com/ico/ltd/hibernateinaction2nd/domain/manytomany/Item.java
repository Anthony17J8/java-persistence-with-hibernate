package com.ico.ltd.hibernateinaction2nd.domain.manytomany;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * Bi-directional  Many to many association with an intermediate entity
 * <p>
 * The primary advantage of this strategy is the possibility for bidirectional navigation:
 * you can get all items in a category by calling someCategory.getCategorizedItems()
 * and then also navigate from the opposite direction with someItem.getCategorized-
 * Items() . A disadvantage is the more complex code needed to manage the
 * CategorizedItem entity instances to create and remove links, which you have to save
 * and delete independently.
 */
@Entity
public class Item {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    protected String name;

    @OneToMany(mappedBy = "item")
    protected Set<CategorizedItem> categorizedItems = new HashSet<>();

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

    public Set<CategorizedItem> getCategorizedItems() {
        return categorizedItems;
    }
}
