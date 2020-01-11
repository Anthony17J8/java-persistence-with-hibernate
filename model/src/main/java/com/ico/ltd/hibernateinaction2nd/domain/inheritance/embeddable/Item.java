package com.ico.ltd.hibernateinaction2nd.domain.inheritance.embeddable;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A pitfall to watch out for is embedding a property of abstract superclass type (like
 * Measurement ) in an entity (like Item ). This can never work; the JPA provider doesn’t
 * know how to store and load Measurement instances polymorphically. It doesn’t have the
 * information necessary to decide whether the values in the database are Dimension or
 * Weight instances, because there is no discriminator. This means although you can have
 * an @Embeddable class inherit some persistent properties from a @MappedSuperclass ,
 * the reference to an instance isn’t polymorphic—it always names a concrete class.
 */
@Entity
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

    protected Dimensions dimensions;

    protected Weight weight;

    public Item() {
    }

    public Item(String name, Dimensions dimensions, Weight weight) {
        this.name = name;
        this.dimensions = dimensions;
        this.weight = weight;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public Long getId() { // Optional but useful
        return id;
    }

    public String getName() {
        return name;
    }

    public Weight getWeight() {
        return weight;
    }

    public void setDimensions(Dimensions dimensions) {
        this.dimensions = dimensions;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWeight(Weight weight) {
        this.weight = weight;
    }
}
