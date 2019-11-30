package com.ico.ltd.hibernateinaction2nd.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class AccessStrategyTest {

    @Autowired
    EntityManager em;

    @Test
    @Transactional
    @Rollback
    void testAccessTypePropertyNameField() {
        final String name = "New Item Name";
        Item savedItem = new Item(name);

        em.persist(savedItem);

        Item result = em.find(Item.class, 1000L);
        assertEquals("AUCTION " + name, result.getName());
    }
}
