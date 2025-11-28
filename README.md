# WildTag

Originally made for _Spigot 1.16_, then rewritten for _Paper 1.20.6_, and now slightly tweaked for _Paper/Folia 1.21.6+_!  
Remake of a minigame, requested by a friend ;)

---

Ever thought of playing tag in Minecraft using your world? Well this is the plugin for you!  
It has some cool features like uh.. a tracker that is a compass and a custom locator bar (if your server is running on 1.21.6 or above).

> [!CAUTION]
> Use on a copy of your world or a new one, as the plugin **will** modify inventories & spawn locations.

### Command

| Command                                               | Description                                  | Permission    |
|-------------------------------------------------------|----------------------------------------------|---------------|
| `wildtag start`                                       | Start a normal match                         | wildtag.cmd   |
| `wildtag start <hunter(s)> <seconds> <center coords>` | Start a custom match                         | wildtag.cmd   |
| `wildtag stop`                                        | Stop a running match in the world you are in | wildtag.cmd   |
| `wildtag reload`                                      | Reloads the plugin                           | wildtag.cmd   |
| `wildtag timer <increment/decrement> <seconds>`       | Adds/subtracts time from a running match     | wildtag.cmd   |


### Development

Requires [WayTrick](https://github.com/Lukiiy/WayTrick) due to the custom locator bar.
