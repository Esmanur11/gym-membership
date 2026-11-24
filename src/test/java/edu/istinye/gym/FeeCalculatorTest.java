package edu.istinye.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** FR-2 category eligibility, FR-3 fee calculation, FR-6 renewal discount. */
@DisplayName("FR-2/FR-3 Fees, discounts and category eligibility")
class FeeCalculatorTest {

    private final FeeCalculator calculator = new FeeCalculator();

    /**
     * Decision table for the fee. Conditions: plan, category discount, renewal discount.
     * Action: total discount = category + renewal, capped at 35%.
     */
    @ParameterizedTest(name = "TC-FEE-01 DT {0} + {1} + renewal {2}% -> {3} TL")
    @Tag("regression")
    @CsvSource({
            // plan,     category, renewal, expected fee
            "MONTHLY,   STANDARD, 0,  1000.00",
            "MONTHLY,   STUDENT,  0,  800.00",
            "MONTHLY,   SENIOR,   0,  700.00",
            "QUARTERLY, STANDARD, 0,  2700.00",
            "QUARTERLY, STUDENT,  0,  2160.00",
            "QUARTERLY, STANDARD, 5,  2565.00",
            "ANNUAL,    SENIOR,   0,  6720.00",
            "ANNUAL,    STANDARD, 10, 8640.00",
            "ANNUAL,    STUDENT,  5,  7200.00",
            "ANNUAL,    STUDENT,  10, 6720.00",
            "ANNUAL,    SENIOR,   5,  6240.00",   // 35% exactly at the cap
            "ANNUAL,    SENIOR,   10, 6240.00",   // 40% -> capped to 35%
            "MONTHLY,   SENIOR,   10, 650.00"     // 40% -> capped to 35%
    })
    void feeDecisionTable(MembershipType type, MemberCategory category, int renewal, String expected) {
        BigDecimal fee = calculator.calculateFee(type, category, renewal);
        assertEquals(new BigDecimal(expected), fee);
        assertEquals(2, fee.scale());
    }

    @ParameterizedTest(name = "TC-FEE-02 BVA/negative renewal discount {0}% is rejected")
    @ValueSource(ints = {-1, 101})
    void invalidRenewalDiscountRejected(int renewal) {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateFee(MembershipType.MONTHLY, MemberCategory.STANDARD, renewal));
    }

    @Test
    @DisplayName("TC-FEE-03 Negative: null plan or category is rejected")
    void nullArgumentsRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> calculator.calculateFee(null, MemberCategory.STANDARD, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> calculator.calculateFee(MembershipType.MONTHLY, null, 0)));
    }

    /** BVA around the 7-day early-renewal boundary; the membership ends on 2027-11-30. */
    @ParameterizedTest(name = "TC-FEE-04 BVA renew {0} -> renewal discount {1}%")
    @Tag("regression")
    @CsvSource({
            "2027-11-22, 10",   // 8 days left
            "2027-11-23, 10",   // 7 days left (boundary)
            "2027-11-24, 5",    // 6 days left
            "2027-11-30, 5",    // last day of membership
            "2027-12-01, 0"     // expired
    })
    void renewalDiscountBoundaries(LocalDate today, int expected) {
        assertEquals(expected, calculator.renewalDiscountPercent(LocalDate.of(2027, 11, 30), today));
    }

    @Test
    @DisplayName("TC-FEE-05 Negative: null dates for renewal discount are rejected")
    void renewalDiscountNullDates() {
        assertThrows(IllegalArgumentException.class, () -> calculator.renewalDiscountPercent(null, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> calculator.renewalDiscountPercent(LocalDate.now(), null));
    }

    /** FR-2 BVA for category age limits. */
    @ParameterizedTest(name = "TC-CAT-01 BVA {0} at age {1} -> eligible={2}")
    @CsvSource({
            "STUDENT,  16,  true",
            "STUDENT,  25,  true",
            "STUDENT,  26,  false",
            "SENIOR,   64,  false",
            "SENIOR,   65,  true",
            "SENIOR,   100, true",
            "STANDARD, 16,  true",
            "STANDARD, 100, true"})
    void categoryEligibility(MemberCategory category, int age, boolean eligible) {
        assertEquals(eligible, category.isEligible(age));
    }

    @ParameterizedTest(name = "TC-CAT-02 plan {0} has consistent configuration")
    @EnumSource(MembershipType.class)
    void planConfiguration(MembershipType type) {
        assertTrue(type.getMonths() > 0);
        assertTrue(type.getBasePrice().signum() > 0);
        assertEquals(type != MembershipType.MONTHLY, type.isFreezable());
    }
}
