# Thievery display locks - Implementation batches

Work in order. Each batch should compile and be testable before the next.

See [SYSTEM.md](SYSTEM.md) for locked rules. See [TEST_MATRIX.md](TEST_MATRIX.md) for the manual checklist.

---

## Batch 0 - Docs (this pass)

- [x] `SYSTEM.md` locked design
- [x] This file
- [x] `TEST_MATRIX.md`
- [x] Cursor rule `thievery-display-locks.mdc`

**Test:** agents and humans read SYSTEM before coding.

---

## Batch 1 - Shared bar identity

No new steal targets yet. Doors must behave as today.

- [x] `LockPickManager`: cooldown and session identity as string target id (`door:world:x:y:z`), not `packDoorKey`
- [x] `SessionKind.DISPLAY` (unused until Batch 4)
- [x] Public `startSession(player, ProximityAnchor, kind, targetId, effectiveStrength, dexterity, lockpickStrength, onProximityLost)`
- [x] `DoorLockpick.EntityProximityAnchor` (entity location, `Parameters.doorMaxDistance`)
- [x] `DoorManager` uses the new ids; `isSameDoor` becomes target-id compare
- [x] `CooldownResetService` still clears all bar cooldowns for that player

**Test:** lockpick a door, fail, 60s on that door only; success still opens + unlock window; walk away cancels.

---

## Batch 2 - Config, persistence, softdepend

- [x] `config.yml` + `ConfigLoader` / `Cache` or `Parameters`: `lockable-furniture`, `lockable-entities`, `display-lock-strength`
- [x] `EntityLockData` + `EntityLockDataManager` (`plugins/Thievery/entities/<uuid>.json`)
- [x] Furniture lock helpers: read/write `thievery.owner` / `thievery.lockState` on `Furniture.getVariables()`, then `persistFurniture`
- [x] `pom.xml` provided/system InteractibleFurniture; `plugin.yml` softdepend
- [x] Register display listener only if IF is enabled (entity types still work without IF)

**Test:** reload loads lists; save/load an `EntityLockData` file; furniture variables round-trip is unit-level or via a later in-game batch.

---

## Batch 3 - Lock toggle and access (no robbery)

One listener: `steal/DisplayStealManager` (IF + armor stand + item frame). Reuse `LockState` and chest access rules. Do not duplicate `ContainerData`.

- [x] Owner on `FurniturePlaceEvent` / `EntityPlaceEvent`
- [x] Shift left-click: cancel break/damage; owner rotates lock; same title/sound as chests
- [x] Locked without access: cancel IF break, slot take/add, pickup; armor stand manipulate + damage; item frame interact, punch, hanging break
- [x] Owner / guild / public / staff can still break and take
- [x] Non-lockable IF types unchanged

**Test:** lock `artifact_display` and `pedestal`; sneak-left does not break; strangers cannot take; owner can; armor stand and item frame same; unmarked furniture still punches off.

---

## Batch 4 - Bar robbery + shuffled dump

- [x] Right-click with lockpick on a lockable display starts `SessionKind.DISPLAY` via **the same** `LockPickManager`
- [x] Guards: no access (unless debug own), `Cache.traits`, clues, guild-online, fail cooldown, pick vs `display-lock-strength`
- [x] Refuse if no loadout-legal item fits remaining lockpick capacity
- [x] Same-target click selects; fail/break = door titles + 60s on `entity:<uuid>`
- [x] Success: shuffle slots, take what fits inventory and budget, leave the rest
- [x] IF removals fire `FurnitureSlotItemTakeEvent`; honour cancel
- [x] `RiskSource.DOOR` on start; door-style clue at display location on success/fail/break

**Test:** see TEST_MATRIX robbery rows. Confirm doors still use the same manager (no second bar class).

---

## Batch 5 - Polish

- [x] Walk-away message for displays (not "door")
- [x] Delete `EntityLockData` when the entity dies / hanging breaks for real
- [x] Furniture break by owner: variables gone with the piece (no orphan files)
- [x] Staff bypass messages consistent with chests
- [x] Tick TEST_MATRIX; fix player-facing copy (no em dash)

**Test:** full TEST_MATRIX; `mvn package` thievery.

---

## Out of scope (do not sneak into these batches)

- Chest GUI changes
- Keys on displays
- Clear-clues resolver for entities
- IF plugin API lock fields
- Locking all furniture types
