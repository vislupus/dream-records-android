# Contributing to Dream Records

Thank you for your interest in improving Dream Records. Contributions that make recording, organization, accessibility, or data portability more reliable are especially welcome.

## Before you start

1. Check existing issues and pull requests to avoid duplicated work.
2. Open an issue for significant changes so the approach can be discussed first.
3. Do not include private recordings, transcripts, API keys, device identifiers, or log files containing personal data.

## Development workflow

1. Fork the repository and create a descriptive branch, for example `fix/recording-timeout`.
2. Open the project in Android Studio and allow Gradle to synchronize dependencies.
3. Run `./gradlew assembleDebug` (or `gradlew.bat assembleDebug` on Windows).
4. Test any affected recording, playback, database, export/import, and transcription flows on a device or emulator where practical.
5. Submit a pull request with a short description, testing notes, and screenshots for user-interface changes.

Please keep new UI strings in `config/Strings.kt` and update both English and Bulgarian variants when applicable. Keep reusable settings and design constants in `config/AppConfig.kt`.

## Bug reports

Provide your Android version, device model, reproduction steps, expected behavior, and actual behavior. Use redacted examples or synthetic content rather than real dream transcripts or credentials.

## Code of conduct

Be constructive and respectful. Critique technical decisions rather than individuals. Maintainers may close or moderate abusive or irrelevant contributions.
