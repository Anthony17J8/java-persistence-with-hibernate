package com.ico.ltd.hibernateinaction2nd.domain.collections.mapofembeddables;


import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.HashMap;
import java.util.Map;

@Entity
public class Item {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    @ElementCollection
    @CollectionTable(name = "IMAGE")
    protected Map<Filename, Image> images = new HashMap<Filename, Image>();

    public Long getId() {
        return id;
    }

    public Map<Filename, Image> getImages() {
        return images;
    }

    public void setImages(Map<Filename, Image> images) {
        this.images = images;
    }
}
