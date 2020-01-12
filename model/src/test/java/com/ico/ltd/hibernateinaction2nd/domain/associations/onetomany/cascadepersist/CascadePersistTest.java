package com.ico.ltd.hibernateinaction2nd.domain.associations.onetomany.cascadepersist;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CascadePersistTest {

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
        assertEquals(persistItem.getBids().size(), 2);
    }
}