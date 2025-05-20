# AzubiMark

![GitHub repo size](https://img.shields.io/github/repo-size/azubiorg/azubimark)
![GitHub stars](https://img.shields.io/github/stars/azubiorg/azubimark?style=social)
![GitHub forks](https://img.shields.io/github/forks/azubiorg/azubimark?style=social)
![LICENSE](https://img.shields.io/github/license/azubiorg/azubimark)
![GitHub last commit](https://img.shields.io/github/last-commit/azubiorg/azubimark)
![Platform](https://img.shields.io/badge/platform-Android-green)
![Minimum SDK](https://img.shields.io/badge/minSDK-26-blue)

**AzubiMark** is a modern, Material You-themed Markdown viewer for Android. It supports syntax highlighting, dark/light/dynamic themes, and provides a rich, readable viewing experience for `.md` and `.markdown` files on mobile devices.

## Features

- **Material You design**: Embraces modern Android design aesthetics with dynamic theming.
- **Markdown rendering** with:
  - Tables
  - Strikethrough
  - Task lists
  - Syntax highlighting for various programming languages
- **File Browser**: Built-in file browser to navigate and open markdown files.
- **Dynamic Theme Switcher**: Toggle between Light, Dark, and Material You themes.
- **File open support**: Open `.md` files directly from file managers or email clients.

## Screenshots

*(Include relevant screenshots here if available)*

## Installation

To build the app locally:

1. Clone the repository:
   ```bash
   git clone https://github.com/azubiorg/azubimark.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle and build the project.
4. Run the app on your Android device or emulator.

## Requirements

- Android 8.0 (API 26) and above
- Kotlin 1.8+
- Android Gradle Plugin 8.0+
- Android Studio Flamingo or newer

## Tech Stack

- **Jetpack Compose & ViewBinding**
- **Material 3 (Material You)**
- **Markwon** for markdown parsing and rendering
- **Prism4j** for syntax highlighting
- **AppCompat + ConstraintLayout**
- **AndroidX DocumentFile API** for file access

## Directory Structure

```
AzubiMark/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/me/kelexine/azubimark/
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   ├── drawable/
│   │   │   │   └── mipmap/
│   │   │   └── AndroidManifest.xml
├── build.gradle.kts
└── settings.gradle.kts
```

## License

MIT License. See [LICENSE](LICENSE) for details.

## Author

**Kelexine**  
GitHub: [github.com/kelexine](https://github.com/kelexine)
