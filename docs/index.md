---
layout: home

hero:
  name: AdbPad
  text: ADB made visual.
  tagline: A powerful GUI tool that puts Android Debug Bridge at your fingertips — manage devices, run commands, capture screenshots, and launch virtual displays without touching the terminal.
  image:
    src: https://github.com/user-attachments/assets/086a1a56-50a0-4330-ad7f-c40cb4f39ba7
    alt: AdbPad Screenshot
  actions:
    - theme: brand
      text: Download
      link: https://github.com/kaleidot725/AdbPad/releases
    - theme: alt
      text: View on GitHub
      link: https://github.com/kaleidot725/AdbPad

features:
  - icon: 📱
    title: Device Management
    details: View all connected Android devices at a glance and switch between them with a single click.
  - icon: ⚡
    title: ADB Command Execution
    details: Run ADB shell commands directly from the UI without opening a terminal window.
  - icon: ⌨️
    title: Text Input
    details: Send text input to your Android device instantly — great for filling forms during testing.
  - icon: 📸
    title: Screenshots
    details: Capture device screenshots for each theme with a single click and save them locally.
  - icon: 🖥️
    title: Virtual Display
    details: Create virtual displays via scrcpy to test your app on large-screen environments.
---

## Installation {#installation}

### macOS — Homebrew

```sh
brew tap kaleidot725/kaleidot725
brew install --cask adbpad
```

> [!WARNING]
> The app is not signed with an Apple Developer certificate. Right-click the icon → **Open**, then click **Open** in the dialog. Or go to **System Settings › Privacy & Security** and click **Open Anyway**.

### Windows / Manual

1. Download the latest installer from the [Releases page](https://github.com/kaleidot725/AdbPad/releases).
2. Run the installer and follow the on-screen instructions.
3. Launch AdbPad and configure the ADB path in **Settings**.
