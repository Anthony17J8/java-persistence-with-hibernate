package com.ico.ltd.hibernateinaction2nd.domain.joined;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.validation.constraints.NotNull;

/**
 * The primary key columns of the BANKACCOUNT and CREDITCARD tables each also have a
 * foreign key constraint referencing the primary key of the BILLINGDETAILS table.
 * Hibernate relies on an SQL outer join for select bd from BillingDetails bd :
 * select
 * BD.ID, BD.OWNER,
 * CC.EXPMONTH, CC.EXPYEAR, CC.CARDNUMBER,
 * BA.ACCOUNT, BA.BANKNAME, BA.SWIFT,
 * case
 * when CC.CREDITCARD_ID is not null then 1
 * when BA.ID is not null then 2
 * when BD.ID is not null then 0
 * end
 * from
 * BILLINGDETAILS BD
 * left outer join CREDITCARD CC on BD.ID=CC.CREDITCARD_ID
 * left outer join BANKACCOUNT BA on BD.ID=BA.ID
 * <p>
 * For a narrow subclass query like select cc from CreditCard cc , Hibernate uses
 * an inner join:
 * select
 * CREDITCARD_ID, OWNER, EXPMONTH, EXPYEAR, CARDNUMBER
 * from
 * CREDITCARD
 * inner join BILLINGDETAILS on CREDITCARD_ID=ID
 * <p>
 * As you can see, this mapping strategy is more difficult to implement by hand—even ad
 * hoc reporting is more complex. This is an important consideration if you plan to mix
 * Hibernate code with handwritten SQL .
 * Furthermore, even though this mapping strategy is deceptively simple, our experi-
 * ence is that performance can be unacceptable for complex class hierarchies. Queries
 * always require a join across many tables, or many sequential reads.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
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
