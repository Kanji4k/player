# Player Spotter (Fabric, Minecraft 1.21.11)

Client-side mod. It makes other players glow, shows their name, and shows a distance
meter with an arrow pointing down at their head. Nothing is sent to the server, so it
needs no server install and works on any multiplayer server.

## Features
- Glow on other players, with your own color (presets or RGB sliders)
- Name above the head (server/tab/team name, or plain account name)
- Distance meter `[|||||     ]` with meters text, **on/off**
- Auto fill: the bar fills as the player gets closer (off = always full)
- Arrow pointing down to the head, **on/off**
- **Unlimited range** (ON by default): glow, name, meter and arrow show at any distance.
  Turn it OFF to use the range slider **1 to 16 chunks** instead.
- Master on/off, everything saved in `config/playerspotter.json`
- Settings screen from **Mod Menu** (Configure button) or the **K** key
- **V** key turns the whole mod on/off

## Build
You need Java 21.

```
gradle wrapper --gradle-version 9.2.1     # only once, if there is no gradlew yet
./gradlew build                           # Windows: gradlew.bat build
```

The mod jar is `build/libs/player-spotter-1.0.0.jar`. Put it in `.minecraft/mods`
with Fabric Loader 0.17.3+ and Fabric API for 1.21.11. Mod Menu is optional
(but needed for the Configure button).

## Far players (too far to be loaded)
Players outside your render distance are not in your game, so they get no glow or head tag.
But the server still sends their locator bar data, so the mod shows a **Far players** list at
the top left: `[meter] Name 523m`. It follows the same name list and the **Distance meter** switch.
- The server must have the locator bar on.
- The server hides players who sneak, wear a pumpkin or mob head, or are invisible.
- Some far players give only a direction, so it shows `direction only` instead of meters.
- The far bar is full next to you and empty at 2000 m.
- No arrow for far players yet.

## Notes
- "Unlimited" means the mod adds no limit of its own. The game only knows where a player is
  while the server has loaded them for you, so the real maximum is the server's view distance.
  A player farther than that cannot be drawn by any client mod.
- The arrow position is exact in first person. In third person it follows your head,
  not the camera.
- Many servers (Hypixel, competitive/PvP servers) ban glow/ESP style mods. Use it where it is allowed.

## Build with GitHub (no Java or Gradle needed on your PC)
1. Make a free GitHub account and create a new repository.
2. Upload all files from this project (keep the `.github` folder).
3. Open the **Actions** tab. The "Build mod jar" workflow runs by itself (about 5 minutes).
   You can also press **Run workflow**.
4. Open the finished run, scroll to **Artifacts**, and download `player-spotter-jar`.
   Unzip it to get `player-spotter-1.0.0.jar`.

## Choose which players to show
Open the settings (K or Mod Menu) -> **Choose players...**
- Type a name and press **Add**. Players online on the server (tab list) whose name matches
  what you typed show up as buttons (auto fill). Click one to fill the name, then press **Add**.
- After **Add**, a message shows at the top. In the list, a **green** name is loaded near you
  now (glow, meter and arrow can show). A **gray** name is not loaded yet.
- Only listed players get glow, name, meter and arrow.
- Click a name in the list (`[x] Name`) to remove it. Up to 12 names.
- Turn **Only listed players** OFF to show everyone again.
