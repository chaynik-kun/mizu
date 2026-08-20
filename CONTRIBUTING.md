## Rules

### What's allowed

* LLM-assisted contributions are allowed
* Typo and translation fixes are welcome
* Code cleanup and refactoring are welcome
* Bug fixes and performance improvements are welcome
* New features are welcome, but please open an Issue before making large architectural changes

LLM-assisted contributions are held to the same standards as any other contribution.
Please review, understand, and test generated code before submitting it.

### Conventions

* Format your code
* Test your changes before opening a PR
* Keep PRs focused; use separate PRs for unrelated changes
* Include screenshots or screen recordings for UI changes
* Test UI changes in both light and dark themes where applicable
* Check RTL layouts when changing shared UI components
* Do not hardcode user-facing strings; use localized resources
* Do not commit build outputs, local configuration, credentials, tokens, keystores, or other secrets
* Use [Conventional Commits](https://www.conventionalcommits.org/)
* Use [conventional branch names](https://conventional-branch.github.io/)

For playback-related changes, test normal playback, queue navigation, shuffle/repeat behavior, seeking, and track transitions where relevant.

For localization-related changes, make sure the default English resources remain complete and that changes do not introduce missing or incorrectly falling-back strings.

## Contributing

### Structure overview

Mizu is an Android music client built primarily with Kotlin and Jetpack Compose.

The repository contains two main modules:

#### Modules

| Module | Description |
|--------|-------------|
| `composeApp` | Main application code, Compose UI, data/domain layers, Android implementations, resources, and tests. |
| `androidApp` | Android application entry point, manifest, widgets, launcher assets, and platform-specific resources. |

#### Packages

| Package | Description |
|---------|-------------|
| `chaynik.mizu.data` | Database entities, data access, and repositories. |
| `chaynik.mizu.domain` | Application models, managers, and repository interfaces. |
| `chaynik.mizu.shared` | Android media and playback integration. |
| `chaynik.mizu.ui` | Compose screens, components, navigation, and theming. |
| `chaynik.mizu.util` | Focused core and UI utilities. |

### Resources

Strings, fonts, and other Compose resources are located in:

`composeApp/src/commonMain/composeResources`

User-facing text should use localized string resources rather than hardcoded strings.

When modifying shared UI, keep both LTR and RTL layouts in mind. Arabic and Hebrew are supported RTL locales and should not require separate UI workarounds.

SVG icon sources are located in:

`composeApp/src/commonMain/valkyrieResources`

Run:

```sh
./gradlew :generateValkyrieImageVector
