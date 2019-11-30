package com.ico.ltd.hibernateinaction2nd.domain;

import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import java.util.Date;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ItemValidatorTest {

    @Test
    public void validateItem() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Item item = new Item();
        item.setName("Some Item");
        item.setDescription("Description");
        item.setAuctionEnd(new Date());

        Set<ConstraintViolation<Item>> violations = validator.validate(item);
        assertEquals(1, violations.size());

        ConstraintViolation<Item> violation = violations.iterator().next();

        String failedPropertyName =
                violation.getPropertyPath().iterator().next().getName();

        assertEquals(failedPropertyName, "auctionEnd");
        if (Locale.getDefault().getLanguage().equals("en"))
            assertEquals(violation.getMessage(), "must be in the future");
    }

}