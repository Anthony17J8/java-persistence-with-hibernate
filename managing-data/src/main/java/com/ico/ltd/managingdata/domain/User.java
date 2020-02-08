package com.ico.ltd.managingdata.domain;

import com.ico.ltd.managingdata.Constants;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "USERS",
        uniqueConstraints =
        @UniqueConstraint(columnNames = "USERNAME"))
public class User {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    @NotNull
    protected String username;

    protected User() {
    }

    public User(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * equals() method code always accesses the properties
     * of the “other” reference via getter methods. This is extremely important, because the
     * reference passed as other may be a Hibernate proxy, not the actual instance that
     * holds the persistent state. You can’t access the username field of a User proxy directly.
     * To initialize the proxy to get the property value, you need to access it with a getter
     * method.
     * <p>
     * Check the type of the other reference with instanceof , not by comparing the val-
     * ues of getClass() . Again, the other reference may be a proxy, which is a runtime-
     * generated subclass of User , so this and other may not be exactly the same type but a
     * valid super/subtype.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof User)) return false; // Use instanceof!
        User that = (User) other;
        return this.getUsername().equals(that.getUsername()); // Use getters!
    }

    @Override
    public int hashCode() {
        return getUsername().hashCode();
    }
}
