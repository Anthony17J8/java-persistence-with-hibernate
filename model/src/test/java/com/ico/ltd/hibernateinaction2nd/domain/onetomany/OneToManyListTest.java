package com.ico.ltd.hibernateinaction2nd.domain.onetomany;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class OneToManyListTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void storeAndLoadItemBids() throws Exception {

        Item someItem = new Item("Some Item");
        em.persist(someItem);

        Bid someBid = new Bid(new BigDecimal("123.00"), someItem);
        someItem.getBids().add(someBid);
//        someItem.getBids().add(someBid); ???
        em.persist(someBid);

        Bid secondBid = new Bid(new BigDecimal("456.00"), someItem);
        someItem.getBids().add(secondBid);
        em.persist(secondBid);

        assertEquals(2, someItem.getBids().size());

        em.flush();
        em.clear();

        Long ITEM_ID = someItem.getId();
        Long secondBidId = secondBid.getId();

        Item item = em.find(Item.class, ITEM_ID);
        List<Bid> bids = item.getBids();
        assertEquals(bids.size(), 2);
        assertEquals(bids.get(0).getAmount().compareTo(new BigDecimal("123")), 0);
        assertEquals(bids.get(1).getAmount().compareTo(new BigDecimal("456")), 0);
        em.flush();
        em.clear();
    }
}