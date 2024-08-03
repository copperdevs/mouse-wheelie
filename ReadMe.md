
> [!IMPORTANT]  
> This port is still a work in progress and not fully functioning yet. You can check what is left in the port [here](#to-do-things-to-fix).

# Mouse Wheelie - 1.21 Fork

> "Small" Minecraft Fabric mod featuring inventory utilities like scrolling, sorting and auto refilling of items

## To Do (Things to fix)

- [ ] Translations
- [ ] Inventory sorting
    - Currently works when there aren't multiple stacks of the same item
    - Currently only works on first inventory open
- [ ] Inventory scrolling
- [X] Hold alt-leftclick to drop items quickly.
- [ ] Creative inventory tabs scrolling
- [X] Inventory restocking
- [ ] Right-click trades or recipes to directly apply the crafting. When holding shift it will craft/trade a full stack
    - Currently only works on first inventory open

## Fork Changes
- Switched config api from [tweed-api](https://github.com/Siphalor/tweed-api) to [owo-lib](https://github.com/wisp-forest/owo-lib)
- Switched keybinding api from [amecs-api](https://github.com/Siphalor/amecs-api) to [too-many-shortcuts](https://github.com/wyatt-herkamp/too-many-shortcuts)
- *(WIP)* Updated to 1.21
- added `maxRefillsMillis` option

## Translation status

[![Translation status](https://weblate.siphalor.de/widgets/mouse-wheelie/-/lang/multi-auto.svg)](https://weblate.siphalor.de/engage/mouse-wheelie/)

Contribute or improve translations [here](https://weblate.siphalor.de/engage/mouse-wheelie).

## License

This mod is available under [the Apache 2.0 License](./LICENSE.md). Terms and conditions apply.
