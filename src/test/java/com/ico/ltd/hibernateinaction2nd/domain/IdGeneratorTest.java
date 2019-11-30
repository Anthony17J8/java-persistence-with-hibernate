package com.ico.ltd.hibernateinaction2nd.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class IdGeneratorTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void testIncrementId() {
        for (int i = 0; i < 10; i++) {
            Item saved = new Item();
            saved.setName("New Name" + i);
            saved.setAuctionEnd(new Date());
            em.persist(saved);
        }

        for (int i = 0; i < 10; i++) {
            Item result = em.find(Item.class, (long) 1000 + i);
            assertEquals("AUCTION New Name" + i, result.getName());
        }
    }
}
