package com.ico.ltd.hibernateinaction2nd.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DerivedPropertyTest {

    @Autowired
    EntityManager em;

    @Test
    @DirtiesContext
    void testReadOnlyDerivedProperty() {
        Item result = em.find(Item.class, 1L);

        assertEquals(new BigDecimal(200).doubleValue(), result.getAverageBidAmount().doubleValue());
        assertEquals("Java: A Deta...", result.getShortDescription());
    }

    @Test
    @Rollback
    @Transactional
    @DirtiesContext
    void testTransformerColumnTest() {
        Item saved = new Item("Some Name", "Some descr");
        saved.setMetricWeight(2d);
        saved.setInitialPrice(new BigDecimal(100));

        em.persist(saved);

        Double mw = (Double) em.createQuery(
                "select i.metricWeight from Item i where i.id=:itemId"
        )
                .setParameter("itemId", 1000L).getSingleResult();

        assertEquals(saved.getMetricWeight(), mw);
    }
}
