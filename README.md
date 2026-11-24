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
- `src/test/java/edu/istinye/gym/` – JUnit 5 tests (5 classes, 43 test cases, 112 executions)
- `test-results/v1.0/` – results on the first version (95 passed, 17 failed) and its coverage
- `test-results/v1.1/` – results after the fixes (112 passed) and its coverage report (`coverage-html/index.html`)
- `test-results/v1.1-regression-tag/` – the regression-tagged subset (41 passed)

## Versions (Git tags)
- `v1.0` – first implementation, contains defects D1–D7
- `v1.1` – defects fixed; `git diff v1.0 v1.1 -- src/main` shows the seven fixes
