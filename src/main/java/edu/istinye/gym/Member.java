package edu.istinye.gym;

/**
 * A registered gym member. Instances are created only through
 * {@link MembershipService#registerMember}, which validates the data.
 */
public class Member {
    private final String id;
    private final String name;
    private final int age;
    private final String email;
    private final boolean guardianConsent;

    Member(String id, String name, int age, String email, boolean guardianConsent) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.email = email;
        this.guardianConsent = guardianConsent;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }

    public boolean hasGuardianConsent() {
        return guardianConsent;
    }

    @Override
    public String toString() {
        return id + " " + name + " (" + age + ")";
    }
}
