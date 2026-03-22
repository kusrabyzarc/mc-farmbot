# Farmbot

Simple client-side Fabric mod for Minecraft `1.21.10` that helps with AFK farming.

## Features

- client-side auto-farm toggle
- auto-eat from the offhand while farming
- emergency escape on low health
- optional disconnect when offhand food runs out
- in-game settings screen through Mod Menu

## Requirements

- Minecraft `1.21.10`
- Fabric Loader
- Fabric API
- Java `21`
- Mod Menu is optional, but recommended for the config screen

## Installation

1. Install Fabric Loader for Minecraft `1.21.10`.
2. Put Fabric API into your `mods` folder.
3. Put the Farmbot jar into your `mods` folder.
4. Optionally add Mod Menu if you want an in-game config button.

## Usage

- Put a sword in your main hand.
- Put food in your offhand.
- Press `F6` to toggle auto-farm.
- Open Mod Menu to change health and disconnect settings.

## Source

- [GitHub](https://github.com/kusrabyzarc/mc-farmbot)

## License

- [MIT](LICENSE)

## Build

```powershell
.\gradlew.bat build
```

The built jar will appear in `build\libs`.
