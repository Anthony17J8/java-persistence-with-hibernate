package com.ico.ltd.hibernateinaction2nd.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class AccessStrategyTest {

    static final String ITEM_NAME = "New Item Name";

    @Autowired
    EntityManager em;

    private Item saved;

    @BeforeEach
    void setUp() {
        saved = new Item(ITEM_NAME);
    }

    @Test
    @Transactional
    @Rollback
    void testAccessTypePropertyName() {
        em.persist(saved);

        Item result = em.find(Item.class, 1000L);
        assertEquals("AUCTION " + ITEM_NAME, result.getName());
    }

    @Test
    @Transactional
    @Rollback
    void testAccessTypeFieldName() {
        em.persist(saved);
        Item result = em.find(Item.class, 1000L);
        assertEquals(ITEM_NAME, result.getName());
    }
}
