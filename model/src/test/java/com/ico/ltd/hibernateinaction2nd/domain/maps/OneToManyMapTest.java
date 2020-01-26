package com.ico.ltd.hibernateinaction2nd.domain.maps;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OneToManyMapTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void storeAndLoadItemBids() {
        Item someItem = new Item("Some Item");
        em.persist(someItem);

        Bid someBid = new Bid(new BigDecimal("123.00"), someItem);
        em.persist(someBid);
        someItem.getBids().put(someBid.getId(), someBid); // Optional...

        Bid secondBid = new Bid(new BigDecimal("456.00"), someItem);
        em.persist(secondBid);
        someItem.getBids().put(secondBid.getId(), secondBid); // Optional...
        em.flush();
        em.clear();

        Long ITEM_ID = someItem.getId();

        Item item = em.find(Item.class, ITEM_ID);
        assertEquals(item.getBids().size(), 2);

        for (Map.Entry<Long, Bid> entry : item.getBids().entrySet()) {
            // The key is the identifier of each Bid
            assertEquals(entry.getKey(), entry.getValue().getId());
        }
    }
}