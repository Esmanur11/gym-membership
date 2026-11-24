package edu.istinye.gym;

import java.time.LocalDate;

/**
 * Small console demo of the main scenarios. Run with:
 * mvn -q compile exec:java  (or)  java -cp target/classes edu.istinye.gym.App
 */
public class App {

    public static void main(String[] args) {
        MembershipService service = new MembershipService();
        LocalDate today = LocalDate.of(2026, 12, 1);

        Member ayse = service.registerMember("Ayşe Yılmaz", 21, "ayse@example.com", false);
        Membership annual = service.createMembership(ayse.getId(), MembershipType.ANNUAL,
                MemberCategory.STUDENT, today, today);
        print("Created", annual, today);

        LocalDate freezeDay = today.plusMonths(2);
        service.freezeMembership(annual.getId(), 30, freezeDay);
        print("Frozen for 30 days", annual, freezeDay);

        LocalDate renewalDay = annual.getEndDate().minusDays(10);
        Membership renewed = service.renewMembership(annual.getId(), MembershipType.QUARTERLY, renewalDay);
        print("Renewed early", renewed, renewalDay);

        try {
            service.registerMember("Can", 15, "can@example.com", true);
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected registration: " + e.getMessage());
        }
    }

    private static void print(String action, Membership m, LocalDate date) {
        System.out.printf("%-20s %s %s/%s %s -> %s, fee %s TL, status %s%n", action, m.getId(),
                m.getType(), m.getCategory(), m.getStartDate(), m.getEndDate(), m.getFee(), m.getStatus(date));
    }
}
