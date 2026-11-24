package edu.istinye.gym;

import java.math.BigDecimal;

/**
 * Membership plans offered by the gym.
 * Each plan has a duration, a base price (TL) and a maximum number of freeze days
 * that can be used within one membership period (FR-5).
 */
public enum MembershipType {
    MONTHLY(1, new BigDecimal("1000.00"), 0),
    QUARTERLY(3, new BigDecimal("2700.00"), 15),
    ANNUAL(12, new BigDecimal("9600.00"), 60);

    private final int months;
    private final BigDecimal basePrice;
    private final int maxFreezeDays;

    MembershipType(int months, BigDecimal basePrice, int maxFreezeDays) {
        this.months = months;
        this.basePrice = basePrice;
        this.maxFreezeDays = maxFreezeDays;
    }

    public int getMonths() {
        return months;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public int getMaxFreezeDays() {
        return maxFreezeDays;
    }

    public boolean isFreezable() {
        return maxFreezeDays > 0;
    }
}
