package edu.istinye.gym;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FR-5 Freezing a membership.
 *
 * Decision table:
 *            | R1 | R2 | R3 | R4 | R5 | R6
 * plan freezable   | N  | Y  | Y  | Y  | Y  | Y
 * status ACTIVE    | -  | EXP| FRZ| Y  | Y  | Y
 * days >= 7        | -  | -  | -  | N  | Y  | Y
 * used+days <= max | -  | -  | -  | -  | N  | Y
 * outcome          | ISE| ISE| ISE| IAE| ISE| accept
 */
@DisplayName("FR-5 Membership freeze")
class FreezeTest {

    private static final LocalDate START = LocalDate.of(2026, 12, 1);
    private static final LocalDate DAY_10 = START.plusDays(10);
    private MembershipService service;
    private Member member;

    @BeforeEach
    void setUp() {
        service = new MembershipService();
        member = service.registerMember("Ali Veli", 30, "ali@mail.com", false);
    }

    private Membership create(MembershipType type) {
        return service.createMembership(member.getId(), type, MemberCategory.STANDARD, START, START);
    }

    @Test
    @DisplayName("TC-FRZ-01 DT R1: monthly membership cannot be frozen")
    void monthlyCannotBeFrozen() {
        Membership m = create(MembershipType.MONTHLY);
        assertThrows(IllegalStateException.class, () -> service.freezeMembership(m.getId(), 7, DAY_10));
    }

    @Test
    @DisplayName("TC-FRZ-02 DT R2: expired membership cannot be frozen")
    void expiredCannotBeFrozen() {
        Membership m = create(MembershipType.QUARTERLY);
        LocalDate afterEnd = m.getEndDate().plusDays(1);
        assertThrows(IllegalStateException.class, () -> service.freezeMembership(m.getId(), 7, afterEnd));
    }

    @Test
    @DisplayName("TC-FRZ-03 DT R3: membership that is already frozen cannot be frozen again")
    void alreadyFrozenCannotBeFrozen() {
        Membership m = create(MembershipType.ANNUAL);
        service.freezeMembership(m.getId(), 10, DAY_10);
        LocalDate stillFrozen = DAY_10.plusDays(5);
        assertThrows(IllegalStateException.class, () -> service.freezeMembership(m.getId(), 7, stillFrozen));
    }

    @ParameterizedTest(name = "TC-FRZ-04 BVA {0} freeze of {1} days -> {2}")
    @CsvSource({
            "QUARTERLY, 6,  INVALID_INPUT",
            "QUARTERLY, 7,  ACCEPTED",
            "QUARTERLY, 15, ACCEPTED",
            "QUARTERLY, 16, NOT_ALLOWED",
            "ANNUAL,    60, ACCEPTED",
            "ANNUAL,    61, NOT_ALLOWED"})
    void freezeDayBoundaries(MembershipType type, int days, String outcome) {
        Membership m = create(type);
        switch (outcome) {
            case "ACCEPTED":
                service.freezeMembership(m.getId(), days, DAY_10);
                assertEquals(days, m.getUsedFreezeDays());
                return;
            case "INVALID_INPUT":
                assertThrows(IllegalArgumentException.class, () -> service.freezeMembership(m.getId(), days, DAY_10));
                break;
            default:
                assertThrows(IllegalStateException.class, () -> service.freezeMembership(m.getId(), days, DAY_10));
        }
        assertEquals(0, m.getUsedFreezeDays());
    }

    @Test
    @DisplayName("TC-FRZ-05 DT R4: freeze shorter than 7 days is invalid input")
    void tooShortIsInvalidInput() {
        Membership m = create(MembershipType.ANNUAL);
        assertThrows(IllegalArgumentException.class, () -> service.freezeMembership(m.getId(), 6, DAY_10));
    }

    @Test
    @Tag("regression")
    @DisplayName("TC-FRZ-06 DT R5: second freeze exceeding the remaining allowance is rejected")
    void cumulativeLimitEnforced() {
        Membership m = create(MembershipType.QUARTERLY);   // max 15 days
        service.freezeMembership(m.getId(), 10, DAY_10);
        LocalDate later = DAY_10.plusDays(20);
        assertThrows(IllegalStateException.class, () -> service.freezeMembership(m.getId(), 7, later));
        assertEquals(10, m.getUsedFreezeDays());
    }

