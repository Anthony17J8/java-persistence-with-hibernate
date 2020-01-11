package com.ico.ltd.hibernateinaction2nd.domain.tableperclass;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.validation.constraints.NotNull;

/**
 * The database identifier and its mapping have to be present in the superclass, to share
 * it in all subclasses and their tables.
 * <p>
 * The advantages of this mapping strategy are clearer if we examine polymorphic
 * queries. For example, the query select bd from BillingDetails bd generates the
 * following SQL statement:
 * <p>
 * select
 * ID, OWNER, EXPMONTH, EXPYEAR, CARDNUMBER,
 * ACCOUNT, BANKNAME, SWIFT, CLAZZ_
 * from
 * ( select
 * ID, OWNER, EXPMONTH, EXPYEAR, CARDNUMBER,
 * null as ACCOUNT,
 * null as BANKNAME,
 * null as SWIFT,
 * 1 as CLAZZ_
 * from
 * CREDITCARD
 * union all
 * select
 * id, OWNER,
 * null as EXPMONTH,
 * null as EXPYEAR,
 * null as CARDNUMBER,
 * ACCOUNT, BANKNAME, SWIFT,
 * 2 as CLAZZ_
 * from
 * BANKACCOUNT
 * ) as BILLINGDETAILS
 * <p>
 * Another much more important advantage is the ability to handle polymorphic asso-
 * ciations; for example, an association mapping from User to BillingDetails would
 * now be possible. Hibernate can use a UNION query to simulate a single table as the tar-
 * get of the association mapping.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BillingDetails {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

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
