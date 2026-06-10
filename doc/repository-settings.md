# Repository Settings

This document defines the expected GitHub repository settings for protecting
secrets and controlling merges. These settings are external to the repository
and must be verified by a repository administrator in GitHub.

## Secret Protection

Configure the repository so that:

- GitHub secret scanning is enabled.
- GitHub push protection is enabled.
- Secret-scanning alerts are reviewed and resolved by repository maintainers.

The repository also provides local safeguards:

- `detect-private-key` rejects detected private keys during pre-commit checks.
- GitGuardian `ggshield` scans staged changes during pre-commit checks.
- `.env` is ignored by Git, while `.env.example` contains development-only
  placeholders.

Local checks supplement GitHub controls; they do not confirm that repository
settings are enabled.

## Branch Protection

Configure branch protection for `main` and the active development integration
branch so that pull requests must pass the repository CI checks before merge.
Direct pushes and review requirements should follow the repository's access
and review policy.

## Contributor Requirements

Contributors must never commit:

- `.env` files
- JWT or refresh tokens
- passwords
- private keys
- API keys
- real credentials or generated secrets

Use local environment variables, an untracked `.env` file, or an approved
external secret manager. Run `pre-commit run --all-files` before opening or
updating a pull request.

## Manual Verification

A repository administrator must verify secret scanning, push protection, and
branch protection in GitHub. Repository files cannot prove that these settings
are active. Record any unavailable setting or unresolved alert in the project
tracking system without copying secret values into issues or documentation.
