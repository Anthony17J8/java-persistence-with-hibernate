package com.ico.ltd.hibernateinaction2nd.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ConverterTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void testMonetaryAmountConverterTest() {
        Item saved = new Item("New Name", "Some Description");
        MonetaryAmount amount = new MonetaryAmount(new BigDecimal("11.22"), Currency.getInstance("USD"));
        saved.setBuyNowPrice(amount);
        em.persist(saved);
        em.flush();

        Item result = em.find(Item.class, 1000L);
        assertEquals(result.getBuyNowPrice(), amount);
        assertEquals(result.getBuyNowPrice().getValue(), new BigDecimal("11.22"));
        assertEquals(result.getBuyNowPrice().getCurrency(), Currency.getInstance("USD"));
    }
}
