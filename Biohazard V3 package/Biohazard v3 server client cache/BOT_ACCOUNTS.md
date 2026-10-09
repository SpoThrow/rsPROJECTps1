# BOT_ACCOUNTS.md — bot accounts and credentials

The possession model (`BOT_PLAN.md` §5.4) made bots **real characters**, which pulled two
requirements into conflict:

- the bot must **authenticate** a real account — `PlayerSave.loadGame` checks the password
  and returns `3` on a mismatch; and
- a **human must be able to log in** later with a normal client.

Together these pin the account down: it needs a *login-legal* name and a *known* password.
This file settles both, and corrects the `[bot]` prefix from the first draft of
`BOT_PLAN.md`.

---

## 1. The hard constraints (read off the code)

Every one of these is enforced by code that already runs; none is a preference.

| Constraint | Where | Consequence |
| --- | --- | --- |
| Name must match `[A-Za-z0-9 ]+` | `RS2LoginProtocolDecoder.load()`: `if(!name.matches("[A-Za-z0-9 ]+")) returnCode = 4;` | **`[bot]` is illegal** — punctuation is rejected. A `[bot]Name` account can exist in-process but **can never be logged into**. |
| Name is trimmed + lowercased | `name = name.trim(); name = name.toLowerCase();` | Use lowercase alphanumeric names. Character files are keyed by this lowercased name. |
| Name length ≤ 12 | `if(name.length() > 12) returnCode = 8;` (the client also caps input at 12) | Budget the prefix against 12, not "however long". |
| Password is lowercased | `pass = pass.toLowerCase();` before `loadGame` | Generate lowercase passwords, or case never matches. |
| Password ≤ 20 (client) | `LoginScreen`: `myPassword.length() > 20` | Plenty. |
| Password is stored as a hash | `saveGame` writes `Misc.md5Hash(playerPass)`; `loadGame` accepts plaintext / `basicEncrypt` / `md5` | **The plaintext is not recoverable from the character file** — it must be recorded at creation or it is lost. |
| `addStarter` is IP-gated | `PlayerAssistant.addStarter()`: `if(!Connection.hasRecieved1stStarter(connectedFrom))` | All bots share the loopback IP, so **only the first would ever get a starter kit**. |
| `addStarter` starts a tutorial | `Client.initialize()`: `if(addStarter) { canWalk = false; ... sendDialogues(460, 2244); }` | A new bot account would be **frozen** (`canWalk = false`) with a forced dialogue. |

The last two are why **bot accounts must not be created through the normal new-player
path** (see §4).

---

## 2. Naming

`[bot]` is out, so the reserved prefix must be alphanumeric. Decision:

```java
public final class BotNames {
    public static final String PREFIX = "bot";   // lowercase, no punctuation
    // names look like: botwillow, botoakh01, botfishtrout
}
```

- **Legal** — `bot` + alphanumeric slug passes `[A-Za-z0-9 ]+` and survives `trim()`.
- **Short** — 3 chars leaves 9 for the slug inside the 12 cap.
- **Reserved enough** — a collision with a real player named `bot...` is possible in
  principle, but bot accounts are *provisioned* (not self-registered), so the namespace is
  controlled. `createAccount` refuses any name already on disk regardless.

Alternatives weighed and rejected for now:

- `bot willow` (space separator) — legal and readable, but two-word logins are awkward to
  type and easy to get wrong (`trim` also eats a trailing space).
- **No prefix, registry-only** (`willow`) — the cleanest login names, and the
  `Data/cfg/bots.cfg` registry already makes bots greppable; but it loses the at-a-glance
  marker in logs and in `Data/characters/`. Kept as an option if the 12-char budget ever
  bites.

---

## 3. Credentials

The character file holds only the hash, so the plaintext has to live somewhere readable.
Two records, one authoritative:

| File | Holds | Role |
| --- | --- | --- |
| `Data/characters/<name>.txt` | `character-username`, `character-password = md5(...)` | **Authoritative account.** What login checks. |
| `Data/cfg/bots.cfg` | account, **password (plaintext)**, script, profile, home, enabled | **Operator's record.** What you read to log in. |

