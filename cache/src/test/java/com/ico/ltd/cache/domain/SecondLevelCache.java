package com.ico.ltd.cache.domain;

import com.ico.ltd.cache.config.PersistenceConfig;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.NaturalIdCacheStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.management.ObjectName;
import javax.persistence.Cache;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class SecondLevelCache {

    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    @Test
    @DirtiesContext
    void testStoreData() throws Exception {
        storeTestData();
    }

    @Test
    @DirtiesContext
    void cacheBehaviour() throws Exception {
        CacheTestData testData = storeTestData();
        Long USER_ID = testData.users.getFirstId();
        Long ITEM_ID = testData.items.getFirstId();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // Get the statistics API
            Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();

            CacheRegionStatistics itemCacheStats =
                    stats.getCacheRegionStatistics(Item.class.getName());
//            assertEquals(3, itemCacheStats.getElementCountInMemory()); // warm cache, data are loaded when store
            assertEquals(0, itemCacheStats.getHitCount());

            // Hit the second-level cache with entity lookup by identifier
            Item item = em.find(Item.class, ITEM_ID);
            assertEquals(itemCacheStats.getHitCount(), 1);

            // Initializing a proxy will also hit the second-level cache
            CacheRegionStatistics userCacheStats =
                    stats.getCacheRegionStatistics(User.class.getName());
//            assertEquals(userCacheStats.getElementCountInMemory(), 3);
            assertEquals(userCacheStats.getHitCount(), 0);

            User seller = item.getSeller();
            assertEquals(seller.getUsername(), "johndoe"); // Initialize proxy
            assertEquals(userCacheStats.getHitCount(), 1);

            // Get the Item#bids collection and its referenced Bid entity instances
                /*
                   The statistics tell you that there are three <code>Item#bids</code>
                   collections in the cache (one for each <code>Item</code>). No
                   successful cache lookups have occurred so far.
                 */
            CacheRegionStatistics bidsCacheStats =
                    stats.getCacheRegionStatistics(Item.class.getName() + ".bids");
//            assertEquals(bidsCacheStats.getElementCountInMemory(), 3);
            assertEquals(bidsCacheStats.getHitCount(), 0);

                /*
                   The entity cache of <code>Bid</code> has five records, and you
                   haven't accessed it either.
                 */
            CacheRegionStatistics bidCacheStats =
                    stats.getCacheRegionStatistics(Bid.class.getName());
//            assertEquals(bidCacheStats.getElementCountInMemory(), 5);
            assertEquals(bidCacheStats.getHitCount(), 0);

                /*
                   Initializing the collection will read the data from both caches.
                 */
            Set<Bid> bids = item.getBids();
            assertEquals(bids.size(), 3);

                /*
                   The cache found one collection, as well as the data for
                   its three <code>Bid</code> elements.
                 */
            assertEquals(bidsCacheStats.getHitCount(), 1);
            assertEquals(bidCacheStats.getHitCount(), 3);


            tx.commit();
            em.close();
        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    public void cacheNaturalId() throws Exception {
        CacheTestData testData = storeTestData();
        Long USER_ID = testData.users.getFirstId();
        Long ITEM_ID = testData.items.getFirstId();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {

            Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();

            // Clear all natural ID cache regions
            emf.getCache()
                    .unwrap(org.hibernate.Cache.class)
                    .evictNaturalIdData();

            // Clear the User entity cache region
            emf.getCache().evict(User.class);

            {
                tx.begin();
                Session session = em.unwrap(Session.class);

                NaturalIdCacheStatistics userIdStats =
                        stats.getNaturalIdCacheStatistics(User.class.getName() + "##NaturalId");

                assertEquals(userIdStats.getExecutionCount(), 0);

                User user = session.byNaturalId(User.class)
                        .using("username", "johndoe")
                        .load();
                // select ID from USERS where USERNAME = ?
                // select * from USERS where ID = ?

                assertNotNull(user);

                assertEquals(userIdStats.getHitCount(), 0);
                assertEquals(userIdStats.getMissCount(), 1);
                assertEquals(userIdStats.getExecutionCount(), 1);

                CacheRegionStatistics userStats =
                        stats.getCacheRegionStatistics(User.class.getName());
                assertEquals(userStats.getHitCount(), 0);
                assertEquals(userStats.getMissCount(), 1);
//                assertEquals(userStats.getElementCountInMemory(), 1);

                tx.commit();
                em.close();
            }

            { // Execute the lookup again, hit the cache
                em = emf.createEntityManager();
                tx = em.getTransaction();
                tx.begin();

                Session session = em.unwrap(Session.class);

                /*
                   The natural identifier cache region for <code>User</code>s
                   has one element.
                 */
                NaturalIdCacheStatistics userIdStats =
                        stats.getNaturalIdCacheStatistics(User.class.getName() + "##NaturalId");
//                assertEquals(userIdStats.getElementCountInMemory(), 1);

                /*
                   The <code>org.hibernate.Session</code> API performs natural
                   identifier lookup; this is the only API for accessing the
                   natural identifier cache.
                 */
                User user = (User) session.byNaturalId(User.class)
                        .using("username", "johndoe")
                        .load();

                assertNotNull(user);

                /*
                   You had a cache hit for the natural identifier lookup; the
                   cache returned the identifier value of "johndoe".
                 */
                assertEquals(userIdStats.getHitCount(), 1);

                /*
                   You also had a cache hit for the actual entity data of
                   that <code>User</code>.
                 */
                CacheRegionStatistics userStats =
                        stats.getCacheRegionStatistics(User.class.getName());
                assertEquals(userStats.getHitCount(), 1);

                tx.commit();
                em.close();
            }

        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    public void cacheModes() throws Exception {
        CacheTestData testData = storeTestData();
        Long USER_ID = testData.users.getFirstId();
        Long ITEM_ID = testData.items.getFirstId();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {

            {
                tx.begin();

                Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();

                CacheRegionStatistics itemCacheStats =
                        stats.getCacheRegionStatistics(Item.class.getName());

                // Bypass the cache when retrieving an entity instance by identifier
                {
                    Map<String, Object> properties = new HashMap<String, Object>();
                    properties.put("javax.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS);
                    Item item = em.find(Item.class, ITEM_ID, properties); // Hit the database
                    assertEquals(itemCacheStats.getHitCount(), 0);
                }

                // Bypass the cache when storing an entity instance
//                assertEquals(itemCacheStats.getElementCountInMemory(), 3);
                em.setProperty("javax.persistence.cache.storeMode", CacheStoreMode.BYPASS);

                Item item = new Item(
                        // ...
                        "Some Item",
                        Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)),
                        em.find(User.class, USER_ID)
                );

                em.persist(item); // Not stored in the cache

                em.flush();
//                assertEquals(itemCacheStats.getElementCountInMemory(), 3); // Unchanged

                tx.commit();
                em.close();
            }

        } finally {
            tx.rollback();
        }
    }

    @Test
    @DirtiesContext
    public void cacheControl() throws Exception {
        CacheTestData testData = storeTestData();
        Long USER_ID = testData.users.getFirstId();
        Long ITEM_ID = testData.items.getFirstId();

        Cache cache = emf.getCache();

        assertTrue(cache.contains(Item.class, ITEM_ID));
        cache.evict(Item.class, ITEM_ID);
        cache.evict(Item.class);
        cache.evictAll();

        org.hibernate.Cache hibernateCache =
                cache.unwrap(org.hibernate.Cache.class);

        assertFalse(hibernateCache.containsEntity(Item.class, ITEM_ID));
        hibernateCache.evictEntityData();
        hibernateCache.evictCollectionData();
        hibernateCache.evictNaturalIdData();
        hibernateCache.evictQueryRegions();
    }

    public static class CacheTestData {
        public TestData items;
        public TestData users;
    }

    public CacheTestData storeTestData() throws Exception {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        Long[] itemIds = new Long[3];
        Long[] userIds = new Long[3];

        User johndoe = new User("johndoe");
        em.persist(johndoe);
        userIds[0] = johndoe.getId();

        User janeroe = new User("janeroe");
        em.persist(janeroe);
        userIds[1] = janeroe.getId();

        User robertdoe = new User("robertdoe");
        em.persist(robertdoe);
        userIds[2] = robertdoe.getId();

        Item item = new Item("Item One", Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)), johndoe);
        em.persist(item);
        itemIds[0] = item.getId();
        for (int i = 1; i <= 3; i++) {
            Bid bid = new Bid(item, robertdoe, new BigDecimal(9 + i));
            item.getBids().add(bid);
            em.persist(bid);
        }

        item = new Item("Item Two", Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)), johndoe);
        em.persist(item);
        itemIds[1] = item.getId();
        for (int i = 1; i <= 1; i++) {
            Bid bid = new Bid(item, janeroe, new BigDecimal(2 + i));
            item.getBids().add(bid);
            em.persist(bid);
        }

        item = new Item("Item_Three", Date.from(LocalDateTime.now().plusDays(2).toInstant(ZoneOffset.UTC)), janeroe);
        em.persist(item);
        itemIds[2] = item.getId();
        for (int i = 1; i <= 1; i++) {
            Bid bid = new Bid(item, johndoe, new BigDecimal(3 + i));
            item.getBids().add(bid);
            em.persist(bid);
        }

        tx.commit();
        em.close();

        // Warm up the cache properly
        em = emf.createEntityManager();
        tx = em.getTransaction();
        tx.begin();

        // We load all the User instances into the second-level cache
        // here because NONSTRICT_READ_WRITE strategy doesn't insert
        // them into the second-level cache when persist() is called,
        // only when they are loaded from the database. And we need
        // to do this in a new transaction, or TwoPhaseLoad doesn't
        // put it in the cache.
        em.createQuery("select u from User u").getResultList();

        // The Item#bids are only put in the cache when the
        // collection is loaded.
        for (Long itemId : itemIds) {
            em.find(Item.class, itemId).getBids().size();
        }
        tx.commit();
        em.close();

        emf.unwrap(SessionFactory.class).getStatistics().clear();

        CacheTestData testData = new CacheTestData();
        testData.items = new TestData(itemIds);
        testData.users = new TestData(userIds);
        return testData;
    }

    @Test
    @DirtiesContext
    public void cacheQueryResults() throws Exception {
        CacheTestData testData = storeTestData();
        Long USER_ID = testData.users.getFirstId();
        Long ITEM_ID = testData.items.getFirstId();

        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {

            // Clear the Item entity cache region
            emf.getCache().evict(Item.class);

            Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();

            {
                tx.begin();

                String queryString = "select i from Item i where i.name like :n";

                /*
                   You have to enable caching for a particular query. Without
                   the <code>org.hibernate.cachable</code> hint, the
                   result won't be stored in the query result cache.
                 */
                TypedQuery<Item> query = em.createQuery(queryString, Item.class)
                        .setParameter("n", "I%")
                        .setHint("org.hibernate.cacheable", true);

                /*
                   Hibernate will now execute the SQL query and retrieve the
                   result set into memory.                 */
                List<Item> items = query.getResultList();
                assertEquals(items.size(), 3);

                /*
                   Using the statistics API, you can find out more details.
                   This is the first time you execute this query, so you get
                   a cache miss, not a hit. Hibernate puts the query and
                   its result into the cache. If you run the exact same query
                   again, the result will be from the cache.
                 */
                QueryStatistics queryStats = stats.getQueryStatistics(queryString);
                assertEquals(queryStats.getCacheHitCount(), 0);
                assertEquals(queryStats.getCacheMissCount(), 1);
                assertEquals(queryStats.getCachePutCount(), 1);

                /*
                   The actual entity instance data retrieved in the result set is
                   stored in the entity cache region, not in the query result cache.
                 */
                CacheRegionStatistics itemCacheStats =
                        stats.getCacheRegionStatistics(Item.class.getName());
//                assertEquals(itemCacheStats.getElementCountInMemory(), 3);

                tx.commit();
                em.close();
            }

            { // Execute the query again, hitting the cache
                em = emf.createEntityManager();
                tx = em.getTransaction();
                tx.begin();
                String queryString = "select i from Item i where i.name like :n";

                List<Item> items = em.createQuery(queryString, Item.class)
                        .setParameter("n", "I%")
                        .setHint("org.hibernate.cacheable", true)
                        .getResultList();

                assertEquals(items.size(), 3);

                QueryStatistics queryStats = stats.getQueryStatistics(queryString);
                assertEquals(queryStats.getCacheHitCount(), 1);
                assertEquals(queryStats.getCacheMissCount(), 1);
                assertEquals(queryStats.getCachePutCount(), 1);

                tx.commit();
                em.close();
            }

        } finally {
            tx.rollback();
        }
    }

    @javax.management.MXBean
    public interface StatisticsMXBean extends Statistics {
    }

    public void exposeStatistics(final Statistics statistics) throws Exception {
        statistics.setStatisticsEnabled(true);
        Object statisticsBean = Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{StatisticsMXBean.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return method.invoke(statistics, args);
                    }
                });
        ManagementFactory.getPlatformMBeanServer()
                .registerMBean(
                        statisticsBean,
                        new ObjectName("org.hibernate:type=statistics")
                );
    }
}