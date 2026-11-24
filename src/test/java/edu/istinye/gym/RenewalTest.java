package edu.istinye.gym;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** FR-6 Membership renewal. The student's ANNUAL membership runs 2026-12-01 .. 2027-11-30. */
@DisplayName("FR-6 Membership renewal")
class RenewalTest {

    private static final LocalDate START = LocalDate.of(2026, 12, 1);
    private MembershipService service;
    private Membership annual;

    @BeforeEach
    void setUp() {
        service = new MembershipService();
        Member student = service.registerMember("Zeynep Ak", 21, "zeynep@mail.com", false);
        annual = service.createMembership(student.getId(), MembershipType.ANNUAL,
                MemberCategory.STUDENT, START, START);
    }

    /** STUDENT (20%) + renewal discount depending on the renewal date. */
    @ParameterizedTest(name = "TC-REN-01 renew on {0} -> fee {1} TL, new period starts {2}")
    @Tag("regression")
    @CsvSource({
            "2027-11-20, 6720.00, 2027-12-01",   // 10 days early: 20% + 10%
            "2027-11-23, 6720.00, 2027-12-01",   // exactly 7 days early: 20% + 10%
            "2027-11-24, 7200.00, 2027-12-01",   // 6 days early: 20% + 5%
            "2027-11-30, 7200.00, 2027-12-01",   // last day: 20% + 5%
            "2027-12-05, 7680.00, 2027-12-05"    // expired: 20% only, starts today
    })
    void renewalFeeAndStartDate(LocalDate today, String fee, LocalDate newStart) {
        Membership renewed = service.renewMembership(annual.getId(), MembershipType.ANNUAL, today);
        assertAll(
                () -> assertEquals(new BigDecimal(fee), renewed.getFee()),
                () -> assertEquals(newStart, renewed.getStartDate()),
                () -> assertEquals(MemberCategory.STUDENT, renewed.getCategory()));
    }

    @Test
    @Tag("regression")
    @DisplayName("TC-REN-02 Senior early renewal is capped at 35% total discount")
    void seniorEarlyRenewalCapped() {
        Member senior = service.registerMember("Hasan Öz", 70, "hasan@mail.com", false);
        Membership monthly = service.createMembership(senior.getId(), MembershipType.MONTHLY,
                MemberCategory.SENIOR, START, START);
        Membership renewed = service.renewMembership(monthly.getId(), MembershipType.MONTHLY,
                LocalDate.of(2026, 12, 20));
        assertEquals(new BigDecimal("650.00"), renewed.getFee());
    }

    @Test
    @DisplayName("TC-REN-03 Renewal can change the plan type")
    void renewalWithPlanChange() {
        Membership renewed = service.renewMembership(annual.getId(), MembershipType.QUARTERLY,
                LocalDate.of(2027, 11, 1));
        assertEquals(MembershipType.QUARTERLY, renewed.getType());
        assertEquals(new BigDecimal("1890.00"), renewed.getFee());
        assertEquals(2, service.getMembershipsOf(annual.getMember().getId()).size());
    }

    @Test
    @DisplayName("TC-REN-04 Negative: frozen membership cannot be renewed")
    void frozenCannotBeRenewed() {
        LocalDate freezeDay = LocalDate.of(2027, 3, 1);
        service.freezeMembership(annual.getId(), 14, freezeDay);
        assertThrows(IllegalStateException.class,
                () -> service.renewMembership(annual.getId(), MembershipType.ANNUAL, freezeDay.plusDays(3)));
    }

    @Test
    @DisplayName("TC-REN-05 Negative: a membership cannot be renewed twice")
    void cannotRenewTwice() {
        LocalDate day = LocalDate.of(2027, 11, 1);
        service.renewMembership(annual.getId(), MembershipType.ANNUAL, day);
        assertThrows(IllegalStateException.class,
                () -> service.renewMembership(annual.getId(), MembershipType.ANNUAL, day.plusDays(1)));
    }

    @Test
    @DisplayName("TC-REN-06 Negative: unknown membership id and missing arguments are rejected")
    void invalidArguments() {
        LocalDate day = LocalDate.of(2027, 11, 1);
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.renewMembership("S9999", MembershipType.ANNUAL, day)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.renewMembership(annual.getId(), null, day)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.renewMembership(annual.getId(), MembershipType.ANNUAL, null)));
    }

    @Test
    @DisplayName("TC-REN-07 Smoke: console demo runs without errors")
    void appRuns() {
        assertDoesNotThrow(() -> App.main(new String[0]));
    }
}
