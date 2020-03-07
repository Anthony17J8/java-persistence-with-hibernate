package com.ico.ltd.querying.domain;

import com.ico.ltd.querying.config.PersistenceConfig;
import com.ico.ltd.querying.util.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class QueryingTest {

    @Autowired
    protected EntityManagerFactory emf;

    protected EntityManager em;

    public static class TestDataCategoriesItems {
        public TestData categories;
        public TestData items;
        public TestData users;
    }

    @Test
    void testInitData() throws Exception {
        storeTestData();
    }

    protected TestDataCategoriesItems storeTestData() throws Exception {
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        tx.begin();

        Long[] categoryIds = new Long[4];
        Long[] itemIds = new Long[3];
        Long[] userIds = new Long[3];

        User johndoe = new User("johndoe", "John", "Doe");
        Address homeAddress = new Address("Some Street 123", "12345", "Some City");
        johndoe.setActivated(true);
        johndoe.setHomeAddress(homeAddress);
        em.persist(johndoe);
        userIds[0] = johndoe.getId();

        User janeroe = new User("janeroe", "Jane", "Roe");
        janeroe.setActivated(true);
        janeroe.setHomeAddress(new Address("Other Street 11", "1234", "Other City"));
        em.persist(janeroe);
        userIds[1] = janeroe.getId();

        User robertdoe = new User("robertdoe", "Robert", "Doe");
        em.persist(robertdoe);
        userIds[2] = robertdoe.getId();

        Category categoryOne = new Category("One");
        em.persist(categoryOne);
        categoryIds[0] = categoryOne.getId();

        Item item = new Item("Foo", Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)), johndoe);
        item.setBuyNowPrice(new BigDecimal("19.99"));
        em.persist(item);
        itemIds[0] = item.getId();
        categoryOne.getItems().add(item);
        item.getCategories().add(categoryOne);
        for (int i = 1; i <= 3; i++) {
            Bid bid = new Bid(item, robertdoe, new BigDecimal(98 + i));
            item.getBids().add(bid);
            em.persist(bid);
        }
        item.getImages().add(new Image("Foo", "foo.jpg", 640, 480));
        item.getImages().add(new Image("Bar", "bar.jpg", 800, 600));
        item.getImages().add(new Image("Baz", "baz.jpg", 1024, 768));

        Category categoryTwo = new Category("Two");
        categoryTwo.setParent(categoryOne);
        em.persist(categoryTwo);
        categoryIds[1] = categoryTwo.getId();

        item = new Item("Bar", Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)), johndoe);
        em.persist(item);
        itemIds[1] = item.getId();
        categoryTwo.getItems().add(item);
        item.getCategories().add(categoryTwo);
        Bid bid = new Bid(item, janeroe, new BigDecimal("4.99"));
        item.getBids().add(bid);
        em.persist(bid);

        item = new Item("Baz", Date.from(LocalDateTime.now().plusDays(2).toInstant(ZoneOffset.UTC)), janeroe);
        item.setApproved(false);
        em.persist(item);
        itemIds[2] = item.getId();
        categoryTwo.getItems().add(item);
        item.getCategories().add(categoryTwo);

        Category categoryThree = new Category("Three");
        categoryThree.setParent(categoryOne);
        em.persist(categoryThree);
        categoryIds[2] = categoryThree.getId();

        Category categoryFour = new Category("Four");
        categoryFour.setParent(categoryTwo);
        em.persist(categoryFour);
        categoryIds[3] = categoryFour.getId();

        CreditCard cc = new CreditCard(
                "John Doe", "1234123412341234", "06", "2015"
        );
        em.persist(cc);

        BankAccount ba = new BankAccount(
                "Jane Roe", "445566", "One Percent Bank Inc.", "999"
        );
        em.persist(ba);

        LogRecord lr = new LogRecord("johndoe", "This is a log message");
        em.persist(lr);
        lr = new LogRecord("johndoe", "Another log message");
        em.persist(lr);

        tx.commit();
        em.close();

        TestDataCategoriesItems testData = new TestDataCategoriesItems();
        testData.categories = new TestData(categoryIds);
        testData.items = new TestData(itemIds);
        testData.users = new TestData(userIds);
        return testData;
    }
}