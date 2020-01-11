package com.ico.ltd.hibernateinaction2nd.domain.collections.bagofstrings;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.Collection;

@Entity
public class Item {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    @ElementCollection
    @CollectionTable(name = "IMAGE")
    @Column(name = "FILENAME")
    @org.hibernate.annotations.CollectionId( // surrogate primary key allows duplicates
            columns = @Column(name = "IMAGE_ID"), // surrogate primary key column
            type = @org.hibernate.annotations.Type(type = "long"), // Hibernate only
            generator = Constants.ID_GENERATOR
    )
    protected Collection<String> images = new ArrayList<>();

    public Collection<String> getImages() {
        return images;
    }

    public void setImages(Collection<String> images) {
        this.images = images;
    }
}
