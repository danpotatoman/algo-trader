# Algotrader Agent Instructions

These instructions apply only when the user requests a documentation audit or documentation-focused cleanup. They do not govern general implementation, debugging, or testing tasks.

## Project Context

- Java orchestrates trading workflows.
- Python provides prediction endpoints.
- SQLite is the authoritative OHLCV source.
- EndpointConfig replaces the older ModelConfig concept in some areas.

## Documentation Standards

- Keep Javadocs concise and accurate.
- Document responsibilities rather than implementation details.
- Do not comment simple getters.
- Keep existing TODOsAS
- Remove stale terminology.
- Mention implementation status where relevant.
- REGRESSION prediction types are placeholders and are not yet supported end-to-end.
- CLASSIFICATION and CLASSIFICATION_WITH_VOLATILITY are fully supported.

## TODO Handling

Treat existing TODO comments as authoritative project context.

Do not remove, rewrite, or mark TODOs as complete unless you can verify that the required work has been fully implemented in the current change set.

A TODO may be removed only if:

- The implementation required by the TODO is present and complete.
- The implementation has been validated through compilation and relevant tests.
- Removing the TODO does not hide remaining work.

If uncertain, preserve the TODO and mention it in the change summary.

You may add new TODOs only when:

- A limitation or follow-up task is directly introduced by your changes.
- The TODO is specific, actionable, and narrowly scoped.

Do not create speculative or low-value TODOs.

## Refactoring Permissions

Agents may modify:

- Javadocs
- Inline comments
- Private local variables
- Private fields
- Private constants
- Method parameter names

Agents must ask before modifying:

- Public method names
- Class names
- Package names
- Public field names
- JSON property names
- Configuration keys
- Serialized formats
- Database schemas
- Runtime behavior

## Audit Workflow

Before making changes, identify:

1. Stale documentation
2. Stale naming
3. Terminology inconsistencies
4. Possible API-breaking renames

Classify each finding as one of:

- Docs only
- Internal rename
- Public API rename

Do not perform public API renames without approval.

If intent is ambiguous, ask for clarification.

## Validation

Determine whether any executable code changed.

Executable code changes include:

- Method bodies
- Signatures
- Class or enum definitions
- Field definitions
- Imports
- Annotations
- Configuration keys
- Serialized property names
- Build files

Documentation-only changes include:

- Javadocs
- Inline comments
- README files
- Markdown documentation

If executable code changed:

- Run `mvn clean compile`
- Run relevant tests when appropriate

If changes are documentation-only:

- Do not run Maven builds or tests

Always provide a summary that includes:

1. Documentation updates
2. Internal renames
3. Public/API rename suggestions requiring approval
4. Whether validation was run and why