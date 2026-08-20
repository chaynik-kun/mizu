## Rules

### What's allowed

* LLM-assisted contributions are **allowed**
* Typo contributions are **allowed**
* Code cleaning contributions are **allowed**

### Conventions

* Format your code
* Test your changes, ensure UI is correct on different themes and form factors
* Include a screenshot for UI changes
* Keep PRs focused, create separate PRs for unrelated changes
* Use [conventional commits](https://conventionalcommits.org/)
* Use [conventional branch names](https://conventional-branch.github.io/)

## Contributing

### Structure overview

This project is structured like most Compose Multiplatform apps are.

#### Modules

| Module       | Description                                                                                       |
|--------------|---------------------------------------------------------------------------------------------------|
| `composeApp` | Shared application code, Android implementations, resources, and host tests. |
| `androidApp` | Android entry point, manifest, widgets, launcher assets, and platform resources. |

#### Packages

| Package              | Description                                                    |
|----------------------|----------------------------------------------------------------|
| `chaynik.mizu.data`   | Database entities, data access, and repositories.                 |
| `chaynik.mizu.domain` | Application models, managers, and repository interfaces.          |
| `chaynik.mizu.shared` | Android media and playback integration.                            |
| `chaynik.mizu.ui`     | Compose screens, components, navigation, and theming.              |
| `chaynik.mizu.util`   | Focused core and UI utilities.                                     |

#### Resources

Strings, fonts and other things are in `composeApp/src/commonMain/composeResources`

SVG icons are in `composeApp/src/commonMain/valkyrieResources`. Run
`./gradlew :generateValkyrieImageVector` to regenerate code for these
icons. Access them in code using `Icons.<Category>.<Icon>`

Most icons are sourced from [Material Symbols](https://fonts.google.com/icons)
**with the rounded variant.**

### Environment

You will need:

* Android Studio
	* You can use [JetBrains Toolbox](https://www.jetbrains.com/toolbox-app/) to get this
* JDK 21
* Android SDK matching the compile SDK declared in `gradle/libs.versions.toml`
