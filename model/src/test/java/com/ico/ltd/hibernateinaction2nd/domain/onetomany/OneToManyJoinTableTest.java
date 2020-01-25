package com.ico.ltd.hibernateinaction2nd.domain.onetomany;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OneToManyJoinTableTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void storeAndLoadItemUsers() {
        Item someItem = new Item("Some Item");
        em.persist(someItem);
        Item otherItem = new Item("Other Item");
        em.persist(otherItem);

        User someUser = new User("johndoe");
        someUser.getBoughtItems().add(someItem); // Link
        someItem.setBuyer(someUser); // Link
        someUser.getBoughtItems().add(otherItem);
        otherItem.setBuyer(someUser);
        em.persist(someUser);

        Item unsoldItem = new Item("Unsold Item");
        em.persist(unsoldItem);
        em.flush();
        em.clear();

        Long ITEM_ID = someItem.getId();
        Long UNSOLD_ITEM_ID = unsoldItem.getId();

        Item item = em.find(Item.class, ITEM_ID);
        assertEquals(item.getBuyer().getUsername(), "johndoe");
        assertTrue(item.getBuyer().getBoughtItems().contains(item));

        Item item2 = em.find(Item.class, UNSOLD_ITEM_ID);
        assertNull(item2.getBuyer());

    }
}