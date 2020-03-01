package com.ico.ltd.filtering.domain;

import com.ico.ltd.filtering.config.PersistenceConfig;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.MatchMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersistenceConfig.class)
class Envers {

    @Autowired
    EntityManagerFactory emf;

    EntityManager em;

    @Test
    void auditLogging() {
        em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Long ITEM_ID;
            Long USER_ID;

            {
                // create
                tx.begin();
                User user = new User("johndoe");
                em.persist(user);

                Item item = new Item("Foo", user);
                em.persist(item);

                tx.commit();
                em.close();

                ITEM_ID = item.getId();
                USER_ID = user.getId();
            }
            Date TIMESTAMP_CREATE = new Date();

            {
                // Update
                em = emf.createEntityManager();
                tx = em.getTransaction();
                tx.begin();

                Item item = em.find(Item.class, ITEM_ID);
                item.setName("Bar");
                item.getSeller().setUsername("doejohn");

                tx.commit();
                em.close();
            }
            Date TIMESTAMP_UPDATE = new Date();

            {
                // Delete
                em = emf.createEntityManager();
                tx = em.getTransaction();
                tx.begin();

                Item item = em.find(Item.class, ITEM_ID);
                em.remove(item);

                tx.commit();
                em.close();
            }
            Date TIMESTAMP_DELETE = new Date();

            {
                em = emf.createEntityManager();
                tx = em.getTransaction();
                tx.begin();

                /*
                   The main Envers API is the <code>AuditReader</code>, it can be accessed with
                   an <code>EntityManager</code>.
                 */
                AuditReader auditReader = AuditReaderFactory.get(em);

                 /*
                   Given a timestamp, you can find the revision number of a change set, made
                   before or on that timestamp.
                 */
                Number revisionCreate = auditReader.getRevisionNumberForDate(TIMESTAMP_CREATE);
                Number revisionUpdate = auditReader.getRevisionNumberForDate(TIMESTAMP_UPDATE);
                Number revisionDelete = auditReader.getRevisionNumberForDate(TIMESTAMP_DELETE);

                /*
                   If you don't have a timestamp, you can get all revision numbers in which a
                   particular audited entity instance was involved. This operation finds all
                   change sets where the given <code>Item</code> was created, modified, or
                   deleted. In our example, we created, modified, and then deleted the
                   <code>Item</code>. Hence, we have three revisions.
                 */
                List<Number> itemRevisions = auditReader.getRevisions(Item.class, ITEM_ID);
                assertEquals(itemRevisions.size(), 3);
                for (Number itemRevision : itemRevisions) {
                    /*
                       If you have a revision number, you can get the timestamp when Envers
                       logged the change set.
                     */
                    Date itemRevisionTimestamp = auditReader.getRevisionDate(itemRevision);
                    // ...
                }

                /*
                   We created and modified the <code>User</code>, so there are two revisions.
                 */
                List<Number> userRevisions = auditReader.getRevisions(User.class, USER_ID);
                assertEquals(userRevisions.size(), 2);

                em.clear();

                {
                /*
                    If you don’t know modification timestamps or revision numbers, you can write a query
                    with forRevisionsOfEntity() to obtain all audit trail details of a particular entity.
                 */
                    AuditQuery query = auditReader.createQuery()
                            .forRevisionsOfEntity(Item.class, false, false);

                /*
                   This query returns the audit trail details as a <code>List</code> of
                   <code>Object[]</code>.
                 */
                    List<Object[]> resultList = query.getResultList();
                    for (Object[] tuple : resultList) {

                    /*
                        Each result tuple contains the entity instance for a particular revision, the revision
                        details (including revision number and timestamp), as well as the revision type.
                     */
                        Item item = (Item) tuple[0];
                        DefaultRevisionEntity revision = (DefaultRevisionEntity) tuple[1];
                        RevisionType revisionType = (RevisionType) tuple[2];

                    /*
                        The revision type indicates why Envers created the revision, because the entity
                        instance was inserted, modified, or deleted in the database.
                     */
                        if (revision.getId() == 1) {
                            assertEquals(revisionType, RevisionType.ADD);
                            assertEquals(item.getName(), "Foo");
                        } else if (revision.getId() == 2) {
                            assertEquals(revisionType, RevisionType.MOD);
                            assertEquals(item.getName(), "Bar");
                        } else if (revision.getId() == 3) {
                            assertEquals(revisionType, RevisionType.DEL);
                            assertNull(item);
                        }
                    }
                }

                em.clear();

                {
                    /*
                        The find() method returns an audited entity instance version, given a revision. This
                        operation loads the Item as it was after creation.
                     */
                    Item item = auditReader.find(Item.class, ITEM_ID, revisionCreate);
                    assertEquals("Foo", item.getName());
                    assertEquals("johndoe", item.getSeller().getUsername());

                    /*
                        This operation loads the Item after it was updated. Note how the modified seller of
                        this change set is also retrieved automatically.
                     */
                    Item modifiedItem = auditReader.find(Item.class, ITEM_ID, revisionUpdate);
                    assertEquals("Bar", modifiedItem.getName());
                    assertEquals("doejohn", modifiedItem.getSeller().getUsername());

                    /*
                        In this revision, the Item was deleted, so find() returns null .
                     */
                    Item deletedItem = auditReader.find(Item.class, ITEM_ID, revisionDelete);
                    assertNull(deletedItem);

                    /*
                        However, the example did not modify the <code>User</code> in this revision,
                        so Envers returns its closest historical revision.
                     */
                    User user = auditReader.find(User.class, USER_ID, revisionDelete);
                    assertEquals(user.getUsername(), "doejohn");
                }

                em.clear();

                {
                    /*
                        This query returns Item instances restricted to a particular revision and change set.
                     */
                    AuditQuery query = auditReader.createQuery()
                            .forEntitiesAtRevision(Item.class, revisionUpdate);

                    /*
                        You can add further restrictions to the query; here the Item#name must start with “Ba”.
                     */
                    query.add(
                            AuditEntity.property("name").like("Ba", MatchMode.START)
                    );

                    /*
                        Restrictions can include entity associations: for example, you’re looking for the revi-
                        sion of an Item sold by a particular User .
                     */
                    query.add(
                            AuditEntity.relatedId("seller").eq(USER_ID)
                    );

                    /*
                        You can order query results.
                     */
                    query.addOrder(
                            AuditEntity.property("name").desc()
                    );

                    /*
                        You can paginate through large results.
                     */
                    query.setFirstResult(0);
                    query.setMaxResults(10);

                    assertEquals(query.getResultList().size(), 1);
                    Item result = (Item) query.getResultList().get(0);
                    assertEquals(result.getSeller().getUsername(), "doejohn");
                }
                em.clear();
                {
                    AuditQuery query = auditReader.createQuery()
                            .forEntitiesAtRevision(Item.class, revisionUpdate);

                    query.addProjection(
                            AuditEntity.property("name")
                    );

                    assertEquals(query.getResultList().size(), 1);
                    String result = (String) query.getSingleResult();
                    assertEquals(result, "Bar");
                }
                em.clear();
                {
                    User user = auditReader.find(User.class, USER_ID, revisionCreate);

                    em.unwrap(Session.class)
                            .replicate(user, ReplicationMode.OVERWRITE);
                    em.flush();
                    em.clear();

                    user = em.find(User.class, USER_ID);
                    assertEquals(user.getUsername(), "johndoe");

                }
                tx.commit();
                em.close();
            }
        } finally {
            tx.rollback();
        }
    }
}