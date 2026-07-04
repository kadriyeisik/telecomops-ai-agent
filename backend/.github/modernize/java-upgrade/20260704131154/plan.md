# Upgrade Plan: pia-ai-agent (20260704131154)

- **Generated**: 2026-07-04 13:11:54
- **HEAD Branch**: N/A
- **HEAD Commit ID**: N/A

> ⚠️ Note: This project is **not a Git repository**. All changes will remain uncommitted in the working directory and are not version-controlled during this upgrade.

---

## Available Tools

**JDKs**
- JDK 11.0.16.1: `C:\Program Files\Microsoft\jdk-11.0.16.101-hotspot\bin` (unused)
- JDK 17: not available (baseline will be skipped)
- JDK 21.0.10: `C:\Program Files\Android\Android Studio\jbr\bin` (target JDK — used by Steps 3–5)
- JDK 25.0.2: `C:\Program Files\Java\jdk-25.0.2\bin` (available but not targeted)

**Build Tools**
- Maven 3.9.16: `C:\tools\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin` (compatible with Java 21)
- Maven Wrapper: not present (system Maven used directly)

---

## Guidelines

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

---

## Options

- Working branch: N/A (not a Git repository)
- Run tests before and after the upgrade: true

---

## Upgrade Goals

| Goal | Current | Target |
|------|---------|--------|
| Java (JDK) | 17 | **21** (LTS) |

---

## Technology Stack

| Technology/Dependency | Current | Min Compatible | Why Incompatible |
|-----------------------|---------|----------------|------------------|
| Java | 17 | 21 | User requested — upgrade to LTS 21 |
| Spring Boot | 3.3.4 | 3.3.4 | No change — fully supports Java 21 |
| Lombok | 1.18.34 (BOM) | 1.18.20 | Compatible with Java 21; no change needed |
| H2 Database | 2.2.x (BOM) | 2.2.x | Compatible; no change needed |
| maven-compiler-plugin | 3.13.0 (BOM) | 3.11.0 | Compatible with Java 21 |
| maven-surefire-plugin | 3.1.2 (BOM) | 3.0.0 | Compatible with Java 21 |
| Maven | 3.9.16 | 3.9.0 | Compatible; no change needed |

---

## Derived Upgrades

No additional upgrades are required beyond the Java version bump:

- **Spring Boot 3.3.4 → no change**: Officially supports Java 17, 21, 22, and 23; no breaking changes for this project upgrading to Java 21.
- **No `javax.*` → `jakarta.*` migration**: Already using Jakarta EE 10 via Spring Boot 3.x.
- **No build tool upgrade**: Maven 3.9.16 fully supports Java 21.

---

## Impact Analysis

### Dependency Changes

| File | Dependency | Current | Action | Target | Reason |
|------|-----------|---------|--------|--------|--------|
| `backend/pom.xml` | `java.version` property | `17` | upgrade | `21` | User requested — Java 21 LTS target |

### Source Code Changes

No source code changes required. The codebase does not use:
- Internal JDK APIs (`sun.*`, `jdk.internal.*`)
- Removed or deprecated APIs between Java 17 and 21
- `setAccessible` on JDK classes
- Serialization assumptions changed between 17 and 21

### Configuration Changes

No configuration changes required. All `application.properties` settings are framework-level and unaffected by the JDK version bump.

### CI/CD Changes

No CI/CD files detected in the project. No changes needed.

### Risks & Warnings

- **Lombok annotation processing**: Lombok 1.18.34 (managed by Spring Boot 3.3.4 BOM) fully supports Java 21. No risk.
- **JDK not in JAVA_HOME**: JAVA_HOME points to Java 21 (Android Studio JBR). Maven will use this JDK by default — this is the exact target version. **No JAVA_HOME change is needed.**
- **No baseline available**: JDK 17 is not installed on this system; the pre-upgrade baseline step will be skipped. Upgrade success is validated against a clean compile + test run with JDK 21.

---

## Upgrade Steps

- **Step 1: Setup Environment**
  - **Rationale**: Confirm JDK 21 is available and Maven can use it. JDK 17 is absent so baseline is skipped.
  - **Changes to Make**: No installation needed — JDK 21.0.10 already present. Verify `JAVA_HOME` and `mvn -version` report Java 21.
  - **Verification**: `mvn -version`, JDK: `C:\Program Files\Android\Android Studio\jbr\bin`, Expected: Java 21.x reported

- **Step 2: Setup Baseline**
  - **Rationale**: Capture pre-upgrade state for acceptance criteria comparison.
  - **Changes to Make**: None.
  - **Verification**: **SKIPPED** — JDK 17 not available on this machine. Upgrade will be validated against clean compile + full test pass with JDK 21.

- **Step 3: Upgrade java.version to 21**
  - **Rationale**: Single property controls source/target/release for both the compiler plugin and the Spring Boot parent POM. Changing it to 21 enables all Java 21 language features and runtime APIs.
  - **Changes to Make**: See *Dependency Changes* — set `<java.version>21</java.version>` in `backend/pom.xml`.
  - **Verification**: `mvn clean test-compile -q` (run from `backend/`), JDK: Java 21, Expected: BUILD SUCCESS

- **Step 4: CVE Validation & Fix**
  - **Rationale**: Scan current direct dependencies for known CVEs; patch any findings.
  - **Changes to Make**: Determined after scan results.
  - **Verification**: Re-scan after fixes, `mvn clean test-compile -q`, Expected: no open CVEs with available patches, BUILD SUCCESS

- **Step 5: Final Validation**
  - **Rationale**: Confirm all upgrade goals are met, full test suite passes, no deferred work remains.
  - **Changes to Make**: Fix any remaining test failures from Steps 3–4.
  - **Verification**: `mvn clean test` (run from `backend/`), JDK: Java 21, Expected: all tests pass, BUILD SUCCESS
