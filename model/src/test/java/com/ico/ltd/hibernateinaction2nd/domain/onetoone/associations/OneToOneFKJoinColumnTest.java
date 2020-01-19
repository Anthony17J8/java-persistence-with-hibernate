package com.ico.ltd.hibernateinaction2nd.domain.onetoone.associations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class OneToOneFKJoinColumnTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void storeAndLoadUserAddress() {
        User savedUser = new User("John Doe");
        Address someAddress = new Address("Street 112", "12345", "New City");

        savedUser.setShippingAddress(someAddress); // link

        em.persist(savedUser);  // Transitive persistence of shippingAddress
        em.flush();
        em.clear();

        Long userId = savedUser.getId();
        Long addressId = someAddress.getId();

        User user = em.find(User.class, userId);

        assertNotNull(user);

        assertNotNull(user.getShippingAddress()); // lazy loading
        assertEquals("New City", user.getShippingAddress().getCity());

        Address address = em.find(Address.class, addressId);
        assertNotNull(address);
        assertEquals("New City", address.getCity());
    }

    @Test
    @Rollback
    @Transactional
    void storeNonUniqueRelationship() throws Exception {
        User userOne = new User("John Doe");
        User userTwo = new User("Anna Doe");

        Address someAddress = new Address("Street 112", "12345", "New City");

        userOne.setShippingAddress(someAddress);
        em.persist(userOne); // OK

        userTwo.setShippingAddress(someAddress);
        em.persist(userTwo); // Fails, true unique @OneToOne

        assertThrows(PersistenceException.class, () -> em.flush()); // Hibernate tries the INSERT but fails
        em.clear();
    }
}