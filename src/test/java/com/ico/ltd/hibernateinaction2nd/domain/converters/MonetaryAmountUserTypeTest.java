package com.ico.ltd.hibernateinaction2nd.domain.converters;

import com.ico.ltd.hibernateinaction2nd.domain.Item;
import com.ico.ltd.hibernateinaction2nd.domain.MonetaryAmount;
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
class MonetaryAmountUserTypeTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    public void testSaveItemWithMonetaryAmounts() throws Exception {
        Item saved = new Item("Some name", "Some description");
        saved.setBuyNowPrice(new MonetaryAmount(new BigDecimal(2), Currency.getInstance("EUR")));
        saved.setInitialPrice(new MonetaryAmount(new BigDecimal(3), Currency.getInstance("USD")));

        em.persist(saved);
        em.flush();
        em.clear();

        Item result = em.createQuery("select i from Item i where  i.id=:itemId", Item.class)
                .setParameter("itemId", 1000L)
                .getSingleResult();

        assertEquals("4.00 EUR", result.getBuyNowPrice().toString());
        assertEquals("6.00 USD", result.getInitialPrice().toString());
    }
}