# 🏴‍☠️ Bounties — GPLv3 Open Source Bounty Plugin
> A free, open-source bounty plugin for Paper servers. Inspired by the DonutSMP Bounty Plugin.

---

## ⚡ Features

- Smooth bounty GUI with paginated player head display
- Add bounties on any player via command or GUI
- Confirmation menu to prevent accidental placements
- Cooldowns to prevent bounty spam
- Sort bounties by amount or name
- Search bounties by player name
- Vault economy integration
- Fully customizable messages via `config.yml`
- Tab completion on all commands
- SQLite persistence — bounties survive restarts
- **🔓 Full cracked server support** — player skulls render correctly for both premium and cracked players, even offline, even with SkinRestorer

---

## 📦 Dependencies

| Dependency | Link |
|---|---|
| Vault | [SpigotMC](https://www.spigotmc.org/resources/vault.34315/) |
| EssentialsX *(optional)* | [SpigotMC](https://www.spigotmc.org/resources/essentialsx.9089/) |
| SkinRestorer *(optional, recommended for cracked servers)* | [SpigotMC](https://www.spigotmc.org/resources/skinsrestorer.2124/) |

---

## 🔓 Cracked Server Support

Most bounty plugins render player skulls using Bukkit's offline player cache — which means skulls break the moment a player logs off, and cracked players never render at all.

This plugin fixes that properly:

- On every join, the player's skin texture (including SkinRestorer-injected skins) is cached directly into the local SQLite database
- Skulls are rendered from this texture cache — no dependency on whether the player is online, premium, or cracked
- Works out of the box with SkinRestorer — no extra configuration needed

---

## 🔧 Commands & Permissions

| Command | Description | Permission |
|---|---|---|
| `/bounty` | Opens the bounty menu | `bounties.use` |
| `/bounty add <player> <amount>` | Add a bounty | `bounties.add` |
| `/bounty search <player>` | View a player's bounties | `bounties.search` |
| `/bounty clear <player>` | Clear a player's bounties | `bounties.clear` |
| `/bounty reload` | Reload config | `bounties.reload` |

**Permission nodes:**
- `bounties.add` — place bounties
- `bounties.reload` — reload config
- `bounties.clear` — clear bounties
- `bounties.admin` — full admin access
- `bounties.*` — all permissions

---

## 🚀 Installation

1. Download the latest `.jar` from Releases
2. Drop it in your server's `/plugins` folder
3. Restart the server
4. Edit `plugins/BountyRedux/config.yml` if needed

---

## 🛠️ Building from Source
```bash
git clone https://github.com/LightNabz/BountyRedux.git
cd bounties
mvn clean package
```

The built jar will be in `/target/BountyRedux-1.0.0.jar`.

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0**.  
See [LICENSE](LICENSE) for details.