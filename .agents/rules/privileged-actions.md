# Privileged Action Execution Rules

## Typed Actions
1. All shell commands MUST be wrapped in strongly-typed `SystemAction` objects.
2. Raw command strings are strictly forbidden in business logic, UI components, or policy engine.

## Lifecycle of a System Action
- **Pre-check:** Verify capability tier (Stock, Shizuku, Root) and check safety governor veto status.
- **Snapshot:** Read and record existing system setting value to `BaselineRepository`.
- **Execution:** Execute action with explicit execution timeout (max 5000 ms).
- **Verification:** Query system setting again to confirm mutation applied cleanly.
- **Logging:** Log action, parameters, timestamp, and outcome to `ActionHistory`.
- **Rollback:** In case of failure or state exit, restore exact captured baseline value.
