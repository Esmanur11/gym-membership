package edu.istinye.gym;

/**
 * Member categories and their discount percentages (FR-2, FR-3).
 */
public enum MemberCategory {
    STANDARD(0),
    STUDENT(20),
    SENIOR(30);

    private final int discountPercent;

    MemberCategory(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    /**
     * FR-2: STUDENT is allowed for ages 16-25, SENIOR for ages 65 and above,
     * STANDARD for every registered member.
     */
    public boolean isEligible(int age) {
        switch (this) {
            case STUDENT:
                return age >= 16 && age <= 25;
            case SENIOR:
                return age >= 65;
            default:
                return true;
        }
    }
}
