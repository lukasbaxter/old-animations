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
| **Sneak camera depth** | drops 0.35 (1.62 → 1.27) | dropped 0.08 (1.62 → 1.54) | ✅ (see caveat) |
| **Sneak model pose** | `head.y+=4.2  body.y+=3.2  arms.y+=3.2` | `head.y=1  legs.y=9`, body/arms unmoved | ✅ |
| **Blocking arm (3rd person)** | pitched, yawed 30° in, tracks the head | pitched only | ✅ |
| **Blocked item (3rd person)** | item just follows the raised arm | item rotated across the chest | ✅ |
| **Red armor on hit** | armour renders with `NO_OVERLAY` | armour reddened with the wearer | ✅ |
| **Heart flash** | hearts flash white on damage/heal | no flash | ✅ |
| **Attack cooldown indicator** | crosshair/hotbar indicator | no equivalent | ✅ |
| **Swing duration** | from the item's `SwingAnimation` component | always 6 ticks | ✅ (opt-in) |
| **Blocking slowdown** | swords aren't "in use", so no slowdown | 20% movement, no sprinting | ✅ (opt-in, not visual-only) |
| **Attacking while using** | `startAttack` bails on `isHandsBusy()` | never checked, so bow punching worked | ✅ (opt-in, sends packets) |
| **Own arrow crit trail** | crit particles stream behind it | same trail | ➖ removable (preference) |
| **Blockhit while mining** | only swings while a break progresses | swung regardless | ✅ (local animation only) |

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
changes no packet — with two opt-in exceptions, **Blocking Slows You Down** and
**Attack While Using An Item**, both called out below and both off by default.
That puts a hard limit on some things worth being clear about:

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

### How the sneak camera actually worked

1.7 never lowered the eye height when you crouched. It set `ySize = 0.2` while
sneak was held, and `Entity.moveEntity` multiplied `ySize` by `0.4` every tick.
The camera sat at `posY = bbMinY + yOffset - ySize`, so:

- holding sneak parked it exactly `0.2 × 0.4 = 0.08` low **from the first tick** —
  instant, and that 0.08 is where the familiar 1.62 → 1.54 comes from
- releasing left `ySize` decaying 0.08 → 0.032 → 0.0128 → … — **60% of the
  remaining gap per tick**, gone in about four ticks

So the asymmetry is real, but it fell out of a smoothing variable rather than
being designed. 26.2 eases in both directions instead.

Two corrections have been made here. v1.0.x snapped both ways, which is 1.8, not
1.7 (fixed in v1.1.0). v1.1.0–v1.3.0 then eased back up at 50% per tick instead
of 60%, which made standing up drag longer than it ever did in 1.7 (fixed in
v1.4.0).

One caveat worth knowing even after that fix: 1.7 was easing a **0.08** drop, and
26.2 is easing a **0.35** one (1.62 → 1.27), so the same curve reads as much more
movement. Both halves are fixed as of v1.5.0: the drop is resized to 1.7's 0.08 by default
(see below), and `Instant Unsneak` — now on by default — removes the ease.

### Caveat on the sneak camera depth

26.2 drops the crouch camera from 1.62 to 1.27, a drop of **0.35**. 1.7 dropped it
by **0.08** and no further. So 26.2 crouches more than four times as deep, and any
curve applied to that drop reads as four times as much movement — which is why
even the correct 1.7 curve felt heavy before this was fixed.

`sneakCameraDrop` in `config/oldanimations.json` is the fraction of 26.2's drop to
actually apply:

| Value | Drop | |
|---|---|---|
| `1.0` | 0.35 | 26.2 as shipped |
| `0.2286` | 0.08 | 1.7 — **the default** |
| `0.0` | 0 | camera does not move |

