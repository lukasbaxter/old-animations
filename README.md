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
| **Sneak camera curve** | eases both ways | instant down, eased up | ✅ |
| **Sneak eye height** | 1.27 | 1.54 | ✅ (opt-in, see caveat) |
| **Sneak model pose** | `head.y+=4.2  body.y+=3.2  arms.y+=3.2` | `head.y=1  legs.y=9`, body/arms unmoved | ✅ |
| **Blocking arm (3rd person)** | pitched, yawed 30° in, tracks the head | pitched only | ✅ |
| **Red armor on hit** | armour renders with `NO_OVERLAY` | armour reddened with the wearer | ✅ |
| **Heart flash** | hearts flash white on damage/heal | no flash | ✅ |
| **Attack cooldown indicator** | crosshair/hotbar indicator | no equivalent | ✅ |
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

### A correction, kept here on purpose

v1.0.x shipped an "Instant Sneak Camera" toggle that snapped the camera in **both**
directions. That is the **1.8** behaviour, not the 1.7 one. 1.7's crouch camera is
asymmetric: instant on the way down, eased at 50% per tick on the way back up
(that asymmetry is what Orange's mod calls "longer unsneak"). Fixed in v1.1.0,
and the setting is now `1.7 Sneak Camera`.

Since 26.2 eases in both directions, the audible difference is entering a crouch,
not leaving one.

### Caveat on `1.7 Sneak Eye Height`

This one is **off by default** on purpose. It moves the camera to 1.54, but block
and entity picking still originate from the real 1.27 eye position — that is what
keeps the mod visual-only. The result is that while crouched, your crosshair sits
slightly below where you are actually aiming. Turn it on if you want the 1.7 look;
leave it off if you want your aim to match the reticle. `Instant Sneak Camera` is
the part of 1.7 sneak you actually feel, and it has no such tradeoff — it is on by
default.

---

## Compared to Orange's 1.7 Animations / Animatium

Orange's mod (and the open-source [Animatium-Legacy](https://github.com/Legacy-Visuals-Project/Animatium-Legacy),
GPL-3.0) target **1.8.9**. The 1.8→1.7 gap is not the same as the 26.2→1.7 gap, so
their option list does not port across one-for-one. Their feature lists were used
to decide *what to look for*; every behaviour here was then derived independently
from the 26.2 and 1.7 sources, and no code was copied. This mod stays MIT.

**Taken from their list and implemented:** block-hitting animation, third-person
sword/arm block position, 1.7 sneak camera curve including the longer unsneak,
1.7 third-person sneaking pose, red armor on hit, no heart flash.

**Not implemented, and why:**

| Their feature | Why not |
|---|---|
| Damage tilt / hurt camera shake | Already a vanilla slider: **Options → Accessibility → Damage Tilt**, set it to 0. A mod toggle would just shadow it. |
| Punching during usage (bow/potion punching) | 26.2 gates attacks behind `!player.isUsingItem()`. Enabling it means sending attack packets vanilla would not send. That is gameplay, not visuals, and it is the kind of thing server anticheats flag. Out of scope by design. |
| 1.7 bow pullback, eat/drink animation, swing arc | Already byte-for-byte 1.7 in 26.2. Nothing to restore. |
| Enchantment glint, 2D dropped items, potion models, skull sprites, XP orb positions | Cosmetic rather than combat-relevant, and each is a large rendering change that cannot be validated without playing. Not worth the crash risk for what they add. |
| Item switching / re-equip animation | 26.2 already skips the swap animation for component-only changes via `ignoreSwapAnimation`. The remaining gap is small and not verifiable without visual testing. |

---

## Install (Windows)

