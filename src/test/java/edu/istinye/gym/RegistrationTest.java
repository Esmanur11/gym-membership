package edu.istinye.gym;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** FR-1 Member registration: EP, BVA, decision table (age x consent), negative tests. */
@DisplayName("FR-1 Member registration")
class RegistrationTest {

    private MembershipService service;

    @BeforeEach
    void setUp() {
        service = new MembershipService();
    }

    @Nested
    @DisplayName("Name validation")
    class NameTests {

        @Test
        @DisplayName("TC-REG-01 EP valid name with Turkish letters is accepted and trimmed")
        void validNameAccepted() {
            Member m = service.registerMember("  Ayşe Yılmaz  ", 20, "ayse@mail.com", false);
            assertAll(
                    () -> assertEquals("Ayşe Yılmaz", m.getName()),
                    () -> assertEquals(20, m.getAge()),
                    () -> assertEquals("M0001", m.getId()));
        }

        @ParameterizedTest(name = "TC-REG-02 EP invalid name [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "Ali3", "Ali_Veli", "Ali@"})
        void invalidNameRejected(String name) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.registerMember(name, 20, "x@mail.com", false));
        }

        @ParameterizedTest(name = "TC-REG-03 BVA name length {0} -> valid={1}")
        @CsvSource({"1, false", "2, true", "50, true", "51, false"})
        void nameLengthBoundaries(int length, boolean valid) {
            String name = "a".repeat(length);
            if (valid) {
                assertEquals(length, service.registerMember(name, 20, "x@mail.com", false).getName().length());
            } else {
                assertThrows(IllegalArgumentException.class,
                        () -> service.registerMember(name, 20, "x@mail.com", false));
            }
        }
    }

    @Nested
    @DisplayName("Age validation")
    class AgeTests {

        @ParameterizedTest(name = "TC-REG-04 BVA age {0} (with consent) is accepted")
        @Tag("regression")
        @ValueSource(ints = {16, 17, 18, 99, 100})
        void validAgesAccepted(int age) {
            Member m = service.registerMember("Test User", age, "t@mail.com", true);
            assertEquals(age, m.getAge());
        }

        @ParameterizedTest(name = "TC-REG-05 BVA/negative age {0} is rejected")
        @ValueSource(ints = {-1, 0, 15, 101})
        void invalidAgesRejected(int age) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.registerMember("Test User", age, "t@mail.com", true));
        }

        /**
         * Decision table: age band x guardian consent.
         *  R1 16-17 + consent    -> accept
         *  R2 16-17 + no consent -> reject
         *  R3 18+   + consent    -> accept
         *  R4 18+   + no consent -> accept
         */
        @ParameterizedTest(name = "TC-REG-06 DT age {0}, consent {1} -> accepted={2}")
        @Tag("regression")
        @CsvSource({
                "16, true,  true",
                "17, true,  true",
                "16, false, false",
                "17, false, false",
                "18, true,  true",
                "18, false, true",
                "40, false, true"})
        void guardianConsentDecisionTable(int age, boolean consent, boolean accepted) {
            if (accepted) {
                assertNotNull(service.registerMember("Test User", age, "t@mail.com", consent));
            } else {
                IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                        () -> service.registerMember("Test User", age, "t@mail.com", consent));
                assertTrue(e.getMessage().contains("guardian consent"));
            }
        }
    }

    @Nested
    @DisplayName("E-mail validation")
    class EmailTests {

        @ParameterizedTest(name = "TC-REG-07 EP valid e-mail [{0}]")
        @ValueSource(strings = {"a@b.co", "ayse.yilmaz@istinye.edu.tr", "  user@mail.com  "})
        void validEmailAccepted(String email) {
            assertNotNull(service.registerMember("Test User", 20, email, false));
        }

        @ParameterizedTest(name = "TC-REG-08 EP invalid e-mail [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"abc", "abc@", "@mail.com", "a@mail", "a b@mail.com", "a@@mail.com"})
        void invalidEmailRejected(String email) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.registerMember("Test User", 20, email, false));
        }

        @Test
        @DisplayName("TC-REG-09 Negative: duplicate e-mail is rejected")
        void duplicateEmailRejected() {
            service.registerMember("First User", 20, "same@mail.com", false);
            assertThrows(IllegalArgumentException.class,
                    () -> service.registerMember("Second User", 30, "same@mail.com", false));
        }

        @Test
        @Tag("regression")
        @DisplayName("TC-REG-10 Negative: duplicate e-mail with different letter case is rejected")
        void duplicateEmailDifferentCaseRejected() {
            service.registerMember("First User", 20, "same@mail.com", false);
            assertThrows(IllegalArgumentException.class,
                    () -> service.registerMember("Second User", 30, "SAME@Mail.com", false));
        }

        @Test
        @DisplayName("TC-REG-11 Different e-mails get sequential member ids")
        void sequentialIds() {
            Member a = service.registerMember("First User", 20, "a@mail.com", false);
            Member b = service.registerMember("Second User", 30, "b@mail.com", false);
            assertAll(
                    () -> assertEquals("M0001", a.getId()),
                    () -> assertEquals("M0002", b.getId()),
                    () -> assertSame(b, service.findMember("M0002")));
        }
    }
}
