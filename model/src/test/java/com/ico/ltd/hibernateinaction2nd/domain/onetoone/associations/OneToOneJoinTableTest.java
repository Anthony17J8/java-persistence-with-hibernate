package com.ico.ltd.hibernateinaction2nd.domain.onetoone.associations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class OneToOneJoinTableTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    public void storeAndLoadItemShipment() throws Exception {

        Shipment singleShipment = new Shipment();
        em.persist(singleShipment);

        Item someItem = new Item("Some item");
        em.persist(someItem);

        Shipment auctionShipment = new Shipment(someItem);
        em.persist(auctionShipment);

        em.flush();
        em.clear();

        Long itemId = someItem.getId();
        Long shipmentId = singleShipment.getId();
        Long auctionShipmentId = auctionShipment.getId();

        Shipment single = em.find(Shipment.class, shipmentId);
        assertNull(single.getAuction());

        Shipment aShipment = em.find(Shipment.class, auctionShipmentId);
        Item item = em.find(Item.class, itemId);
        assertEquals(item, aShipment.getAuction()); // lazy loading
    }

    @Test
    @Transactional
    @Rollback
    void storeNonUniqueRelationship() throws Exception {
        Item someItem = new Item("Some Item");
        em.persist(someItem);

        Shipment shipment1 = new Shipment(someItem);
        em.persist(shipment1);

        Shipment shipment2 = new Shipment(someItem);
        em.persist(shipment2);

        assertThrows(PersistenceException.class, () -> em.flush());
    }
}