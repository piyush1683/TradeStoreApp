# Security Configuration for GitHub Actions

## Overview
This repository implements a comprehensive security scanning pipeline that includes:

1. **OWASP Dependency Check**: Scans for known vulnerabilities in dependencies
2. **Semgrep SAST**: Static Application Security Testing for code vulnerabilities  
3. **SpotBugs Security Analysis**: Java-specific security bug detection
4. **License Compliance Scanning**: Ensures license compatibility

## Vulnerability Severity Levels

The pipeline uses the following severity classification:

### CRITICAL (Score: 9.0-10.0)
- **Action**: Build FAILS immediately
- **Examples**: Remote code execution, SQL injection in core libraries
- **Response**: Must be fixed before merge

### HIGH (Score: 7.0-8.9)  
- **Action**: Build FAILS immediately
- **Response**: Should be fixed before merge, requires security review

### MEDIUM (Score: 4.0-6.9)
- **Action**: Build continues with warning
- **Response**: Fix in next release cycle

### LOW (Score: 0.1-3.9)
- **Action**: Build continues, logged for tracking
- **Response**: Fix when convenient

## Scan Tools Configuration

### 1. OWASP Dependency Check
- **Threshold**: CVSS >= 7.0 (HIGH and CRITICAL)
- **Databases**: NVD, RetireJS, NPM Audit
- **Output**: HTML, JSON, XML reports
- **Suppression**: Use `suppression.xml` for false positives

### 2. Semgrep SAST  
- **Rulesets**: 
  - `p/security-audit` - General security patterns
  - `p/owasp-top-ten` - OWASP Top 10 vulnerabilities
  - `p/java` - Java-specific security issues
- **Output**: SARIF format uploaded to GitHub Security tab

### 3. SpotBugs Security
- **Focus**: Java security-specific bug patterns
- **Patterns**: SQL injection, XSS, path traversal, weak crypto
- **Integration**: Gradle plugin with security-focused filters

## Suppressing False Positives

### OWASP Dependency Check
Edit `suppression.xml`:
```xml
<suppress>
    <notes><![CDATA[
    Reason for suppression
    ]]></notes>
    <gav regex="true">^group:artifact:version$</gav>
    <cve>CVE-YYYY-NNNN</cve>
</suppress>
```

### Semgrep
Add to `.semgrepignore`:
```
# Ignore test files for certain rules
tests/
**/test/**
```

Or use inline comments:
```java
// nosemgrep: java.lang.security.audit.crypto.weak-hash
MessageDigest md = MessageDigest.getInstance("MD5");
```

## GitHub Secrets Required

### Optional but Recommended:
- `SEMGREP_APP_TOKEN`: For Semgrep cloud features and trend analysis
- `FOSSA_API_KEY`: For advanced license compliance scanning

## Monitoring and Alerts

The workflow provides:
- **GitHub Security Alerts**: SARIF uploads enable security tab insights
- **Action Summaries**: Detailed scan results in workflow summaries  
- **Artifact Uploads**: Full reports stored for 30 days
- **PR Checks**: All scans must pass for PR approval

## Customization

### Adjusting Severity Thresholds
Modify the `--failOnCVSS` parameter in the workflow:
```yaml
args: >
  --enableRetired
  --enableExperimental  
  --failOnCVSS 8.0  # More strict (HIGH+ only)
```

### Adding Custom Rules
For Semgrep, create `.semgrep.yml`:
```yaml
rules:
  - id: custom-security-rule
    pattern: dangerous_function($X)
    message: Avoid using dangerous_function
    languages: [java]
    severity: ERROR
```

## Troubleshooting

### Common Issues:

1. **Gradle Daemon Issues**: Disabled via `GRADLE_OPTS` for CI stability
2. **Memory Issues**: Worker count limited to 2 for GitHub runners  
3. **Test Enablement**: Tests are enabled dynamically since they're disabled by default
4. **Network Timeouts**: OWASP DB updates may be slow, uses caching

### Performance Optimization:
- Gradle build cache enabled
- Parallel job execution where possible
- Artifact caching for dependencies
- Incremental analysis when supported