# CRM Domain Expansion: Deals, Pipelines, Activities, Timeline, and Custom Fields

## Decided
In `DealServiceImpl` and `PipelineServiceImpl`, pipelines belong to `Organization` and each workspace auto-provisions a default sales pipeline with 6 standard stages (`Lead`, `Qualified`, `Proposal`, `Negotiation`, `Won`, `Lost`). Deals progress through stages with automatic `WON`/`LOST` categorization and `actualCloseDate` timestamps. In `TimelineServiceImpl`, unified relationship history combines customer milestones, linked contacts, deals, and activities into chronologically descending feeds. Dynamic metadata across `Customer`, `Contact`, `Deal`, and `Activity` utilizes Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)` to map PostgreSQL `jsonb` attributes seamlessly without schema migrations.

## Watch out
Deleting a pipeline is rejected if it is marked as the default pipeline or if it currently contains active deals. Deleting deals or activities requires `ADMIN` or `OWNER` role, while `MEMBER` can create and update them. `READ_ONLY` role cannot mutate deals, pipelines, or activities.
