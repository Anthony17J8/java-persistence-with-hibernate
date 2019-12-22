package com.ico.ltd.hibernateinaction2nd.domain.converters;

import com.ico.ltd.hibernateinaction2nd.domain.MonetaryAmount;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.DynamicParameterizedType;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Currency;
import java.util.Properties;

/**
 * {@link DynamicParameterizedType} gives you access
 * to dynamic information in the adapter, such as the mapped column and table
 * names. You might as well use this instead of ParameterizedUserType ; there is
 * no additional cost or complexity.
 * <p>
 * {@link CompositeUserType} This extends UserType , providing Hibernate with more
 * details about your adapted class. You can tell Hibernate that the Monetary-
 * Amount component has two properties: amount and currency . You can then ref-
 * erence these properties in queries with dot notation: for example, select
 * avg(i.buyNowPrice.amount) from Item i .
 */
public class MonetaryAmountUserType implements CompositeUserType, DynamicParameterizedType {

    protected Currency convertTo;

    /**
     * You can access some dynamic parameters here, such as the name of the mapped col-
     * umns, the mapped (entity) table, or even the annotations on the field/getter of the
     * mapped property. You don’t need them in this example, though.
     */
    @Override
    public void setParameterValues(Properties parameters) {
        ParameterType parameterType = (ParameterType) parameters.get(PARAMETER_TYPE);
        String[] columns = parameterType.getColumns();
        String table = parameterType.getTable();
        Annotation[] annotations = parameterType.getAnnotationsMethod();

        /**
         * You only use the convertTo parameter to determine the target currency when saving a
         * value into the database. If the parameter hasn’t been set, default to US dollars.
         */
        String convertToParameter = parameters.getProperty("convertTo");
        this.convertTo = Currency.getInstance(convertToParameter != null ? convertToParameter : "USD");
    }

    /**
     * The method <code>returnedClass</code> adapts the given class, in this case
     * <code>MonetaryAmount</code>.
     */
    @Override
    public Class returnedClass() {
        return MonetaryAmount.class;
    }

    /**
     * If Hibernate has to make a copy of the value, it will call
     * this method. For simple immutable classes like <code>MonetaryAmount</code>,
     * you can return the given instance.
     */
    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    /**
     * Hibernate can enable some optimizations if it knows
     * that <code>MonetaryAmount</code> is immutable.
     */
    @Override
    public boolean isMutable() {
        return false;
    }

    /**
     * Hibernate will use value equality to determine whether the value
     * was changed, and the database needs to be updated. We rely on the equality
     * routine we have already written on the <code>MonetaryAmount</code> class.
     */
    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y || !(x == null || y == null) && x.equals(y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    /**
     * Hibernate calls <code>disassemble</code> when it stores a value in the global shared second-level
     * cache. You need to return a <code>Serializable</code> representation. For <code>MonetaryAmount</code>,
     * a <code>String</code> representation is an easy solution. Or, because <code>MonetaryAmount</code> is actually
     * <code>Serializable</code>, you could return it directly.
     */
    @Override
    public Serializable disassemble(Object value, SharedSessionContractImplementor session) throws HibernateException {
        return value.toString();
    }


    /**
     * Hibernate calls this method when it reads the serialized
     * representation from the global shared second-level cache. We create a
     * <code>MonetaryAmount</code> instance from the <code>String</code>
     * representation. Or, if have stored a serialized <code>MonetaryAmount</code>,
     * you could return it directly.
     */
    @Override
    public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
        return MonetaryAmount.fromString((String) cached);
    }

    /**
     * Called during <code>EntityManager#merge()</code> operations, you
     * need to return a copy of the <code>original</code>. Or, if your value type is
     * immutable, like <code>MonetaryAmount</code>, you can simply return the original.
     */
    @Override
    public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner) throws HibernateException {
        return original;
    }

    /**
     * This is called to read the ResultSet when a MonetaryAmount value has to be retrieved
     * from the database. You take the amount and currency values as given in the query
     * result and create a new instance of MonetaryAmount .
     */
    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        BigDecimal amount = rs.getBigDecimal(names[0]);
        if (rs.wasNull()) {
            return null;
        }
        Currency currency = Currency.getInstance(rs.getString(names[1]));

        return new MonetaryAmount(amount, currency);
    }

    /**
     * This is called when a MonetaryAmount value has to be stored in the database. You con-
     * vert the value to the target currency and then set the amount and currency on the pro-
     * vided PreparedStatement (unless MonetaryAmount was null , in which case you call
     * setNull() to prepare the statement).
     */
    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {

        if (value == null) {
            st.setNull(index, StandardBasicTypes.BIG_DECIMAL.sqlType());
            st.setNull(index + 1, StandardBasicTypes.CURRENCY.sqlType());
        } else {
            MonetaryAmount amount = (MonetaryAmount) value;
            MonetaryAmount dbAmount = convert(amount, convertTo);
            st.setBigDecimal(index, dbAmount.getValue());
            st.setString(index + 1, convertTo.getCurrencyCode());
        }
    }

    /**
     * Here you can implement whatever currency conversion routine you need. For the
     * sake of the example, you double the value so you can easily test whether conversion
     * was successful. You’ll have to replace this code with a real currency converter in a real
     * application. It’s not a method of the Hibernate UserType API .
     */
    protected MonetaryAmount convert(MonetaryAmount amount, Currency toCurrency) {
        return new MonetaryAmount(
                amount.getValue().multiply(new BigDecimal(2)),
                toCurrency);
    }

    /**
     * Finally, following are the methods required by the CompositeUserType interface, pro-
     * viding the details of the MonetaryAmount properties so Hibernate can integrate the
     * class with the query engine
     */
    @Override
    public String[] getPropertyNames() {
        return new String[]{"value", "currency"};
    }

    @Override
    public Type[] getPropertyTypes() {
        return new Type[]{
                StandardBasicTypes.BIG_DECIMAL,
                StandardBasicTypes.CURRENCY
        };
    }

    @Override
    public Object getPropertyValue(Object component, int property) throws HibernateException {
        MonetaryAmount monetaryAmount = (MonetaryAmount) component;
        if (property == 0) {
            return monetaryAmount.getValue();
        } else {
            return monetaryAmount.getCurrency();
        }
    }

    @Override
    public void setPropertyValue(Object component, int property, Object value) throws HibernateException {
        throw new UnsupportedOperationException("MonetaryAmount is immutable");
    }
}
