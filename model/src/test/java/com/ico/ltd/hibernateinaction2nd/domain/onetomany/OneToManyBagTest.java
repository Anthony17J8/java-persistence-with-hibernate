package com.ico.ltd.hibernateinaction2nd.domain.onetomany;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OneToManyBagTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void storeAndLoadItemBids() {
        Item newItem = new Item("New Item");
        em.persist(newItem);

        Bid newBid = new Bid(new BigDecimal("123.000"), newItem);
        newItem.getBids().add(newBid);
        newItem.getBids().add(newBid); // no persistent effect
        em.persist(newBid);

        assertEquals(2, newItem.getBids().size());
        em.flush();
        em.clear();

        Long itemId = newItem.getId();
        Item saved = em.find(Item.class, itemId);
        assertEquals(1, saved.getBids().size());
        em.clear();

        Item item = em.find(Item.class, itemId);
        Bid newBid2 = new Bid(new BigDecimal("456.000"), newItem);
        item.getBids().add(newBid2); // item bids not loaded from db
        em.persist(newBid2);
        em.flush();
    }
}