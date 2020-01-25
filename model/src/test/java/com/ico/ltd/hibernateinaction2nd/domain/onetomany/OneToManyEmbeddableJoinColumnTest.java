package com.ico.ltd.hibernateinaction2nd.domain.onetomany;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OneToManyEmbeddableJoinColumnTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void storeAndLoadUsersShipments() {
        User user = new User("johndoe");
        Address deliveryAddress = new Address("Some Street", "12345", "Some City");
        user.setShippingAddress(deliveryAddress);
        em.persist(user);

        Shipment firstShipment = new Shipment();
        deliveryAddress.getDeliveries().add(firstShipment);
        em.persist(firstShipment);

        Shipment secondShipment = new Shipment();
        deliveryAddress.getDeliveries().add(secondShipment);
        em.persist(secondShipment);

        em.flush();
        em.clear();

        Long USER_ID = user.getId();

        User johndoe = em.find(User.class, USER_ID);
        assertEquals(johndoe.getShippingAddress().getDeliveries().size(), 2);
    }
}