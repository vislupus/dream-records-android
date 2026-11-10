# Security Policy

## Reporting a security issue

Please **do not** publish exploits, API keys, recordings, or other sensitive information in a public issue.

If the repository owner has enabled GitHub private vulnerability reporting, use the **Report a vulnerability** option under the repository's Security tab. If that option is unavailable, open a public issue containing only a non-sensitive summary and request an appropriate private contact channel before sharing technical details.

## Data handling notes

Dream Records stores voice recordings and transcripts in Android app-private storage. API keys are saved in app-private DataStore preferences and are **not separately encrypted by the application**. Exported ZIP backups may contain both audio and highly personal text; they are not password-protected by the app.

Optional transcription sends the selected audio file to the chosen external provider. Review each provider's policies before enabling transcription, and never commit credentials into the repository.

## Supported versions

Security fixes, when available, will be targeted at the latest published version. No formal long-term support schedule has been established.
