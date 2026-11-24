package edu.istinye.gym;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calculates membership fees (FR-3) and renewal discounts (FR-6).
 */
public class FeeCalculator {

    /** Maximum total discount (category + renewal) that can be applied. */
    public static final int MAX_DISCOUNT_PERCENT = 35;
    /** Renewing at least this many days before expiry counts as an early renewal. */
    public static final int EARLY_RENEWAL_DAYS = 7;
    public static final int EARLY_RENEWAL_DISCOUNT = 10;
    public static final int LATE_RENEWAL_DISCOUNT = 5;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * Fee = base price x (100 - total discount) / 100, rounded HALF_UP to 2 decimals.
     * Total discount = category discount + renewal discount, capped at 35%.
     */
    public BigDecimal calculateFee(MembershipType type, MemberCategory category, int renewalDiscountPercent) {
        if (type == null || category == null) {
            throw new IllegalArgumentException("Membership type and category are required");
        }
        if (renewalDiscountPercent < 0 || renewalDiscountPercent > 100) {
            throw new IllegalArgumentException("Renewal discount must be between 0 and 100");
        }
        int totalDiscount = category.getDiscountPercent() + renewalDiscountPercent;
        BigDecimal multiplier = HUNDRED.subtract(BigDecimal.valueOf(totalDiscount)).divide(HUNDRED);
        return type.getBasePrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * FR-6: 10% if renewed 7 or more days before the end date,
     * 5% if renewed in the last 6 days (including the end date itself),
     * 0% if the membership has already expired.
     */
    public int renewalDiscountPercent(LocalDate currentEndDate, LocalDate today) {
        if (currentEndDate == null || today == null) {
            throw new IllegalArgumentException("Dates are required");
        }
        if (today.isAfter(currentEndDate)) {
            return 0;
        }
        long daysLeft = ChronoUnit.DAYS.between(today, currentEndDate);
        if (daysLeft > EARLY_RENEWAL_DAYS) {
            return EARLY_RENEWAL_DISCOUNT;
        }
        return LATE_RENEWAL_DISCOUNT;
    }
}
