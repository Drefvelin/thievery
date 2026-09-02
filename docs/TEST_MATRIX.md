# Thievery display locks - Test matrix

Manual checks after the matching implementation batch. Player-facing strings must not contain U+2014.

## Regression (after Batch 1, repeat after Batch 4)

| # | Check |
|---|--------|
| R1 | Door lockpick bar still runs; success opens door |
| R2 | Door fail/break applies 60s on that door only |
| R3 | Walking away cancels door pick |
| R4 | Chest GUI lockpick unchanged |

## Lock toggle and access (Batch 3)

| # | Check |
|---|--------|
| L1 | Shift left-click on `artifact_display` / `pedestal` cycles PRIVATE / GUILD / PUBLIC, does not break |
| L2 | Non-owner shift left-click does not break and does not change state |
| L3 | Stranger cannot take slot items from a PRIVATE locked display |
| L4 | Owner can take and can break |
| L5 | Guild member can access GUILD; outsider cannot |
| L6 | PUBLIC anyone can take/break |
| L7 | Unlisted IF type still breaks on left-click as today |
| L8 | Armor stand: sneak-hit toggles; locked stand cannot be stripped or killed by stranger |
| L9 | Item frame / glow frame: sneak-hit toggles; locked frame cannot be rotated, emptied, or broken by stranger |
| L10 | Chunk unload/reload: furniture lock still there (variables). Armor stand / frame lock still there (uuid file) |
| L11 | Pick up and replace furniture: lock still on that piece |

## Robbery (Batch 4)

| # | Check |
|---|--------|
| S1 | Lockpick right-click starts the **same** bar (title dashes, risk line) |
| S2 | Already have access: refuse (unless `debug-allow-own-chest`) |
| S3 | No stealable item (wrong loadout or over capacity): refuse, no bar |
| S4 | One cheap legal item among illegal/expensive: bar allowed |
| S5 | Success: items leave in shuffled order; inventory full leaves remainder in the display (no ground dump) |
| S6 | Armor stand: sometimes helm, sometimes legs first across attempts |
| S7 | Fail: 60s on that entity; second pick blocked until expiry |
| S8 | Break: lockpick consumed, same 60s |
| S9 | Walk away: cancel, penalty cooldown |
| S10 | Meditation-locked pedestal slot: take event cancel skips that slot |
| S11 | Missing thief trait: refuse like chests |
| S12 | Weak pick vs `display-lock-strength` * min ratio: refuse |

## Polish (Batch 5)

| # | Check |
|---|--------|
| P1 | Display cancel message does not say "door" |
| L12 | Break/kill display deletes entity lock file |
| P2 | No U+2014 in new messages or titles |

## Money and player robbery

| # | Check |
|---|--------|
| M1 | Loadout **without** `money`: Denar coin stacks hidden/unstealable in pickpocket, chest steal, display dump |
| M2 | Loadout **with** `money`: coin steal value = `coin.value * stack * amount_per_money`; budget limits quantity |
| M3 | `/robbery start` + accept: GUI slot 8 (top-right) shows pouch when victim balance > 0 and thief has `money` |
| M4 | Slot 8 left-click takes up to `pouch-click-amount` (10); shift-left up to `pouch-shift-amount` (100) |
| M5 | Pouch take capped by victim balance, remaining budget (`floor(remaining / amount_per_money)`), and config amounts |
| M6 | Pouch debits victim DenarEconomy pouch, credits robber pouch (not coin items); GUI title budget updates |
| M7 | Without `money` in loadout: slot 8 is filler; coins in shuffled grid stay hidden |
| M8 | Victim with 7 denars: single click takes 7 |
| M9 | Grave steal **without** `money`: coins in grave are skipped; message "nothing you can steal" if only coins |
| M10 | Grave steal **with** `money`: coins taken greedily like other items |
| M11 | No U+2014 in new player-facing strings (pouch pane, grave rob hint, steal messages) |