Example:

```
# Data/cfg/bots.cfg
# account lines: the character is real and saved; the password is what you log in with
account botwillow  password wq7f2k9r  script gather_willow  home draynor  enabled true
account botoakh01  password h3n8tz4m  script gather_oak     home draynor  enabled true
```

**Plaintext caveat.** This is a local private-server config; keep it out of any public
repository, or move the passwords to a `Data/cfg/bot-accounts.cfg` that is git-ignored.
The point is that the operator can read them — a hash is useless for a human login.

**How possession authenticates.** `possess` calls
`PlayerSave.loadGame(cl, name, storedPassword)` with the recorded plaintext. This needs
**no core change** and exercises the *same* auth path a human uses. A "trusted" `loadGame`
overload that skips the check was considered and rejected: it would create a second auth
path that can drift from the real one.

---

## 4. Creating an account

`BotManager.createAccount(name, password, profile)`:

1. **Validate like the server does** — refuse a name that is not `[a-z0-9 ]+`, is empty,
   or is longer than 12. Cheaper to reject here than to spawn an unloggable account.
2. **Refuse if `Data/characters/<name>.txt` already exists** — that is a `possess`, not a
   create.
3. Create a sessionless `Client`; set `playerName` / `playerName2` / `properName` and
   `playerPass` (already lowercase).
4. **Do not set `addStarter`.** Grant the profile's kit directly with
   `getItems().addItem(...)` — the bot kits are their own lean tables, **not** the starter
   lists (see the drift note in §4.1) — and set skills and the starting position explicitly.
   This is the fix for the IP-gate and the tutorial freeze.
5. Set the persistence flags immediately: `saveFile = true`, `saveCharacter = true`,
   `newPlayer = false`.
6. `PlayerSave.saveGame(cl)` to materialise the file.
7. Append the plaintext credential to `Data/cfg/bots.cfg`.

**Profiles** are the answer to "what does a brand-new bot of this kind own, know and
stand?" — expanded below.

### 4.1 Profiles — what a fresh account starts with

A profile mirrors the three archetypes the server already has: `adventurerPid`,
`pkerPid`, `skillerPid` (`Player.java`), which the new-player buttons set in
`ClickingButtons` (case 515 sets `adventurerPid = true` before `addStarter()`; the pker
and skiller buttons do the same).

| Field | Example (`WOODCUTTER`) | Why it exists |
| --- | --- | --- |
| `tie` | `skiller` | Sets the matching `xxxPid` flag, so all profile-dependent logic agrees with a real skiller account. |
| `items` | `(1351, 1)` bronze axe | The kit. Slice 1's test already assumes a held bronze axe. |
| `skills` | all level 1, 0 XP | Base; `::train` / the lamp can lift it later. |
| `start` | a tile near the test tree, plane 0 | Where the character stands when first created. |
| `spellbook` | modern | `magic.playerMagicBook`. |
| `prefix` | `bot` | Preferred `BotNames` prefix for slug generation (§2). |

For slice 1 this is a **Java table** (`BotProfiles`) with one entry (`WOODCUTTER`) —
small, compile-checked, and enough. A `Data/cfg/bot-profiles.cfg` form is the
roadmap-Phase-E migration, not slice-1 work.

**Provisioning** (`BotProvisioning.provision(c, profile)`) is the direct path that replaces
`addStarter`:

1. set the tie flag (`c.skillerPid = true`; siblings false);
2. `c.getItems().addItem(id, n)` for each profile item;
3. set `skills.playerLevel` / `playerXP` and `getPA().refreshSkill(i)`;
4. set `position.teleportToX/Y` + `heightLevel`, and `magic.playerMagicBook`;
5. **assert the tutorial state is clean** — `c.addStarter = false`, `c.canWalk = true`,
   no open dialogue — because `Client.initialize()` freezes movement when `addStarter` is
   set (`canWalk = false; ... sendDialogues(460, 2244)`);
6. set the persistence flags (`saveFile`, `saveCharacter` true; `newPlayer` false).

