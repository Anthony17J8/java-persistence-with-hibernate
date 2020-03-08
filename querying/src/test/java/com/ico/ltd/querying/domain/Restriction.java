package com.ico.ltd.querying.domain;

import org.junit.jupiter.api.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Restriction extends QueryingTest {

    @Test
    void executeQueries() throws Exception {
        TestDataCategoriesItems testData = storeTestData();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        try {
            tx.begin();

            { // select i from Item i where i.name = 'Foo'
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                criteria.select(i).where(
                        cb.equal(i.get("name"), "Foo")
                );

                TypedQuery<Item> q = em.createQuery(criteria);
                assertThat(q.getResultList(), hasSize(1));
                assertEquals("Foo", q.getResultList().iterator().next().getName());
            }
            em.clear();
            {
                // Equals boolean
                // select u from User u where u.activated = true
                CriteriaQuery<User> criteria = cb.createQuery(User.class);
                Root<User> u = criteria.from(User.class);
                criteria.select(u).where(
                        cb.equal(u.get("activated"), true)
                );

                TypedQuery<User> q = em.createQuery(criteria);
                assertThat(q.getResultList(), hasSize(2));
            }
            em.clear();
            {
                // Between
                // select b from Bid b where b.amount between 99 and 110
                CriteriaQuery<Bid> criteria = cb.createQuery(Bid.class);
                Root<Bid> b = criteria.from(Bid.class);
                criteria.select(b).where(
                        cb.between(
                                b.get("amount"),
                                new BigDecimal("100"), new BigDecimal("110")
                        )
                );

                TypedQuery<Bid> query = em.createQuery(criteria);
                List<Bid> bids = query.getResultList();
                assertThat(bids, hasSize(2));
            }
            em.clear();
            {
                // Greater than
                // select b from Bid b where b.amount > 100
                CriteriaQuery<Bid> criteria = cb.createQuery(Bid.class);
                Root<Bid> b = criteria.from(Bid.class);
                criteria.select(b).where(
                        cb.gt(  // gt() only works with Number, use greaterThan() otherwise!
                                b.get("amount"),
                                new BigDecimal("100")
                        )
                );

                TypedQuery<Bid> query = em.createQuery(criteria);
                List<Bid> bids = query.getResultList();
                assertThat(bids, hasSize(1));
            }
            em.clear();
            {
                // Greater than with date (!Number)
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);

                Date tomorrow = Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC));
                criteria.select(i).where(
                        cb.greaterThan(
                                i.get("auctionEnd"),
                                tomorrow
                        )
                );

                TypedQuery<Item> query = em.createQuery(criteria);
                List<Item> items = query.getResultList();
                assertThat(items, hasSize(1));
            }
            em.clear();
            {   // IN list
                // select u from User u where u.username in ('johndoe', 'janeroe')
                CriteriaQuery<User> criteria = cb.createQuery(User.class);
                Root<User> u = criteria.from(User.class);
                criteria.select(u).where(
                        cb.in(u.get("username"))
                                .value("johndoe")
                                .value("janeroe")
                );

                TypedQuery<User> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(2));
            }
            em.clear();
            {
                // Enum
                // For restrictions with enums, use the fully qualified literal:
                // select i from Item where i.auctionType = com.ico.ltd.querying.domain.AuctionType.HIGHEST_BID
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> b = criteria.from(Item.class);
                criteria.select(b).where(
                        cb.equal(
                                b.get("auctionType"),
                                AuctionType.HIGHEST_BID
                        )
                );
                TypedQuery<Item> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(3));
            }
            em.clear();
            {
                // Ternary operators
                // select i from Item i where i.buyNowPrice is null
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> b = criteria.from(Item.class);
                criteria.select(b).where(
                        cb.isNull(b.get("buyNowPrice"))
                );

                TypedQuery<Item> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(2));
            }
            em.clear();
            {
                // select i from Item i where i.buyNowPrice is not null
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> b = criteria.from(Item.class);
                criteria.select(b).where(
                        cb.isNotNull(b.get("buyNowPrice"))
                );

                TypedQuery<Item> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(1));
            }
            em.clear();
            {
                // String matching (LIKE)
                // select u from User u where u.username like 'john%'
                CriteriaQuery<User> criteria = cb.createQuery(User.class);
                Root<User> u = criteria.from(User.class);
                criteria.select(u).where(
                        cb.like(u.get("username"), "john%")
                );
                TypedQuery<User> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(1));
            }
            em.clear();
            {
                // String matching (Negate LIKE)
                // select u from User u where u.username not like 'john%'
                CriteriaQuery<User> criteria = cb.createQuery(User.class);
                Root<User> u = criteria.from(User.class);
                criteria.select(u).where(
                        cb.like(u.get("username"), "john%").not()
                );
                TypedQuery<User> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(2));
            }
            em.clear();
            {
                CriteriaQuery<User> criteria = cb.createQuery(User.class);
                Root<User> u = criteria.from(User.class);
                criteria.select(u).where(
                        cb.like(u.get("username"), "%oe%")
                );

                TypedQuery<User> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(3));
            }
            em.clear();
            {
                // select i from Item i
                // where i.name like 'Name\\_with\\_underscores' escape :escapeChar
                // query.setParameter("escapeChar", "\\");

                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                criteria.select(i).where(
                        cb.like(i.get("name"), "Name\\_with\\_underscores", '\\')
                );
                TypedQuery<Item> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(0));
            }
            em.clear();
            {
                // arithmetic
                // select b from Bid b where (b.amount / 2) - 0.5 > 49
                CriteriaQuery<Bid> criteria = cb.createQuery(Bid.class);
                Root<Bid> b = criteria.from(Bid.class);
                criteria.select(b).where(
                        cb.gt(
                                cb.diff(
                                        cb.quot(b.get("amount"), 2),
                                        0.5
                                ),
                                49
                        )
                );

                TypedQuery<Bid> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(2));
            }
            em.clear();
            {
                // Logical groups
                // select i from Item i where (i.name like 'Fo%' and i.buyNowPrice is not null) or i.name = 'Bar'
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);

                Predicate predicate = cb.and(
                        cb.like(i.get("name"), "Fo%"),
                        cb.isNotNull(i.get("buyNowPrice"))
                );

                predicate = cb.or(
                        predicate,
                        cb.equal(i.get("name"), "Bar")
                );

                criteria.select(i).where(predicate);
                TypedQuery<Item> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(2));
            }
            em.clear();
            {
                // select c from Category c where c.items is not empty
                CriteriaQuery<Category> criteria = cb.createQuery(Category.class);
                Root<Category> c = criteria.from(Category.class);
                criteria.select(c).where(
                        cb.isNotEmpty(c.get("items"))
                );

                TypedQuery<Category> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(2));
            }
            em.clear();
            {
                // select c from Category c where size(c.items) > 1
                CriteriaQuery<Category> criteria = cb.createQuery(Category.class);
                Root<Category> c = criteria.from(Category.class);
                criteria.select(c).where(
                        cb.gt(
                                cb.size(c.get("items")),
                                1
                        )
                );

                TypedQuery<Category> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(1));
            }
            em.clear();
            {
                // select c from Category c where :item member of c.items
                CriteriaQuery<Category> criteria = cb.createQuery(Category.class);
                Root<Category> c = criteria.from(Category.class);
                criteria.select(c).where(
                        cb.isMember(
                                cb.parameter(Item.class, "item"),
                                c.<Collection<Item>>get("items"))
                );

                TypedQuery<Category> query = em.createQuery(criteria);
                Item item = em.find(Item.class, testData.items.getFirstId());
                query.setParameter("item", item);
                assertThat(query.getResultList(), hasSize(1));
            }
            em.clear();
            {
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                criteria.select(i).where(
                        cb.like(cb.lower(i.<String>get("name")), "ba%")
                );

                TypedQuery<Item> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(2));
            }
            em.clear();
            {
                // Calling arbitrary functions
                // select i from Item i where function('DATEDIFF', 'DAY', i.createdOn, i.auctionEnd) > 1
                // todo Issue: Invalid value "1" for parameter "parameterIndex
                CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
                Root<Item> i = criteria.from(Item.class);
                criteria.select(i).where(
                        cb.gt(
                                cb.function(
                                        "DATEDIFF",
                                        Integer.class,
                                        cb.literal("DAY"),
                                        i.get("createdOn"),
                                        i.get("auctionEnd")
                                ),
                                1
                        )
                );

                TypedQuery<Item> query = em.createQuery(criteria);
                assertThat(query.getResultList(), hasSize(1));
            }
            em.clear();
            {
                // select u from User u order by u.username desc
                CriteriaQuery<User> criteria = cb.createQuery(User.class);
                Root<User> u = criteria.from(User.class);
                criteria.select(u).orderBy(
                        cb.desc(u.get("username"))
                );

                TypedQuery<User> query = em.createQuery(criteria);
                List<User> users = query.getResultList();
                assertThat(users, hasSize(3));
                assertEquals("robertdoe", users.get(0).getUsername());
                assertEquals("johndoe", users.get(1).getUsername());
                assertEquals("janedoe", users.get(2).getUsername());
            }
            em.clear();
            {
                // select u from User u order by u.activated desc, u.username asc
                CriteriaQuery<User> criteria = cb.createQuery(User.class);
                Root<User> u = criteria.from(User.class);
                criteria.select(u).orderBy(
                        cb.desc(u.get("activated")),
                        cb.asc(u.get("username"))
                );
                TypedQuery<User> query = em.createQuery(criteria);
                List<User> users = query.getResultList();
                assertThat(users, hasSize(3));
            }
            tx.commit();
            em.close();

        } finally {
            tx.rollback();
        }
    }
}
