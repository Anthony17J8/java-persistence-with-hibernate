package com.ico.ltd.hibernateinaction2nd.domain.inheritance.implicitpolymorphism;

import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

/**
 * The main problem with implicit inheritance mapping is that it doesn’t support
 * polymorphic associations very well. In the database, you usually represent associations
 * as foreign key relationships. In the schema shown in figure 6.1, if the subclasses are all
 * mapped to different tables, a polymorphic association to their superclass (abstract
 * BillingDetails ) can’t be represented as a simple foreign key relationship. You can’t
 * have another entity mapped with a foreign key “referencing BILLINGDETAILS ”—there
 * is no such table. This would be problematic in the domain model, because Billing-
 * Details is associated with User ; both the CREDITCARD and BANKACCOUNT tables would
 * need a foreign key reference to the USERS table. None of these issues can be easily
 * resolved, so you should consider an alternative mapping strategy.
 * <p>
 * You could declare the identifier property in the superclass, with a shared column
 * name and generator strategy for all subclasses, so you wouldn’t have to repeat it. We
 * haven’t done this in the examples to show you that it’s optional.
 * <p>
 * Polymorphic queries that return instances of all classes that match the interface of
 * the queried class are also problematic. Hibernate must execute a query against the
 * superclass as several SQL SELECT s, one for each concrete subclass. The JPA query
 * <p>
 * select bd from BillingDetails bd requires two SQL statements:
 * <p>
 * select
 * ID, OWNER, ACCOUNT, BANKNAME, SWIFT
 * from
 * BANKACCOUNT
 * select
 * ID, CC_OWNER, CARDNUMBER, EXPMONTH, EXPYEAR
 * from
 * CREDITCARD
 */
@MappedSuperclass
public abstract class BillingDetails {

    @NotNull
    protected String owner;

    public BillingDetails() {
    }

    public BillingDetails(String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
