# Potato Scaling

**Potato Scaling** is a Fabric Minecraft mod that scales the Create Mod's Potato Cannon damage based on RPG attributes.

## Features

- **Flat Damage Scaling**: Adds damage from attributes like `ranged_weapon:damage` to the Potato Cannon.
- **Configurable**: Define which attributes should be added as flat damage.

## Configuration

The configuration file is located at `config/potatoscaling.json`.

```json
{
  "flat_attributes": [
    "ranged_weapon:damage"
  ]
}
```

## Building

This project uses Gradle.

1.  Clone the repository.
2.  Run `./gradlew build` (Linux/Mac) or `gradlew build` (Windows).
3.  The artifact will be in `build/libs`.

## Dependencies

- Fabric API
- Create Mod (Fabric)
