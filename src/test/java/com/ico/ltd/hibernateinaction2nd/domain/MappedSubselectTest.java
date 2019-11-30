package com.ico.ltd.hibernateinaction2nd.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
class MappedSubselectTest {

    @Autowired
    EntityManager em;

    @Test
    @Transactional
    @Rollback
    void testSubselect() throws Exception {
        ItemBidSummary result = em.find(ItemBidSummary.class, 1L);
        assertEquals("Some name", result.getName());

        em.clear();

        // update name of item
        Item item = em.find(Item.class, 1L);
        item.setName("New name");

        // No flush before retrieval by the identifier
        // ItemBidSummary wrongResult = em.find(ItemBidSummary.class, 1L);
        // assertNotEquals("New name", wrongResult.getName());

        // Automatic flush before queries if synchronized tables are affected
        Query query = em.createQuery(
                "select ibs from ItemBidSummary ibs where ibs.itemId = :id"
        );

        ItemBidSummary trueResult = (ItemBidSummary) query.setParameter("id", 1L).getSingleResult();
        assertEquals("AUCTION New name", trueResult.getName());
        assertEquals(3, trueResult.getNumberOfBids());
    }
}