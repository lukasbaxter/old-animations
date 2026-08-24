# Old Animations

1.7-style animations for Minecraft **26.2** on Fabric. Client-side and purely visual.

Built by diffing the 26.2 client against 1.7.10 rather than by eyeballing it, so
each feature below corresponds to a specific behaviour that actually changed.

---

## What it changes

| Feature | 26.2 does | 1.7 did | Restored |
|---|---|---|---|
| **Sword blocking** | swords have no use action at all | right-click raised the sword | ✅ |
| **Blockhit** | n/a | swing arc played on top of the block pose | ✅ |
| **Sneak camera easing** | eases 50% toward the target per tick | snapped instantly | ✅ |
| **Sneak eye height** | 1.27 | 1.54 | ✅ (opt-in, see caveat) |
| **Sneak model pose** | `head.y+=4.2  body.y+=3.2  arms.y+=3.2` | `head.y=1  legs.y=9`, body/arms unmoved | ✅ |
| **Blocking arm (3rd person)** | pitched, yawed 30° in, tracks the head | pitched only | ✅ |
| **Swing duration** | from the item's `SwingAnimation` component | always 6 ticks | ✅ (opt-in) |

### What is deliberately *not* here

- **Bow pull** and **eat/drink** animations. Their constants in 26.2 are
  byte-for-byte the 1.7.10 ones (`-0.2785682, 0.18344387, 0.15731531`, `-13.935°`,
  `35.3°`, `-9.785°`, the `(p²+2p)/3` pull curve, the `pow(t, 27)` eat jiggle).
  They were never changed, so there is nothing to restore and a toggle for them
  would do literally nothing.
- **The third-person swing arc.** 26.2 uses `Ease.outQuart` with the same
  `1.2`/`0.75`/`-0.4` coefficients 1.7 used. Already identical.

---

## What it cannot do

This mod only draws things differently. It sends nothing, suppresses nothing, and
changes no packet. That puts a hard limit on two things worth being clear about:

1. **Sword blocking does not block damage.** In 1.7 blocking halved incoming
   damage; that is server-side and gone in 26.2. You get the pose and the muscle
   memory, not the damage reduction.

2. **Other players cannot be shown blocking.** Swords have no use action in 26.2,
   so the server never broadcasts "this player is blocking" — the information does
   not exist on the wire. Third-person blocking therefore applies to *your own*
   player only (F5 view). Inventing it for other players would mean showing you
   something that isn't true.

Attack cooldown, reach, knockback and sweep attacks are all server-side gameplay
and are likewise untouched.

### Caveat on `1.7 Sneak Eye Height`

This one is **off by default** on purpose. It moves the camera to 1.54, but block
and entity picking still originate from the real 1.27 eye position — that is what
keeps the mod visual-only. The result is that while crouched, your crosshair sits
slightly below where you are actually aiming. Turn it on if you want the 1.7 look;
leave it off if you want your aim to match the reticle. `Instant Sneak Camera` is
the part of 1.7 sneak you actually feel, and it has no such tradeoff — it is on by
default.

---

## Install (Windows)

Grab the installer once (it is also attached to every release):

```powershell
irm https://raw.githubusercontent.com/lukasbaxter/old-animations/main/scripts/install-oldanimations.ps1 -OutFile install-oldanimations.ps1
```

Then, every time you want the newest build:

```powershell
.\install-oldanimations.ps1
```

It finds your mods folder, pulls the newest release jar from GitHub, deletes the
old `oldanimations-*.jar`, and drops the new one in. The folder is remembered, so
after the first run it is just the one command.

```powershell
.\install-oldanimations.ps1 -ListDirs      # show every detected mods folder
.\install-oldanimations.ps1 -ModsDir "..." # pick/override the folder
.\install-oldanimations.ps1 -Version v1.0.1  # install a specific release
.\install-oldanimations.ps1 -Force         # reinstall the same version
```

If PowerShell refuses to run it:

```powershell
powershell -ExecutionPolicy Bypass -File .\install-oldanimations.ps1
```

Close Minecraft first — Windows locks the jar while the game is running, and the
script will tell you so rather than half-installing.

Requires Minecraft **26.2** with **Fabric Loader 0.19.3+**. No Fabric API needed.

---

## Configuring

Press **O** in game (rebindable under Controls → Old Animations). There is also an
unbound "Toggle Old Animations" key for flipping the whole mod off mid-game.

Settings live in `config/oldanimations.json`. Four values are file-only because
they are fine-tuning you would rarely touch:

| Key | Default | Meaning |
|---|---|---|
| `blockOffsetX/Y/Z` | `0.0` | nudges the first-person block pose |
| `blockScale` | `1.0` | scales the blocked item |

`blockPose` picks which constants build the first-person block:

- **`V1_8`** (default) — the transform vanilla still uses for non-shield `BLOCK`
  items. Built for modern item display transforms, so it lands correctly as-is.
- **`V1_7`** — 1.7.10's `doBlockTransformations()` verbatim. Closer to 1.7 on
  paper, but 1.7 had no per-model display transforms, so this usually wants the
  offset/scale values above to sit right.

---

## Building

```bash
./gradlew build      # jar lands in build/libs/
./gradlew runClient  # dev client
```

Needs JDK 25. Note that 26.2 ships **unobfuscated** — Yarn and intermediary stop
at 1.21.11 — so there is no `mappings` dependency in `build.gradle`. That is
correct, not an omission.

Releases are cut by pushing a tag:

```bash
git tag v1.0.1 && git push origin v1.0.1
```

which builds the jar and publishes it, and is what the installer reads.

---

## Licence

MIT.
