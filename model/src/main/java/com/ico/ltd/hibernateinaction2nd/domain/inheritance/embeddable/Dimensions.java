package com.ico.ltd.hibernateinaction2nd.domain.inheritance.embeddable;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Without this override, an Item embedding both Dimension and Weight would map to
 * a table with conflicting column names.
 */
@Embeddable
@AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "DIMENSIONS_NAME")),
        @AttributeOverride(name = "symbol", column = @Column(name = "DIMENSIONS_SYMBOL"))
})
public class Dimensions extends Measurement {

    @NotNull
    protected BigDecimal depth;

    @NotNull
    protected BigDecimal height;

    @NotNull
    protected BigDecimal width;

    public Dimensions() {
    }

    public BigDecimal getDepth() {
        return depth;
    }

    public void setDepth(BigDecimal depth) {
        this.depth = depth;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public void setWidth(BigDecimal width) {
        this.width = width;
    }
}
