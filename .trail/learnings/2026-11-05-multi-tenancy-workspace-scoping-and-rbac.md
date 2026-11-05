# Multi-tenancy workspace scoping and RBAC architecture

## Decided
In `src/main/java/com/example/headlesscrm/domain/Organization.java` and `src/main/java/com/example/headlesscrm/domain/Customer.java`, customers belong to `Organization` because CRM data is shared within a team. `WorkspaceServiceImpl` resolves workspace from `X-Workspace-Id` header or falls back to user default workspace because single-workspace callers should not break.

## Watch out
In `WorkspaceSecurityService`, delete actions require ADMIN or OWNER roles because junior team members should not delete records. READ_ONLY members cannot create or mutate customers or contacts.