    @Test
    @DisplayName("TC-FRZ-07 Two freezes that exactly use the allowance are accepted")
    void cumulativeExactlyAtLimit() {
        Membership m = create(MembershipType.QUARTERLY);
        service.freezeMembership(m.getId(), 8, DAY_10);
        service.freezeMembership(m.getId(), 7, DAY_10.plusDays(20));
        assertEquals(15, m.getUsedFreezeDays());
        assertEquals(0, m.getRemainingFreezeDays());
    }

    @Test
    @DisplayName("TC-FRZ-08 DT R6: accepted freeze extends end date and changes status")
    void freezeEffects() {
        Membership m = create(MembershipType.ANNUAL);
        LocalDate oldEnd = m.getEndDate();
        service.freezeMembership(m.getId(), 30, DAY_10);
        LocalDate lastFrozenDay = DAY_10.plusDays(29);
        assertAll(
                () -> assertEquals(oldEnd.plusDays(30), m.getEndDate()),
                () -> assertEquals(lastFrozenDay, m.getFrozenUntil()),
                () -> assertEquals(MembershipStatus.ACTIVE, m.getStatus(DAY_10.minusDays(1))),
                () -> assertEquals(MembershipStatus.FROZEN, m.getStatus(DAY_10)),
                () -> assertEquals(MembershipStatus.FROZEN, m.getStatus(lastFrozenDay)),
                () -> assertEquals(MembershipStatus.ACTIVE, m.getStatus(lastFrozenDay.plusDays(1))),
                () -> assertEquals(30, m.getRemainingFreezeDays()));
    }

    @Test
    @DisplayName("TC-FRZ-09 Negative: membership that has not started cannot be frozen")
    void notStartedCannotBeFrozen() {
        Membership m = service.createMembership(member.getId(), MembershipType.ANNUAL,
                MemberCategory.STANDARD, START.plusDays(5), START);
        assertThrows(IllegalStateException.class, () -> service.freezeMembership(m.getId(), 7, START));
    }

    @Test
    @DisplayName("TC-FRZ-10 Negative: unknown membership id and null date are rejected")
    void invalidArguments() {
        Membership m = create(MembershipType.ANNUAL);
        assertThrows(IllegalArgumentException.class, () -> service.freezeMembership("S9999", 7, DAY_10));
        assertThrows(IllegalArgumentException.class, () -> service.freezeMembership(m.getId(), 7, null));
    }

    @Test
    @Tag("regression")
    @DisplayName("TC-FRZ-11 Negative: membership that was already renewed cannot be frozen")
    void renewedMembershipCannotBeFrozen() {
        Membership m = create(MembershipType.ANNUAL);              // 2026-12-01 .. 2027-11-30
        LocalDate renewalDay = LocalDate.of(2027, 11, 1);
        Membership next = service.renewMembership(m.getId(), MembershipType.ANNUAL, renewalDay);
        LocalDate oldEnd = m.getEndDate();
        assertThrows(IllegalStateException.class, () -> service.freezeMembership(m.getId(), 14, renewalDay.plusDays(1)));
        assertEquals(oldEnd, m.getEndDate());
        assertTrue(next.getStartDate().isAfter(m.getEndDate()), "periods must not overlap");
    }

    @ParameterizedTest(name = "TC-FRZ-12 Negative/BVA: freeze of {0} days after 10 used is rejected without overflow")
    @Tag("regression")
    @ValueSource(ints = {Integer.MAX_VALUE, Integer.MAX_VALUE - 5})
    void hugeFreezeRejected(int days) {
        Membership m = create(MembershipType.QUARTERLY);
        service.freezeMembership(m.getId(), 10, DAY_10);
        LocalDate later = DAY_10.plusDays(20);
        assertThrows(IllegalStateException.class, () -> service.freezeMembership(m.getId(), days, later));
        assertEquals(10, m.getUsedFreezeDays());
    }
}
