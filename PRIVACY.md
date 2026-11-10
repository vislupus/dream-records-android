# Privacy Policy — Dream Records

**Effective date:** November 10, 2026

Dream Records is an Android dream journal designed to store your recordings and written entries on your device. This document describes how the current open-source application handles information. It does not replace the privacy terms of third-party transcription services.

## Information stored on your device

The app stores audio recordings, transcripts, dates, tags, and combined night entries in Android app-private storage. User preferences and API keys are stored in app-private Jetpack DataStore preferences. API keys are **not encrypted with a separate application-managed encryption layer**.

The app does not include a proprietary account system, a cloud synchronization backend, or analytics and advertising SDK integrations in the current source code.

## Microphone access

Microphone permission is used to record your voice when you initiate recording. An Android foreground service allows recording to continue while the screen is off. You can stop recording manually or use the configurable silence-based auto-stop function.

## Optional online transcription

If you choose to transcribe a recording, Dream Records sends that audio file to the selected external provider (Google Gemini, Groq, or ElevenLabs) using your API key. Your recording and related request data may be processed according to that provider's terms and privacy policy. Provider charges and retention policies are outside the app's control.

Recording, playback, journal browsing, and manual editing work without online transcription. The application does not automatically transmit every saved recording to an AI provider.

## Exporting and importing data

The export function creates a ZIP containing YAML metadata and audio recordings. This export is **not password protected by the app**. The destination and any subsequent sharing are controlled by you. Importing a backup may replace transcripts, tag assignments, and combined night text for entries with matching IDs.

## Retention and deletion

Journal entries remain on your device until you delete them, remove the app's data, or uninstall the app. Android automatic backup is disabled in the application manifest. Use the app's export function to create your own backups before deleting the application or changing the signing key.

## Security considerations

Protect your device, API keys, and exported backups. The project does not guarantee that a compromised or unlocked device cannot expose locally stored information. Avoid posting personal transcripts, exports, or credentials in public GitHub issues.

## Changes

This policy may be updated when the application's features or data practices change. Refer to the repository history for revisions.

## Questions

Open a GitHub issue with a non-sensitive question. For security vulnerabilities, follow [SECURITY.md](SECURITY.md) and avoid publishing confidential information.
