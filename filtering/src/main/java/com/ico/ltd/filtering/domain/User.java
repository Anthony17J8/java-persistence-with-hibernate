package com.ico.ltd.filtering.domain;

import com.ico.ltd.filtering.Constants;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "USERS")
//@ExcludeDefaultListeners
//@ExcludeSuperclassListeners
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
     * You don’t have to write a separate entity listener class to intercept life cycle events.
     * You can, for example, implement the notifyAdmin() method on the User entity class.
     *
     * Note that callback methods on an entity class don’t have any arguments: the “current”
     * entity involved in the state changes is this . Duplicate callbacks for the same event
     * aren’t allowed in a single class. But you can intercept the same event with callback
     * methods in several listener classes or in a listener and an entity class.
     */
    @PostPersist
    public void notifyAdmin() {
        User currentUser = CurrentUser.INSTANCE.get();
        Mail mail = Mail.INSTANCE;
        mail.send(
                "Entity instance persisted by "
                        + currentUser.getUsername()
                        + ": "
                        + this
        );
    }
}
