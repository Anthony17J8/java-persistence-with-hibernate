package com.ico.ltd.hibernateinaction2nd.domain.onetoone.associations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OneToOneFKGeneratorTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void storeAndLoadUserAddress() {
        User savedUser = new User("John Doe");
        Address someAddress = new Address(savedUser, "Street 112", "12345", "New City");

        savedUser.setShippingAddress(someAddress); // link

        em.persist(savedUser);  // Transitive persistence of shippingAddress
        em.flush();
        em.clear();

        Long userId = savedUser.getId();
        Long addressId = someAddress.getId();

        User user = em.find(User.class, userId);

        assertEquals(user.getShippingAddress().getZipcode(), "12345");

        Address address = em.find(Address.class, addressId);

        assertEquals("12345", address.getZipcode());
        assertEquals(userId, addressId);
        assertEquals(user, address.getUser());
    }

}