# Thievery - Display locks (locked design)

Steal from lockable **displays**: InteractibleFurniture types, armor stands, and item frames. Chests and doors stay as they are.

See [IMPLEMENTATION_BATCHES.md](IMPLEMENTATION_BATCHES.md) for the build order. See [TEST_MATRIX.md](TEST_MATRIX.md) for the manual checklist.

## Locked split (do not blur)

| Target | Lock model | Robbery | Fail |
|--------|------------|---------|------|
| Doors | Key + strength (`DoorData`) | Title **bar** | 60s on that door |
| Chests / barrels / etc. | Owner + `LockState` | Hidden **GUI** probe | Access-map cooldown |
| IF furniture, armor stands, item frames | Owner + `LockState` (chest model) | Same **bar** as doors | Same 60s as doors (`fail-cooldown-ms`) |

Do not put displays on the chest GUI. Do not copy `LockPickManager` into a second bar. Doors and displays share one engine.

## Bar engine

`LockPickManager` is the only bar. Generalize it; do not fork.

- Cooldown identity is a **string target id**, not `packDoorKey(Location)`:
  - Doors: `door:<world>:<x>:<y>:<z>`
  - Entities: `entity:<uuid>`
- `SessionKind`: `DOOR` and `DISPLAY` (furniture, armor stand, and item frame all use `DISPLAY`).
- `DoorLockpick.ProximityAnchor` stays. Add `EntityProximityAnchor` (same `door-max-distance`).
- `startDoorSession` remains a thin wrapper. Displays call the shared `startSession`.
- Fail and break still `cancelSession(uuid, true)` so they share `lockpickFailCooldownMs`.
- Right-click while already in a session for **that** target is select. Starting a session cancels (and penalizes) any previous one.

Display bar difficulty uses config `lockpicking.display-lock-strength` (no key). Same formula as doors:

`effectiveStrength = strength * (1 - pickStrength * lockpickMaxReduction)`

Honor `min-lock-strength-ratio` against that display strength.

## Lock state

Reuse `LockState` (`PRIVATE` / `GUILD` / `PUBLIC`) and the same access rules as `ContainerData.canAccess` (owner, guild, public, `thievery.admin` bypass).

Shift left-click is the toggle (same titles/sound as chests). Only the owner rotates state. Shift left-click **never** breaks a lockable display.

If the player cannot access a locked display, cancel:

- IF: `FurnitureBreakEvent`, slot take/add, empty-hand pickup
- Armor stand: `PlayerArmorStandManipulateEvent`, damage
- Item frame: interact (rotate/insert), punch-out, hanging break

Owner / guild / public access still allows legitimate break and take.

Owner is set on place (`FurniturePlaceEvent`, `EntityPlaceEvent`). If missing, the first successful owner toggle may claim it.

## Persistence (entity-capable)

`ContainerDataManager` is block-file keyed. Do not reuse it for entities.

**Vanilla entities** (armor stand, item frame, glow item frame): `plugins/Thievery/entities/<uuid>.json` (`owner`, `lockState`). Delete on entity remove.

**IF furniture:** do not key only by `Furniture.getEntityId()`. Restore can respawn the ItemDisplay and change the UUID (`FurnitureRestoreHandler.ensureDisplay`). Store `thievery.owner` and `thievery.lockState` in `Furniture.getVariables()` and `persistFurniture`. The lock follows pickup, chunk reload, and respawn.

Config:

```yaml
lockpicking:
  lockable-furniture:
    - artifact_display
    - pedestal
  lockable-entities:
    - ARMOR_STAND
    - ITEM_FRAME
    - GLOW_ITEM_FRAME
  display-lock-strength: 0.5
```

Only listed IF ids are lockable. Listed entity types are all lockable.

Thievery softdepends InteractibleFurniture. Register IF listeners only when IF is present.

## Robbery (success loot)

Start the bar only if the player does not already have access (same as chests; honor `debug-allow-own-chest`). Apply the chest **thief trait** check (`Cache.traits`). Reuse door start checks: clues, guild-online, fail cooldown, lockpick strength.

Contents:

- IF: items in `getActiveSlots()`
- Armor stand: helmet, chest, legs, boots, main hand, offhand
- Item frame: the framed item

**Gate:** at least one item the thief can take: `CategoryHandler.canRevealItem` (loadout) and `StealBudget.computeTakeableAmount` > 0, using lockpick `capacity`. Bundles: `ItemValue.hasStealableContents` / `canStealAnything`. If none, refuse.

On **SUCCESS**: shuffle slots, walk that order (armor stand piece is luck). For each:

- Skip if not on loadout or over remaining budget
- `addItem`; if it does not fit, **leave it in the display** (no world drop)
- Charge `StealBudget` from the lockpick capacity

On **FAIL / BREAK**: same titles, sounds, and lockpick break as `DoorManager.handleSelectResult`. Cooldown is already applied by `LockPickManager`.

When removing IF slot items, fire `FurnitureSlotItemTakeEvent` first. If another plugin cancels (meditation lock, etc.), skip that slot.

## Risk and clues

Starting the bar uses `RiskSource.DOOR` (same minigame). After a successful dump, drop door-style clues at the display location (owner UUID). Do not open a chest GUI or ramp chest break chance.

## Out of scope

- Changing chest GUI lockpicking
- Keys on displays
- Locking every IF type
- Clear-clues-on-entity in the first cut (may follow later)
- Putting lock fields into the IF plugin API unless `variables` proves insufficient
