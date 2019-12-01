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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ConverterTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void testMonetaryAmountConverter() {
        Item saved = new Item("New Name", "Some Description");
        MonetaryAmount amount = new MonetaryAmount(new BigDecimal("11.22"), Currency.getInstance("USD"));
        saved.setBuyNowPrice(amount);

        em.persist(saved);

        Item result = em.find(Item.class, saved.getId());
        assertEquals(result.getBuyNowPrice(), amount);
        assertEquals(result.getBuyNowPrice().getValue(), new BigDecimal("11.22"));
        assertEquals(result.getBuyNowPrice().getCurrency(), Currency.getInstance("USD"));
    }

    @Test
    @Rollback
    @Transactional
    void testZipcodeConverter() {
        User saved = new User();
        City city = new City("Some City", "Some country", new GermanZipcode("33352"));
        Address homeAddress = new Address("Some street", city);
        saved.setHomeAddress(homeAddress);

        em.persist(saved);

        User result = em.find(User.class, saved.getId());
        assertTrue(result.getHomeAddress().getCity().getZipcode() instanceof GermanZipcode);
        assertEquals(result.getHomeAddress().getCity().getZipcode().getValue(), "33352");
    }
}
