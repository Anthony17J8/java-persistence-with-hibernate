package com.ico.ltd.hibernateinaction2nd.domain.tableperhierarchy;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.validation.constraints.NotNull;

/**
 * This mapping strategy is a winner in terms of both performance and simplicity. It’s the
 * best-performing way to represent polymorphism—both polymorphic and non-
 * polymorphic queries perform well—and it’s even easy to write queries by hand. Ad
 * hoc reporting is possible without complex joins or unions. Schema evolution is
 * straightforward.
 * <p>
 * There is one major problem: data integrity. You must declare columns for properties
 * declared by subclasses to be nullable. If your subclasses each define several non-
 * nullable properties, the loss of NOT NULL constraints may be a serious problem from the
 * point of view of data correctness. Imagine that an expiration date for credit cards is
 * required, but your database schema can’t enforce this rule because all columns of the
 * table can be NULL . A simple application programming error can lead to invalid data.
 * <p>
 * select
 * ID, OWNER, EXPMONTH, EXPYEAR, CARDNUMBER,
 * ACCOUNT, BANKNAME, SWIFT, BD_TYPE
 * from
 * BILLINGDETAILS
 * To query the CreditCard subclass, Hibernate adds a restriction on the discriminator
 * column:
 * <p>
 * select
 * ID, OWNER, EXPMONTH, EXPYEAR, CARDNUMBER
 * from
 * BILLINGDETAILS
 * where
 * BD_TYPE='CC'
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "BD_TYPE")
//@org.hibernate.annotations.DiscriminatorFormula(
//"case when CARDNUMBER is not null then 'CC' else 'BA' end"
//)
public abstract class BillingDetails {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    // @NotNull Ignored by Hibernate for schema generation
    @NotNull
    @Column(nullable = false)
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
