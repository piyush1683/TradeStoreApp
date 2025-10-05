# TradeStoreApp

## Security Scanning

This project includes automated vulnerability scanning using OSV-Scanner in the GitHub Actions workflow.

### OSV-Scanner Configuration

The OSV-Scanner is configured to:
- Scan all Gradle dependencies for known vulnerabilities
- Generate dependency lock files for accurate scanning
- Run as a separate job in the CI/CD pipeline
- Continue the workflow even if vulnerabilities are found (for reporting purposes)

### Configuration Files

- `.github/workflows/dev-branch-build.yml` - Main workflow with OSV-Scanner integration
- `osv-scanner.toml` - OSV-Scanner configuration file
- `build.gradle` - Root Gradle file with dependency locking enabled

### Running Security Scans Locally

To generate dependency lock files locally:
```bash
./gradlew dependencies --write-locks
```

The OSV-Scanner will automatically detect and scan:
- Gradle build files (`build.gradle`)
- Gradle lock files (`gradle.lockfile`)
- Any other supported dependency files

### Vulnerability Reports

Security scan results are available in the GitHub Actions workflow logs under the "security-scan" job.
