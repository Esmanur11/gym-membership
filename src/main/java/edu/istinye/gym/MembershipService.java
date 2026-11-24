package edu.istinye.gym;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Main service of the application. Applies all business rules for
 * registration (FR-1), category eligibility (FR-2), membership creation (FR-4),
 * freezing (FR-5) and renewal (FR-6). Invalid input is rejected with
 * IllegalArgumentException; operations not allowed in the current state are
 * rejected with IllegalStateException (FR-7).
 *
 * "today" is passed as a parameter instead of calling LocalDate.now(), so the
 * behaviour is deterministic and testable.
 */
public class MembershipService {

    public static final int MIN_AGE = 16;
    public static final int ADULT_AGE = 18;
    public static final int MAX_AGE = 100;
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 50;
    public static final int MIN_FREEZE_DAYS = 7;

    private static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L} ]+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Map<String, Member> members = new LinkedHashMap<>();
    private final Map<String, Membership> memberships = new LinkedHashMap<>();
    private final FeeCalculator feeCalculator = new FeeCalculator();
    private int memberCounter = 0;
    private int membershipCounter = 0;

    // ---------------------------------------------------------------- FR-1

    public Member registerMember(String name, int age, String email, boolean guardianConsent) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        String cleanName = name.trim();
        if (cleanName.length() < NAME_MIN_LENGTH || cleanName.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Name must be 2-50 characters");
        }
        if (!NAME_PATTERN.matcher(cleanName).matches()) {
            throw new IllegalArgumentException("Name may contain only letters and spaces");
        }
        if (age < MIN_AGE) {
            throw new IllegalArgumentException("Member must be at least 16 years old");
        }
        if (age > MAX_AGE) {
            throw new IllegalArgumentException("Age must not exceed 100");
        }
        if (age < ADULT_AGE && !guardianConsent) {
            throw new IllegalArgumentException("Members under 18 need guardian consent");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid e-mail address");
        }
        String cleanEmail = email.trim();
        for (Member m : members.values()) {
            if (m.getEmail().equalsIgnoreCase(cleanEmail)) {
                throw new IllegalArgumentException("E-mail is already registered");
            }
        }
        memberCounter++;
        String id = String.format("M%04d", memberCounter);
        Member member = new Member(id, cleanName, age, cleanEmail, guardianConsent);
        members.put(id, member);
        return member;
    }

    // ---------------------------------------------------------------- FR-4

    public Membership createMembership(String memberId, MembershipType type, MemberCategory category,
                                       LocalDate startDate, LocalDate today) {
        Member member = requireMember(memberId);
        if (type == null || category == null || startDate == null || today == null) {
            throw new IllegalArgumentException("Type, category and dates are required");
        }
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        checkEligibility(member, category);
        for (Membership existing : getMembershipsOf(memberId)) {
            if (existing.getStatus(today) != MembershipStatus.EXPIRED) {
                throw new IllegalStateException("Member already has an active membership");
            }
        }
        BigDecimal fee = feeCalculator.calculateFee(type, category, 0);
        return addMembership(member, type, category, startDate, fee);
    }

    // ---------------------------------------------------------------- FR-5

    public void freezeMembership(String membershipId, int days, LocalDate today) {
        Membership membership = requireMembership(membershipId);
        if (today == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (days < MIN_FREEZE_DAYS) {
            throw new IllegalArgumentException("A freeze must be at least 7 days");
        }
        if (!membership.getType().isFreezable()) {
            throw new IllegalStateException("Monthly memberships cannot be frozen");
        }
        if (today.isBefore(membership.getStartDate())) {
            throw new IllegalStateException("Membership has not started yet");
        }
        MembershipStatus status = membership.getStatus(today);
        if (status == MembershipStatus.EXPIRED) {
            throw new IllegalStateException("Expired membership cannot be frozen");
        }
        if (status == MembershipStatus.FROZEN) {
            throw new IllegalStateException("Membership is already frozen");
        }
        if (membership.getUsedFreezeDays() + days > membership.getType().getMaxFreezeDays()) {
            throw new IllegalStateException("Freeze limit exceeded");
        }
        membership.applyFreeze(today, days);
    }

    // ---------------------------------------------------------------- FR-6

    public Membership renewMembership(String membershipId, MembershipType newType, LocalDate today) {
        Membership current = requireMembership(membershipId);
        if (newType == null || today == null) {
            throw new IllegalArgumentException("Type and date are required");
        }
        MembershipStatus status = current.getStatus(today);
        if (status == MembershipStatus.FROZEN) {
            throw new IllegalStateException("Frozen membership cannot be renewed");
        }
        for (Membership other : getMembershipsOf(current.getMember().getId())) {
            if (other.getStartDate().isAfter(current.getEndDate())) {
                throw new IllegalStateException("Membership has already been renewed");
            }
        }
        checkEligibility(current.getMember(), current.getCategory());
        int renewalDiscount = feeCalculator.renewalDiscountPercent(current.getEndDate(), today);
        LocalDate newStart = (status == MembershipStatus.EXPIRED) ? today : current.getEndDate().plusDays(1);
        BigDecimal fee = feeCalculator.calculateFee(newType, current.getCategory(), renewalDiscount);
        return addMembership(current.getMember(), newType, current.getCategory(), newStart, fee);
    }

    // ---------------------------------------------------------------- queries

    public Member findMember(String memberId) {
        return members.get(memberId);
    }

    public Membership findMembership(String membershipId) {
        return memberships.get(membershipId);
    }

    public List<Membership> getMembershipsOf(String memberId) {
        List<Membership> result = new ArrayList<>();
        for (Membership m : memberships.values()) {
            if (m.getMember().getId().equals(memberId)) {
                result.add(m);
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- helpers

    private Membership addMembership(Member member, MembershipType type, MemberCategory category,
                                     LocalDate start, BigDecimal fee) {
        LocalDate end = start.plusMonths(type.getMonths()).minusDays(1);
        membershipCounter++;
        String id = String.format("S%04d", membershipCounter);
        Membership membership = new Membership(id, member, type, category, start, end, fee);
        memberships.put(id, membership);
        return membership;
    }

    private void checkEligibility(Member member, MemberCategory category) {
        if (!category.isEligible(member.getAge())) {
            throw new IllegalArgumentException(category + " category is not allowed for age " + member.getAge());
        }
    }

    private Member requireMember(String memberId) {
        Member member = members.get(memberId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + memberId);
        }
        return member;
    }

    private Membership requireMembership(String membershipId) {
        Membership membership = memberships.get(membershipId);
        if (membership == null) {
            throw new IllegalArgumentException("Membership not found: " + membershipId);
        }
        return membership;
    }
}
