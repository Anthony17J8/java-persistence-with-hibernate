package com.ico.ltd.hibernateinaction2nd.domain.ternarymap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MapTernaryTest {

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

        someCategory.getItemAddedBy().put(someItem, someUser);
        someCategory.getItemAddedBy().put(otherItem, someUser);
        otherCategory.getItemAddedBy().put(someItem, someUser);
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

        assertEquals(category1.getItemAddedBy().size(), 2);

        assertEquals(category2.getItemAddedBy().size(), 1);

        assertEquals(category2.getItemAddedBy().keySet().iterator().next(), item1);
        assertEquals(category2.getItemAddedBy().values().iterator().next(), user);
    }
}