package com.ico.ltd.hibernateinaction2nd.domain.manytomany;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ManyToManyTernaryTest {

    @Autowired
    EntityManager em;

    @Test
    @Rollback
    @Transactional
    void storeAndLoadCategoryItems() {
        Category someCategory = new Category("Some Category");
        Category otherCategory = new Category("Other Category");
        em.persist(someCategory);
        em.persist(otherCategory);

        Item someItem = new Item("Some Item");
        Item otherItem = new Item("Other Item");
        em.persist(someItem);
        em.persist(otherItem);

        User someUser = new User("johndoe");
        em.persist(someUser);

        CategorizedItem linkOne = new CategorizedItem(
                someUser, someItem
        );
        someCategory.getCategorizedItems().add(linkOne);

        CategorizedItem linkTwo = new CategorizedItem(
                someUser, otherItem
        );
        someCategory.getCategorizedItems().add(linkTwo);

        CategorizedItem linkThree = new CategorizedItem(
                someUser, someItem
        );
        otherCategory.getCategorizedItems().add(linkThree);

        em.flush();
        em.clear();

        Long CATEGORY_ID = someCategory.getId();
        Long OTHER_CATEGORY_ID = otherCategory.getId();
        Long ITEM_ID = someItem.getId();
        Long USER_ID = someUser.getId();

        Category category1 = em.find(Category.class, CATEGORY_ID);
        Category category2 = em.find(Category.class, OTHER_CATEGORY_ID);

        Item item1 = em.find(Item.class, ITEM_ID);

        User user = em.find(User.class, USER_ID);

        assertEquals(category1.getCategorizedItems().size(), 2);

        assertEquals(category2.getCategorizedItems().size(), 1);

        assertEquals(category2.getCategorizedItems().iterator().next().getItem(), item1);
        assertEquals(category2.getCategorizedItems().iterator().next().getAddedBy(), user);



        Item item = em.find(Item.class, ITEM_ID);

        List<Category> categoriesOfItem =
                em.createQuery(
                        "select c from Category c " +
                                "join c.categorizedItems ci " +
                                "where ci.item = :itemParameter")
                        .setParameter("itemParameter", item)
                        .getResultList();

        assertEquals(categoriesOfItem.size(), 2);
    }
}