**The cost.** This moves the camera, not the eye. Picking originates from
`Entity.getEyePosition`, which stays at the real 1.27, so while crouched the
crosshair sits `(1 - sneakCameraDrop) × 0.35` blocks above where the ray actually
starts — 0.27 at the default. There is no version of a shallower crouch camera
that avoids this: making the *eye* shallower instead would disagree with the
server, which computes 1.27 from the pose either way. Set it to `1.0` if you would
rather have the deep crouch and an honest reticle.

---

## How the blockhit is derived

26.2's `swingArm()` does two things: a positional bob, then
`applyItemArmAttackTransform`, which is `Ry(45)` + the swing rotations +
`scale(0.4)`.

1.7 applied neither of those wholesale while an item was in use. Its bob lived in
the `else` branch that blocking skipped entirely, and the `Ry(45)` and the `0.4`
are already folded into the block pose. What 1.7 *did* apply, unconditionally,
was the swing rotations, sitting between the `Ry(45)` and the scale:

```
Ry(45); Ry(-f*20); Rz(-g*20); Rx(-g*80); scale(0.4); blockTransform
    f = sin(swing² · π)      g = sin(√swing · π)
```

Versions before v1.4.0 called `swingArm()` here, which meant every click also
shrank the sword to 0.4 and yawed it another 45°, on top of a pose that already
included both. That is why the sword left the screen while spam-clicking. Since
the block pose is the fold of `Ry(45) · scale(0.4) · block`, the swing now goes in
front of it conjugated back out of that frame: `Ry(45) · SWING · Ry(-45)`.

---

## How the third-person block is derived

1.7 and 1.8 produced the same-looking block two different ways, and only one of
them was the arm.

**1.8** yawed the arm 30° inward and let the sword follow it. **1.7** left the
arm yaw at zero — `ModelBiped` only did `rotateAngleX * 0.5 - (PI/10) * 3` — and
instead rotated the *item*, in `RenderPlayer.renderEquippedItems`:

```
translate(0.05, 0, -0.1); Ry(-50); Rx(-10); Rz(-60)
```

Restoring the 1.7 arm without the item half gives you a sword pointing straight
up along a raised arm, which is neither version. Both halves are needed.

The item numbers cannot be pasted into 26.2, for the same reason the first-person
ones could not. In 1.7 they sat immediately after `bipedRightArm.postRender()`
plus `translate(-0.0625, 0.4375, 0.0625)`, and in front of
`translate(0, 0.1875, 0)`, `scale(0.625, -0.625, 0.625)`, `Rx(-100)`, `Ry(45)`.
26.2 reaches the item by an unrelated route — `translateToHand`, `Rx(-90)`,
`Ry(180)`, `translate(1/16, 2/16, -10/16)` — and then applies the model's
`thirdperson_righthand` display transform, which 1.7 had no concept of.

So the insert is re-expressed at the frame 26.2 hands the item over in. Writing
the 1.7 insert as `D` and the arm-to-insert step as `A17`, the motion blocking
gives the sword *in arm-bone space* is `A17 · D · A17⁻¹`. Conjugating that
through 26.2's own arm-to-item step `A26`:

```
X = A26⁻¹ · A17 · D · A17⁻¹ · A26
  = translate(-0.30829775, -0.08273418, 0.12773331)
    then 80.4557° about (-0.220024, 0.748384, 0.625708)
```

Because it is a conjugation, the sword moves relative to the arm by exactly what
it moved by in 1.7, and everything downstream of the injection point — including
the display transform — cancels out of the arithmetic. The pivot is the 1.7 arm
frame rather than the item's own origin, which is why the translation is much
larger than the `(0.05, 0, -0.1)` nudge it came from.

Mirrored across the YZ plane for a left-handed main arm. 1.7 had no left-handed
players, so that half is an extrapolation rather than a restoration.

---

## Two things about the block that were wrong

**The double blockhit on re-press.** A right click that interacts with something
makes `Minecraft.startUseItem` call `player.swing(hand)`. The mod's block is held
on that same button, so every press while blocking played a swing arc on top of
the block pose — releasing and quickly re-blocking read as the sword letting go
and re-blocking a second time.

