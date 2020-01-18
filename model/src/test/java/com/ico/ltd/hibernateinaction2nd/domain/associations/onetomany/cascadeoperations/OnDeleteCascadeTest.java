package com.ico.ltd.hibernateinaction2nd.domain.associations.onetomany.cascadeoperations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class OnDeleteCascadeTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void storeAndLoadItemsBids() {
        Item saved = new Item("Saved Item");
        em.persist(saved);

        Bid someBid = new Bid(new BigDecimal("123.00"), saved);
        saved.getBids().add(someBid);

        Bid secondBid = new Bid(new BigDecimal("456.00"), saved);
        saved.getBids().add(secondBid);

        em.flush();

        Long id = saved.getId();

        Item persistItem = em.find(Item.class, id);

        assertEquals(2, persistItem.getBids().size());
        em.remove(persistItem);
        em.flush();

        assertNull(em.find(Item.class, id));
        TypedQuery<Bid> query = em.createQuery("select b from Bid b where b.item.id =: itemId ", Bid.class);
        query.setParameter("itemId", id);
        assertEquals(0, query.getResultList().size());
    }
}