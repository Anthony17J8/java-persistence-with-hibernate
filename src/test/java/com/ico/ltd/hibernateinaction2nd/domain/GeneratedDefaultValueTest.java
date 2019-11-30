package com.ico.ltd.hibernateinaction2nd.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
public class GeneratedDefaultValueTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void testGenerateDefaultValues() {
        Item saved = new Item("Name", "Description");
        em.persist(saved);
        em.flush();

        Item result = em.find(Item.class, 1000L);

        assertEquals(1., result.getInitialPrice().doubleValue());
        assertNotNull(result.getCreatedOn());
    }
}
