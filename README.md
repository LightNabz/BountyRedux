# 🏴‍☠️ Bounties — GPLv3 Open Source Bounty Plugin

> A free, open-source bounty plugin for Paper servers. Inspired by the DonutSMP Bounty Plugin.

---

## ⚡ Features

- Smooth bounty GUI with top bounties display
- Add bounties on any player via command
- Confirmation menu to prevent accidental placements
- Cooldowns to prevent bounty spam
- Vault economy integration
- Fully customizable messages via `config.yml`
- Tab completion on all commands

---

## 📦 Dependencies

| Dependency | Link |
|---|---|
| Vault | [SpigotMC](https://www.spigotmc.org/resources/vault.34315/) |
| EssentialsX *(optional)* | [SpigotMC](https://www.spigotmc.org/resources/essentialsx.9089/) |

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
