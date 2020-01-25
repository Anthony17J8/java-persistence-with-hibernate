package com.ico.ltd.hibernateinaction2nd.domain.manytomany;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ManyToManyLinkEntityTest {

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

        CategorizedItem linkOne = new CategorizedItem(
                "johndoe", someCategory, someItem
        );

        CategorizedItem linkTwo = new CategorizedItem(
                "johndoe", someCategory, otherItem
        );

        CategorizedItem linkThree = new CategorizedItem(
                "johndoe", otherCategory, someItem
        );

        em.persist(linkOne);
        em.persist(linkTwo);
        em.persist(linkThree);
        em.flush();
        em.clear();

        Long CATEGORY_ID = someCategory.getId();
        Long OTHER_CATEGORY_ID = otherCategory.getId();
        Long ITEM_ID = someItem.getId();
        Long OTHER_ITEM_ID = otherItem.getId();

        Category category1 = em.find(Category.class, CATEGORY_ID);
        Category category2 = em.find(Category.class, OTHER_CATEGORY_ID);

        Item item1 = em.find(Item.class, ITEM_ID);
        Item item2 = em.find(Item.class, OTHER_ITEM_ID);

        assertEquals(category1.getCategorizedItems().size(), 2);
        assertEquals(item1.getCategorizedItems().size(), 2);

        assertEquals(category2.getCategorizedItems().size(), 1);
        assertEquals(item2.getCategorizedItems().size(), 1);

        assertEquals(category2.getCategorizedItems().iterator().next().getItem(), item1);
        assertEquals(item2.getCategorizedItems().iterator().next().getCategory(), category1);
    }
}