# ZabytLib

zabyt's shared HUD/config/screen toolkit for Fabric mods.

A small library mod that other zabyt mods are built on top of. Instead of every mod re-implementing config screens, HUD positioning and menu integration, they use the helpers from this library.

## What it provides

- **Menu icon integration** - adds an icon button on the title screen and pause menu in the style of ukulib, which opens a list of every mod using the library, each row showing the mod's own icon and name; clicking a row opens that mod's config screen.
- **SimpleConfig** - JSON-backed config classes: declare public fields with defaults, load/save with one call.
- **AutoConfigScreen** - declare config fields with the `@ConfigEntry` annotation (label/min/max) and get a full ModMenu-compatible settings screen for free: toggles, sliders, numeric fields, enum dropdowns and strings, saved automatically.
- **Toast API** - `ZabytToasts.show(...)` for vanilla-style update/status notifications.
- **Update checker** - checks Modrinth for newer releases of the zabyt mods on the title screen and shows a toast; toggle in ZabytLib's own config.
- **HUD helpers** - draggable HUD screens, screen zones, HUD info panels, shared HUD colors and timers so all zabyt HUDs look and behave the same.

## Mods using it

- [ArmorHudMod](https://github.com/zabyt/ArmorHudMod)
- [MMPlayer](https://github.com/zabyt/MMPlayer)
- [NeverRunOut](https://github.com/zabyt/NeverRunOut)

## Building

The clone is self-contained: every jar needed for compilation (ModMenu API) is in `libs/`.

```
git clone https://github.com/zabyt/ZabytLib
cd ZabytLib
gradlew.bat build
```

The built jar ends up in `build/libs/`.

## Dependencies

- Minecraft `~26.2`, Fabric Loader `>=0.19.3`, Java `>=25`
- [ModMenu](https://modrinth.com/mod/modmenu) at runtime

## Credits

The menu-icon + mods-list concept is a tribute to [ukulib](https://github.com/uku3lig/ukulib) by uku3lig - shoutout to uku for the original idea.

## License

MIT
