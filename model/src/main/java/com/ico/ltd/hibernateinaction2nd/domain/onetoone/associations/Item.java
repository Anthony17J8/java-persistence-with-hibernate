package com.ico.ltd.hibernateinaction2nd.domain.onetoone.associations;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * One to One Uni-directional mapping with join table
 *
 * << Table >>          << Table >>                         << Table >>
 * ITEM                 ITEM_SHIPMENT                       SHIPMENT
 * ID << PK >>          SHIPMENT_ID << PK >> << FK >        ID << PK >>
 * ...                  ITEM_ID << FK >> << UNIQUE >>>      ...
 */
@Entity
public class Item {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    protected String name;

    public Item() {
    }

    public Item(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
