package com.ico.ltd.hibernateinaction2nd.domain.inheritance.mixed;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.validation.constraints.NotNull;

/**
 * Remember that InheritanceType.SINGLE_TABLE enforces all columns of sub-
 * classes to be nullable. One of the benefits of this mapping is that you can now declare
 * columns of the CREDITCARD table as NOT NULL , guaranteeing data integrity.
 * At runtime, Hibernate executes an outer join to fetch BillingDetails and all sub-
 * class instances polymorphically:
 * select
 * ID, OWNER, ACCOUNT, BANKNAME, SWIFT,
 * EXPMONTH, EXPYEAR, CARDNUMBER,
 * BD_TYPE
 * from
 * BILLINGDETAILS
 * left outer join CREDITCARD on ID=CREDITCARD_ID
 * <p>
 * You can also use this trick for other subclasses in your class hierarchy. If you have an
 * exceptionally wide class hierarchy, the outer join can become a problem. Some data-
 * base systems (Oracle, for example) limit the number of tables in an outer join opera-
 * tion. For a wide hierarchy, you may want to switch to a different fetching strategy that
 * executes an immediate second SQL select instead of an outer join.
 */
@Entity
@DiscriminatorValue("CC")
@SecondaryTable(
        name = "CREDITCARD",
        pkJoinColumns = @PrimaryKeyJoinColumn(name = "CREDITCARD_ID")
)
public class CreditCard extends BillingDetails {

    @NotNull // Ignored by JPA for DDL, strategy is SINGLE_TABLE!
    @Column(table = "CREDITCARD", nullable = false) // Override the primary table
    protected String cardNumber;

    @Column(table = "CREDITCARD", nullable = false)
    protected String expMonth;

    @Column(table = "CREDITCARD", nullable = false)
    protected String expYear;

    public CreditCard() {
        super();
    }

    public CreditCard(String owner, String cardNumber, String expMonth, String expYear) {
        super(owner);
        this.cardNumber = cardNumber;
        this.expMonth = expMonth;
        this.expYear = expYear;
    }

    public Long getId() {
        return id;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpMonth() {
        return expMonth;
    }

    public void setExpMonth(String expMonth) {
        this.expMonth = expMonth;
    }

    public String getExpYear() {
        return expYear;
    }

    public void setExpYear(String expYear) {
        this.expYear = expYear;
    }
}
