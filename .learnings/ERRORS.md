# Errors

Command failures and integration errors.

---

## [ERR-20260902-002] maven-dependency-tree

**Logged**: 2026-09-02T10:46:22+08:00
**Priority**: low
**Status**: resolved
**Area**: config

### Summary
Maven could not write update-tracking files under the sandboxed local repository while resolving optional plugin descriptors.

### Error
```
FileSystemException: /Users/mario/.m2/repository/...: Operation not permitted
```

### Context
- Command attempted: `mvn dependency:tree -Dincludes=org.springframework.ai -Dverbose`
- Maven still completed successfully and printed the requested dependency tree.
- The later verification used Maven offline mode to avoid repository writes.

### Suggested Fix
Use `mvn -o` when all required artifacts are already cached and network or local-repository writes are unnecessary.

### Metadata
- Reproducible: yes
- Related Files: pom.xml

### Resolution
- **Resolved**: 2026-09-02T10:48:25+08:00
- **Notes**: Re-ran the context test in offline mode successfully.

---

## [ERR-20260902-001] git-status

**Logged**: 2026-09-02T10:45:46+08:00
**Priority**: low
**Status**: resolved
**Area**: config

### Summary
Git status check failed because the workspace is not a Git repository.

### Error
```
fatal: not a git repository (or any of the parent directories): .git
```

### Context
- Command attempted: `git status --short --branch`
- Working directory: project workspace root
- The Git check was optional and did not block the Spring configuration diagnosis.

### Suggested Fix
Only run Git-specific checks after verifying that `.git` exists.

### Metadata
- Reproducible: yes
- Related Files: none

### Resolution
- **Resolved**: 2026-09-02T10:45:46+08:00
- **Notes**: Continued with filesystem and Maven inspection; no project fix was required.

---

## [ERR-20260902-003] stale-surefire-report

**Logged**: 2026-09-02T10:54:59+08:00
**Priority**: low
**Status**: resolved
**Area**: tests

### Summary
A previously generated Surefire XML report disappeared after the IDE rebuilt the project.

### Error
```
No such file or directory: target/surefire-reports/TEST-cn.mario.aiplatform.AiPlatformApplicationTests.xml
```

### Context
- Attempted to inspect a generated test classpath as secondary evidence.
- Files under `target/` are ephemeral and may change while the user rebuilds or runs the application.
- The current Maven dependency tree independently established the relevant runtime dependencies.

### Suggested Fix
Do not rely on stale `target/` reports; regenerate them or inspect the current Maven dependency tree.

### Metadata
- Reproducible: unknown
- Related Files: pom.xml

### Resolution
- **Resolved**: 2026-09-02T10:54:59+08:00
- **Notes**: Used the live offline Maven dependency tree instead.

---
