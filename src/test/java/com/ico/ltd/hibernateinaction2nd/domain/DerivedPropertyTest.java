package com.ico.ltd.hibernateinaction2nd.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DerivedPropertyTest {

    @Autowired
    EntityManager em;

    @Test
    void testReadOnlyDerivedProperty() {
        Item result = em.find(Item.class, 1L);

        assertEquals(new BigDecimal(200).doubleValue(), result.getAverageBidAmount().doubleValue());
        assertEquals("Java: A Deta...", result.getShortDescription());
    }
}
