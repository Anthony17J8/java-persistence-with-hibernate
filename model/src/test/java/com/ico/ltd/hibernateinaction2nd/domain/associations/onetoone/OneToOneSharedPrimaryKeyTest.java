package com.ico.ltd.hibernateinaction2nd.domain.associations.onetoone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class OneToOneSharedPrimaryKeyTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void storeAndLoadItemsBids() {
        Address someAddress = new Address("Street 112", "12345", "New City");
        em.persist(someAddress);

        User savedUser = new User(someAddress.getId(), "John Doe");

        em.persist(savedUser);
        Long id = savedUser.getId();
        em.flush();
        em.clear();

        User persisted = em.find(User.class, id);
        Address address = persisted.getShippingAddress(); // at this stage address wouldn't be loaded (lazy loading)
        assertNotNull(address);
        assertEquals("New City", address.getCity()); // address loaded
        assertEquals("Street 112", address.getStreet());
        assertEquals("12345", address.getZipcode());
    }
}