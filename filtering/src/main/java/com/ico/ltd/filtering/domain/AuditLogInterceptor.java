package com.ico.ltd.filtering.domain;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AuditLogInterceptor extends EmptyInterceptor {

    /*
       You need to access the database to write the audit log, so this interceptor
       needs a Hibernate <code>Session</code>. You also want to store the identifier
       of the currently logged-in user in each audit log record. The <code>inserts</code>
       and <code>updates</code> instance variables are collections where this interceptor
       will hold its internal state.
     */
    protected Session currentSession;

    protected Long currentUserId;

    protected Set<Auditable> inserts = new HashSet<Auditable>();

    protected Set<Auditable> updates = new HashSet<Auditable>();

    public void setCurrentSession(Session session) {
        this.currentSession = session;
    }

    public void setCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
    }

    /*
      This method is called when an entity instance is made persistent.
    */
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (entity instanceof Auditable) {
            inserts.add((Auditable) entity);
        }
        return false; // we didn't modify the state
    }

    /*
       This method is called when an entity instance is detected as dirty
       during flushing of the persistence context.
     */
    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState,
                                Object[] previousState, String[] propertyNames, Type[] types) {

        if (entity instanceof Auditable) {
            updates.add((Auditable) entity);
        }
        return false;
    }

    /*
      This method is called after flushing of the persistence context is complete.
      Here, you write the audit log records for all insertions and updates you
      collected earlier.
     */
    @Override
    public void postFlush(Iterator entities) {

        /*
           You are not allowed to access the original persistence context, the
           <code>Session</code> that is currently executing this interceptor.
           The <code>Session</code> is in a fragile state during interceptor calls.
           Hibernate allows you to create a new <code>Session</code> that
           inherits some information from the original <code>Session</code> with
           the <code>sessionWithOptions()</code> method. Here the new temporary
           <code>Session</code> works with the same transaction and database
           connection as the original <code>Session</code>.
         */
        try (Session tempSession = currentSession
                .sessionWithOptions()
                .connection()
                .openSession()) {

            inserts.forEach(
                    entity -> tempSession.persist(new AuditLogRecord("insert", entity, currentUserId))
            );

            updates.forEach(
                    entity -> tempSession.persist(new AuditLogRecord("update", entity, currentUserId))
            );

            /*
               You flush and close the temporary <code>Session</code>
               independently from the original <code>Session</code>.
             */
            tempSession.flush();
        } finally {
            inserts.clear();
            updates.clear();
        }

    }
}