Download both files from the [latest release](https://github.com/lukasbaxter/old-animations/releases/latest),
put them in the same folder, and **double-click `install-oldanimations.cmd`**.

Windows blocks unsigned `.ps1` files by default, which is why the `.cmd` wrapper
exists. Batch files aren't covered by the execution policy, so it runs the
installer without needing `Set-ExecutionPolicy` or admin rights.

To fetch both from a terminal instead:

```powershell
$base = "https://raw.githubusercontent.com/lukasbaxter/old-animations/main/scripts"
irm "$base/install-oldanimations.ps1" -OutFile install-oldanimations.ps1
irm "$base/install-oldanimations.cmd" -OutFile install-oldanimations.cmd
```

Then, every time you want the newest build:

```powershell
.\install-oldanimations.cmd
```

Arguments pass straight through:

```powershell
.\install-oldanimations.cmd -ListDirs          # show every detected mods folder
.\install-oldanimations.cmd -ModsDir "..."     # pick/override the folder
.\install-oldanimations.cmd -Version v1.0.1    # install a specific release
.\install-oldanimations.cmd -Force             # reinstall the same version
```

<details>
<summary>Running the .ps1 directly instead</summary>

If you'd rather skip the wrapper, either bypass the policy per-run:

```powershell
powershell -ExecutionPolicy Bypass -File .\install-oldanimations.ps1
```

or allow local scripts once, for your user only (no admin needed):

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
Unblock-File .\install-oldanimations.ps1
```

`RemoteSigned` still blocks downloaded scripts until you `Unblock-File` them,
which is why both lines are there.

</details>

Close Minecraft first. Windows locks the jar while the game is running, and the
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

### How the block pose is derived

1.7 and 1.8 used the **same** sword block transform:

```
translate(-0.5, 0.2, 0); Ry(30); Rx(-80); Ry(60)
```

applied after `transformFirstPersonItem`'s `Ry(45)` and `scale(0.4)`, in front of
a `firstperson` display transform of `Ry(-135) Rz(25)` at scale `1.7`.

26.2 moved the `Ry(45)` and the `0.4` into the item model itself — `handheld`
now declares `firstperson_righthand` as `Ry(-90) Rz(25)` at scale `0.68`. Folding
the old chain through that change gives:

```
translate  (-0.5, 0.2, 0) x 0.4 through Ry(45)   = (-0.14142136, 0.08, 0.14142136)
rotation   Ry(45) Ry(30) Rx(-80) Ry(60) Ry(-135) = Ry(75) Rx(-80) Ry(-75)
           cancelling the new display Ry(-90)     = Ry(75) Rx(-80) Ry(15)
```

That is what the mod applies, and it reproduces the original orientation to
**0.0000°**. It puts the blade at 149° from horizontal, across the middle of the
view — the pose everyone means by "it crosses your chest".

For reference, vanilla's own `BLOCK` branch rounds the same rotation into Euler
angles and lands **0.329°** off, so this is fractionally closer to 1.7 than
vanilla's constants are.

Versions before 1.2.0 offered a "1.7" preset that dropped the raw 1.7 numbers
into 26.2 without folding in the `Ry(45)` and the scale. That is wrong by
**57°** and produced a visibly incorrect pose. It has been removed rather than
fixed, because once corrected it is identical to the pose above.

---

## Compared to Orange's 1.7 Animations / Animatium

Orange's mod (and the open-source [Animatium-Legacy](https://github.com/Legacy-Visuals-Project/Animatium-Legacy),
GPL-3.0) target **1.8.9**. The 1.8→1.7 gap is not the same as the 26.2→1.7 gap, so
their option list does not port across one-for-one. Their feature lists were used
to decide *what to look for*; every behaviour here was then derived independently
from the 26.2 and 1.7 sources, and no code was copied. This mod stays MIT.

**Taken from their list and implemented:** block-hitting animation, third-person
sword/arm block position, 1.7 sneak camera curve including the longer unsneak,
1.7 third-person sneaking pose, red armor on hit, no heart flash.

**Not implemented, and why:**

| Their feature | Why not |
|---|---|
| Damage tilt / hurt camera shake | Already a vanilla slider: **Options → Accessibility → Damage Tilt**, set it to 0. A mod toggle would just shadow it. |
| Punching during usage (bow/potion punching) | 26.2 gates attacks behind `!player.isUsingItem()`. Enabling it means sending attack packets vanilla would not send. That is gameplay, not visuals, and it is the kind of thing server anticheats flag. Out of scope by design. |
| 1.7 bow pullback, eat/drink animation, swing arc | Already byte-for-byte 1.7 in 26.2. Nothing to restore. |
| Enchantment glint, 2D dropped items, potion models, skull sprites, XP orb positions | Cosmetic rather than combat-relevant, and each is a large rendering change that cannot be validated without playing. Not worth the crash risk for what they add. |
| Item switching / re-equip animation | 26.2 already skips the swap animation for component-only changes via `ignoreSwapAnimation`. The remaining gap is small and not verifiable without visual testing. |

---

## Install (Windows)

Download both files from the [latest release](https://github.com/lukasbaxter/old-animations/releases/latest),
put them in the same folder, and **double-click `install-oldanimations.cmd`**.

Windows blocks unsigned `.ps1` files by default, which is why the `.cmd` wrapper
exists. Batch files aren't covered by the execution policy, so it runs the
installer without needing `Set-ExecutionPolicy` or admin rights.

To fetch both from a terminal instead:

```powershell
$base = "https://raw.githubusercontent.com/lukasbaxter/old-animations/main/scripts"
irm "$base/install-oldanimations.ps1" -OutFile install-oldanimations.ps1
irm "$base/install-oldanimations.cmd" -OutFile install-oldanimations.cmd
```

Then, every time you want the newest build:

```powershell
.\install-oldanimations.cmd
```

Arguments pass straight through:

```powershell
.\install-oldanimations.cmd -ListDirs          # show every detected mods folder
.\install-oldanimations.cmd -ModsDir "..."     # pick/override the folder
.\install-oldanimations.cmd -Version v1.0.1    # install a specific release
.\install-oldanimations.cmd -Force             # reinstall the same version
```

<details>
<summary>Running the .ps1 directly instead</summary>

If you'd rather skip the wrapper, either bypass the policy per-run:

```powershell
powershell -ExecutionPolicy Bypass -File .\install-oldanimations.ps1
```

or allow local scripts once, for your user only (no admin needed):

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
Unblock-File .\install-oldanimations.ps1
```

`RemoteSigned` still blocks downloaded scripts until you `Unblock-File` them,
which is why both lines are there.

</details>

Close Minecraft first. Windows locks the jar while the game is running, and the
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
