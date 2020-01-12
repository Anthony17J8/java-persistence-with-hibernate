package com.ico.ltd.hibernateinaction2nd.domain.collections.setofembeddables;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.AttributeOverride;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Item {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    @ElementCollection // required
    @CollectionTable(name = "IMAGE") // override collection table name
    @AttributeOverride(
            name = "filename",
            column = @Column(name = "FNAME", nullable = false)
    )
    protected Set<Image> images = new HashSet<>();

    public Set<Image> getImages() {
        return images;
    }

    public void setImages(Set<Image> images) {
        this.images = images;
    }

    public Long getId() {
        return id;
    }
}
