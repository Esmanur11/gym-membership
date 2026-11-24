# Gym Membership Management System

SWE411 Software Verification and Validation – SVV Research Assignment
Esmanur Tekin (220911789)

A Java console application that manages gym memberships (registration, fees,
freezing, renewal), tested with JUnit 5 and measured with JaCoCo.

## Requirements
- Java 17 or newer
- Maven 3.8+ (IntelliJ IDEA also works: open `pom.xml` as a project)

## Commands
| What | Command |
| --- | --- |
| Run all tests + coverage | `mvn test` |
| Run only the regression subset | `mvn test -Dgroups=regression` |
| Coverage report | open `target/site/jacoco/index.html` after `mvn test` |
| Run the console demo | `mvn compile` then `java -cp target/classes edu.istinye.gym.App` |

## Folder structure
- `src/main/java/edu/istinye/gym/` – application (8 classes)
- `src/test/java/edu/istinye/gym/` – JUnit 5 tests (5 classes, 47 test methods, 119 executions)
- `test-results/v1.0/` – results on the first version (95 passed, 17 failed) and its coverage
- `test-results/v1.1/` – results after fixing D1–D7 (112 passed, original suite)
- `test-results/v1.1-new-tests/` – extended suite on v1.1 (113 passed, 5 failed: defects D8–D10)
- `test-results/v1.2/` – results after fixing D8–D10 (118 passed) and coverage report (`coverage-html/index.html`)
- `test-results/v1.2-regression-tag/` – the regression-tagged subset on v1.2 (47 passed)
- `test-results/v1.3/` – final results after fixing D11 (all defects D1–D11 fixed) (119 passed, `console-output.txt`); the regression subset now has 48 tests

## Versions (Git tags)
- `v1.0` – first implementation with seven deliberately seeded defects D1–D7 (fault seeding)
- `v1.1` – D1–D7 fixed; `git diff v1.0 v1.1 -- src/main` shows the seven fixes
- `v1.2` – D8–D10 (found by review, confirmed by new tests) fixed; `git diff v1.1 v1.2 -- src/main`
- `v1.3` – D11 – renewal of a NOT_STARTED membership – fixed; `git diff v1.2 v1.3 -- src/main`
