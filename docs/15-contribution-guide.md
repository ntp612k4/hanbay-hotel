# 15. Contribution Guide

# HANBAY HOTEL
## Contribution Guide

---

# 1. Purpose

This document defines how team members contribute to the HANBAY HOTEL project.

---

# 2. Development Flow

```text
Pick Jira task
→ Create feature branch
→ Implement
→ Test locally
→ Create Pull Request
→ Review
→ Merge
```

---

# 3. Before Coding

- Understand the requirement.
- Check related API/document.
- Confirm unclear requirements with team leader.
- Pull latest `develop`.

---

# 4. Branch Naming

```text
feature/module-task-name
bugfix/module-bug-name
docs/document-name
```

Examples:

```text
feature/booking-create
feature/room-search
bugfix/payment-webhook
docs/business-workflows
```

---

# 5. Commit Message

Use conventional commit:

```text
feat: add booking create API
fix: handle expired pending booking
docs: update API specification
refactor: clean payment service
test: add booking unit test
```

---

# 6. Pull Request Rules

Every PR should include:

- Description
- Related task
- What changed
- How to test
- Screenshot if UI changed

---

# 7. Code Review Rules

Reviewer checks:

- Correct business logic
- Clean code
- Naming
- Exception handling
- API validation
- Security
- Testability

---

# 8. Definition of Done

A task is Done only when:

- Code completed
- Build successful
- Tested locally
- No critical bug
- PR approved
- Merged to develop
- Documentation updated if needed

---

# 9. Communication Rules

- Report blockers early.
- Do not stay silent when stuck.
- Update task status daily.
- Ask questions with context:
  - What you tried
  - What failed
  - What you need help with

---

# 10. Team Values

- Ownership
- Transparency
- Respect
- Learning mindset
- Clean delivery