**Why not just call `addStarter()`:** it is **IP-gated** (one kit per `connectedFrom`, so
only the first bot per loopback would get one), it is **two-tier** (a first *and* second
grant), it starts the **tutorial that freezes walking**, it **broadcasts** a welcome to
every online player, and it grants **2,000,000 coins** — too rich for a bot meant to look
like an ordinary new account.

Provisioning runs **once, at `createAccount`** — never on `possess` — so re-possessing a
character never re-grants. A dev-only `::bot reprovision <account> <profile>` (clear, then
re-apply) exists for testing.

**Drift note — resolved by not sharing, on purpose.** The plan was to extract the kit lists so
`addStarter` and `BotProfiles` had one source of truth. Building it showed that is the wrong goal: the
`adventurer` starter grants **2,000,000 coins** and a suit of armour, which §4.1 above already rejects as
"too rich for a bot meant to look like an ordinary new account". A shared list would therefore have to be
shared *and* filtered, which is more machinery than the duplication it removes. So `BotProfiles` carries
its own lean kits and there is **no drift to reconcile** — the two tables answer different questions.
`1351` (bronze axe) is the one item they have in common, by coincidence rather than by design.

**Implemented.** `BotProfiles` (table) + `BotProvisioning` (applies it) + an optional `profile` column on
`bots.cfg`. Three things found while building it are worth recording, because each is invisible until it
bites:

- ⚠️ **Hitpoints is the one skill a "set them all to 1" loop must not touch.** `Player`'s constructor
  seeds every skill to level 1 *except* hitpoints, which starts at 10 with the XP for 10. A naive reset
  leaves the account at a single hitpoint — a character that is dead on arrival. `BotProvisioning`
  restores it explicitly and the test pins it.
- ⚠️ **`provision` grants items additively, not idempotently.** Calling it twice grants the kit twice. That
  is safe only because `createAccount` provisions a brand-new empty character and `reprovision` clears
  first; it is documented and tested so nobody "fixes" it into a silent inventory wipe.
- ⚠️ **Item ids must come from this server's `item.cfg`, not from memory.** `addItem` grants nothing for an
  undefined id, with no log line, so a wrong id means a bot that simply never gathers. A test reads
  `item.cfg` and checks every id in the table against it.

**No packets are sent by provisioning.** A bot is sessionless, so `getOutStream()` is null and every send
in this codebase guards on that; `refreshSkill` would throw rather than refresh. The arrays are what
`saveGame` persists, and the interface catches up if a human logs in.

---

## 5. Logging in later

A human connects with a normal client and types the recorded **name** and **password**.
That works because:

- the name is login-legal (`[a-z0-9 ]+`, ≤ 12) — the reason `[bot]` had to go; and
- the password lowercases to exactly what `saveGame` hashed.

**One controller at a time.** While a bot is possessed the account sits in
`PlayerHandler.players`, so a second login is refused by the existing duplicate-name check
(`returnCode = 5`). Release the possession (which saves) and the same credentials work
immediately — that release is the handover point.

---

## 6. Password rotation

A `::bot passwd <account> <newpass>` command (via the existing `CommandHandler` registry):

1. lowercase and validate the new password;
2. set `playerPass` on the possessed client (or load/save if not possessed);
3. `saveGame`;
4. rewrite the plaintext in `Data/cfg/bots.cfg`.

---

## 7. Open items

- **Kit as a single source** — ✅ **closed, by choosing not to share.** See the drift note in §4.1: the
  starter kits are far richer than a bot should be, so a shared list would need to be shared *and*
  filtered. `BotProfiles` keeps its own lean kits instead, and the two tables are not meant to agree.
- **Ban checks** — `Connection.isNamedBanned` runs at login; ensure provisioned bot names
  are never accidentally in the banned list.
- **Prefix vs registry-only** — revisit if the 12-char budget becomes restrictive (§2).
- **Sidebars on a provisioned account** — `addStarter` calls `starterSideBars`, which
  clears every sidebar to `-1`; confirm a directly-provisioned account still ends up with
  a sane interface set once `initialize()` runs, or set it explicitly.
