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

/** FR-4 Membership creation, including FR-2 eligibility at service level. */
@DisplayName("FR-4 Membership creation")
class MembershipCreationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 12, 1);
    private MembershipService service;
    private Member student;

    @BeforeEach
    void setUp() {
        service = new MembershipService();
        student = service.registerMember("Ayşe Yılmaz", 21, "ayse@mail.com", false);
    }

    @ParameterizedTest(name = "TC-MEM-01 {0} starting 2026-12-01 ends on {1}")
    @Tag("regression")
    @CsvSource({
            "MONTHLY,   2026-12-31",
            "QUARTERLY, 2027-02-28",
            "ANNUAL,    2027-11-30"})
    void endDateIsLastDayOfPeriod(MembershipType type, LocalDate expectedEnd) {
        Membership m = service.createMembership(student.getId(), type, MemberCategory.STANDARD, TODAY, TODAY);
        assertEquals(expectedEnd, m.getEndDate());
    }

    @Test
    @DisplayName("TC-MEM-02 New membership stores fee, plan and ACTIVE status")
    void createdMembershipHasCorrectState() {
        Membership m = service.createMembership(student.getId(), MembershipType.ANNUAL,
                MemberCategory.STUDENT, TODAY, TODAY);
        assertAll(
                () -> assertEquals("S0001", m.getId()),
                () -> assertEquals(new BigDecimal("7680.00"), m.getFee()),
                () -> assertEquals(MembershipStatus.ACTIVE, m.getStatus(TODAY)),
                () -> assertEquals(60, m.getRemainingFreezeDays()),
                () -> assertSame(student, m.getMember()),
                () -> assertSame(m, service.findMembership("S0001")));
    }

    @ParameterizedTest(name = "TC-MEM-03 BVA start date offset {0} days -> valid={1}")
    @CsvSource({"-1, false", "0, true", "1, true"})
    void startDateBoundaries(int offset, boolean valid) {
        LocalDate start = TODAY.plusDays(offset);
        if (valid) {
            assertNotNull(service.createMembership(student.getId(), MembershipType.MONTHLY,
                    MemberCategory.STANDARD, start, TODAY));
        } else {
            assertThrows(IllegalArgumentException.class, () -> service.createMembership(student.getId(),
                    MembershipType.MONTHLY, MemberCategory.STANDARD, start, TODAY));
        }
    }

    @Test
    @DisplayName("TC-MEM-04 Negative: ineligible category (STUDENT at age 26) is rejected")
    void ineligibleCategoryRejected() {
        Member adult = service.registerMember("Mehmet Kaya", 26, "mehmet@mail.com", false);
        assertThrows(IllegalArgumentException.class, () -> service.createMembership(adult.getId(),
                MembershipType.MONTHLY, MemberCategory.STUDENT, TODAY, TODAY));
    }

    @Test
    @DisplayName("TC-MEM-05 SENIOR category is accepted at age 65")
    void seniorAccepted() {
        Member senior = service.registerMember("Fatma Demir", 65, "fatma@mail.com", false);
        Membership m = service.createMembership(senior.getId(), MembershipType.MONTHLY,
                MemberCategory.SENIOR, TODAY, TODAY);
        assertEquals(new BigDecimal("700.00"), m.getFee());
    }

    @Test
    @DisplayName("TC-MEM-06 Negative: second membership while one is active is rejected")
    void secondActiveMembershipRejected() {
        Membership first = service.createMembership(student.getId(), MembershipType.MONTHLY,
                MemberCategory.STANDARD, TODAY, TODAY);
        LocalDate lastDay = first.getEndDate();
        assertThrows(IllegalStateException.class, () -> service.createMembership(student.getId(),
                MembershipType.MONTHLY, MemberCategory.STANDARD, lastDay, lastDay));
    }

    @Test
    @DisplayName("TC-MEM-07 New membership allowed the day after the previous one expired")
    void newMembershipAfterExpiry() {
        Membership first = service.createMembership(student.getId(), MembershipType.MONTHLY,
                MemberCategory.STANDARD, TODAY, TODAY);
        LocalDate dayAfter = first.getEndDate().plusDays(1);
        Membership second = service.createMembership(student.getId(), MembershipType.QUARTERLY,
                MemberCategory.STANDARD, dayAfter, dayAfter);
        assertEquals(MembershipStatus.EXPIRED, first.getStatus(dayAfter));
        assertEquals(2, service.getMembershipsOf(student.getId()).size());
        assertEquals(MembershipStatus.ACTIVE, second.getStatus(dayAfter));
    }

    @Test
    @DisplayName("TC-MEM-08 Negative: unknown member and missing arguments are rejected")
    void invalidArgumentsRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> service.createMembership("M9999",
                        MembershipType.MONTHLY, MemberCategory.STANDARD, TODAY, TODAY)),
                () -> assertThrows(IllegalArgumentException.class, () -> service.createMembership(student.getId(),
                        null, MemberCategory.STANDARD, TODAY, TODAY)),
                () -> assertThrows(IllegalArgumentException.class, () -> service.createMembership(student.getId(),
                        MembershipType.MONTHLY, null, TODAY, TODAY)),
                () -> assertThrows(IllegalArgumentException.class, () -> service.createMembership(student.getId(),
                        MembershipType.MONTHLY, MemberCategory.STANDARD, null, TODAY)),
                () -> assertThrows(IllegalArgumentException.class, () -> service.createMembership(student.getId(),
                        MembershipType.MONTHLY, MemberCategory.STANDARD, TODAY, null)));
    }
}