1.7 never showed this because blocking *was* the right click and consumed it.
26.2 still runs the normal interaction underneath, which is what lets you open a
door with a sword out, so the interaction stays and only the animation is
dropped. The `ServerboundSwingPacket` that `swing` would have sent is sent by
hand instead, so the traffic is byte-identical to vanilla's and other players
still see your arm move. Fixed in v1.6.0.

**No stirring animation while mining.** 26.2 only swings while something is
actually being destroyed — `continueAttack` calls `player.swing` inside the
branch where `continueDestroyBlock` returned true. On a server that refuses the
break, or in adventure mode, nothing is destroyed, so nothing swings and the
blockhit has nothing to compose onto.

**Blockhit While Mining** restarts the animation locally when vanilla lets it
lapse, while you are aimed at a block with attack held. It uses
`LivingEntity.swing(hand, false)` rather than `LocalPlayer.swing(hand)`, so no
swing packet is sent: this is a local animation only and other players see
exactly what vanilla would have shown them. On by default.

---

## The bow

**Attacking while using an item** — 1.7 allowed it by simply never checking.
`Minecraft.clickMouse` called `thePlayer.swingItem()` with no reference to
`isUsingItem()`, so drawing a bow never stopped you swinging. 26.2's
`startAttack` bails out on `LocalPlayer.isHandsBusy()`, which is set while an
item is in use.

Restoring it means the client makes attacks happen that a vanilla client would
have swallowed. Unlike the blocking slowdown, which only handicaps you, this is
the shape of thing a server anticheat looks for. It is real 1.7 behaviour and the
toggle is there, but it is off by default and it is the one setting here that
could plausibly get you flagged.

**Your own arrow's crit trail** can be turned off. This is a preference, not a
restoration — 1.7's `EntityArrow.onUpdate` spawned the same `"crit"` stream that
26.2 does. Other players' arrows keep theirs, so a fully drawn shot coming at you
still reads as one.

**Where the arrow leaves from is not fixable here.** Arrows are spawned by the
server, at your eye position as of the moment the server processed the release,
and the packet reaches you a round trip later — by which time you have moved. So
the arrow appears to come from where you *were*, which is why it drifts one way
when you strafe and the other way when you back up. Two related facts, neither of
them client-side:

- the shooter's own motion is folded into the arrow's velocity server-side
- 1.7 offset the spawn 0.16 blocks to the side of your facing
  (`posX -= cos(yaw) * 0.16`), which 26.2 dropped; arrows now leave from the
  centre of your face

A client mod can only make the arrow *render* somewhere other than where it is.
That is a lie about a moving entity's position and it tends to look worse a few
ticks later, so this mod does not do it. Say the word if you want it anyway and
it can go in behind a toggle.

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

### Settings that are preferences or trade-offs, not restorations

| Setting | Default | What it is |
|---|---|---|
| **Instant Item Swap** | on | Skips the equip dip. 1.7 ramped the item up at 0.4/tick exactly like 26.2 does, so this is not a restoration — it is just tighter when switching mid-fight. |
| **Instant Unsneak** | on | Snaps the camera up on release. 1.7 eased over about four ticks; the ease is the part of the sneak that reads as sluggish. Turn it off for the accurate curve. |
| **Blocking Slows You Down** | off | 1.7 cut movement to 20% and blocked sprinting for any item in use. Faithful, but it is **not visual-only** — it changes where you actually go. You move slower, never faster, so there is nothing for an anticheat to object to, but blocking buys you no damage reduction in 26.2 to pay for the handicap. |
| **Hide Own Arrow Trail** | on | Drops the crit particle stream behind arrows you fired. 1.7 had the same trail, so this is taste, not history. |
| **Attack While Using An Item** | off | Bow punching and block-hitting with a drawn bow. Real 1.7 behaviour, but the **only** setting that makes packets happen a vanilla client would not have sent. See above. |

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
