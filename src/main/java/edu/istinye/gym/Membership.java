package edu.istinye.gym;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One membership period of a member. The status is not stored; it is
 * derived from the dates so that it is always consistent (FR-4, FR-5).
 */
public class Membership {
    private final String id;
    private final Member member;
    private final MembershipType type;
    private final MemberCategory category;
    private final LocalDate startDate;
    private LocalDate endDate;
    private final BigDecimal fee;
    private int usedFreezeDays;
    private LocalDate frozenFrom;
    private LocalDate frozenUntil;

    Membership(String id, Member member, MembershipType type, MemberCategory category,
               LocalDate startDate, LocalDate endDate, BigDecimal fee) {
        this.id = id;
        this.member = member;
        this.type = type;
        this.category = category;
        this.startDate = startDate;
        this.endDate = endDate;
        this.fee = fee;
    }

    /** Returns the status of this membership on the given date. */
    public MembershipStatus getStatus(LocalDate date) {
        if (date.isAfter(endDate)) {
            return MembershipStatus.EXPIRED;
        }
        if (date.isBefore(startDate)) {
            return MembershipStatus.NOT_STARTED;
        }
        if (frozenUntil != null && !date.isBefore(frozenFrom) && !date.isAfter(frozenUntil)) {
            return MembershipStatus.FROZEN;
        }
        return MembershipStatus.ACTIVE;
    }

    void applyFreeze(LocalDate from, int days) {
        this.frozenFrom = from;
        this.frozenUntil = from.plusDays(days - 1L);
        this.endDate = endDate.plusDays(days);
        this.usedFreezeDays += days;
    }

    public String getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public MembershipType getType() {
        return type;
    }

    public MemberCategory getCategory() {
        return category;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public int getUsedFreezeDays() {
        return usedFreezeDays;
    }

    public int getRemainingFreezeDays() {
        return type.getMaxFreezeDays() - usedFreezeDays;
    }

    public LocalDate getFrozenUntil() {
        return frozenUntil;
    }
}
