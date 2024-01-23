/*
 * Copyright (C) 2023 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limbohub.handler;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limbohub.LimboHub;
import net.elytrium.limbohub.Settings;
import net.elytrium.limbohub.data.LinkedBossBar;
import net.elytrium.limbohub.entities.NPC;
import net.elytrium.limbohub.menu.Menu;
import net.elytrium.limbohub.protocol.container.Container;
import net.elytrium.limbohub.protocol.item.ItemStack;
import net.elytrium.limbohub.protocol.packets.ClickContainer;
import net.elytrium.limbohub.protocol.packets.Interact;
import net.elytrium.limbohub.protocol.packets.SetContainerContent;
import net.elytrium.limbohub.protocol.packets.SetContainerSlot;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public class HubSessionHandler implements LimboSessionHandler {

  private final Set<NPC> npcCooldowns = new HashSet<>();
  private final Player proxyPlayer;
  private final LimboHub plugin;
  private LimboPlayer player;
  private Menu currentMenu;
  private ScheduledTask bossBarTask;
  private ScheduledTask npcTabListTask;
  private BossBar currentBossBar;

  public HubSessionHandler(Player proxyPlayer, LimboHub plugin) {
    this.proxyPlayer = proxyPlayer;
    this.plugin = plugin;
  }

  @Override
  public void onSpawn(Limbo server, LimboPlayer player) {
    this.player = player;

    for (String line : Settings.IMP.MAIN.WELCOME_MESSAGE) {
      this.proxyPlayer.sendMessage(LimboHub.getSerializer().deserialize(line));
    }

    if (!Settings.IMP.MAIN.WELCOME_TITLE.isBlank() || !Settings.IMP.MAIN.WELCOME_SUBTITLE.isBlank()) {
      this.proxyPlayer.showTitle(Title.title(
          LimboHub.getSerializer().deserialize(Settings.IMP.MAIN.WELCOME_TITLE),
          LimboHub.getSerializer().deserialize(Settings.IMP.MAIN.WELCOME_SUBTITLE),
          Title.Times.times(
              Duration.ofMillis(Settings.IMP.MAIN.WELCOME_TITLE_FADE_IN_MILLIS),
              Duration.ofMillis(Settings.IMP.MAIN.WELCOME_TITLE_STAY_MILLIS),
              Duration.ofMillis(Settings.IMP.MAIN.WELCOME_TITLE_FADE_OUT_MILLIS)
          )
      ));
    }

    if (this.plugin.getSidebar() != null) {
      this.plugin.getSidebar().show(player);
    }

    if (this.plugin.getBossBar() != null) {
      this.currentBossBar = BossBar.bossBar(Component.empty(), 0.0F, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);
      this.proxyPlayer.showBossBar(this.currentBossBar);
      this.updateBossBar(this.plugin.getBossBar());
    }

    this.plugin.getHolograms().forEach(hologram -> hologram.spawn(player));
    this.plugin.getNpcs().values().forEach(npc -> npc.spawn(player));

    if (this.plugin.getDefaultMenu() != null) {
      this.currentMenu = this.plugin.getDefaultMenu();
      this.currentMenu.getContainer().open(player);
    }

    this.npcTabListTask = this.plugin.getServer().getScheduler()
        .buildTask(this.plugin, () -> this.plugin.getNpcs().values().forEach(npc -> npc.cleanUp(player)))
        .delay(Settings.IMP.MAIN.SKIN_LOAD_SECONDS, TimeUnit.SECONDS)
        .schedule();
  }

  private void updateBossBar(LinkedBossBar bossBar) {
    if (bossBar.getBossBar() == null) {
      this.proxyPlayer.hideBossBar(this.currentBossBar);
    } else {
      BossBar newBossBar = bossBar.getBossBar();
      this.currentBossBar.name(newBossBar.name())
          .progress(newBossBar.progress())
          .color(newBossBar.color())
          .overlay(newBossBar.overlay());
      this.proxyPlayer.showBossBar(this.currentBossBar);
    }

    if (bossBar.getNext() != null && bossBar.getStayTime() != -1) {
      this.bossBarTask = this.plugin.getServer().getScheduler()
          .buildTask(this.plugin, () -> this.updateBossBar(bossBar.getNext()))
          .delay(bossBar.getStayTime(), TimeUnit.MILLISECONDS)
          .schedule();
    }
  }

  @Override
  public void onDisconnect() {
    if (this.currentBossBar != null) {
      this.proxyPlayer.hideBossBar(this.currentBossBar);
    }

    if (this.bossBarTask != null) {
      this.bossBarTask.cancel();
    }

    if (this.npcTabListTask != null) {
      if (this.npcTabListTask.status() == TaskStatus.SCHEDULED) {
        this.plugin.getNpcs().values().forEach(npc -> npc.cleanUp(this.player));
      }
      this.npcTabListTask.cancel();
    }
  }

  private void teleportToSpawn() {
    net.elytrium.limbohub.Settings.MAIN.PLAYER_COORDS coords = Settings.IMP.MAIN.PLAYER_COORDS;
    this.player.teleport(coords.X, coords.Y, coords.Z, (float) coords.YAW, (float) coords.PITCH);
  }

  @Override
  public void onMove(double posX, double posY, double posZ) {
    if (Settings.IMP.MAIN.ENABLE_Y_LIMIT && posY < Settings.IMP.MAIN.Y_LIMIT) {
      this.teleportToSpawn();
    } else {
      for (Settings.MAIN.PORTAL portal : Settings.IMP.MAIN.PORTALS) {
        if (posX >= portal.START_X && posY >= portal.START_Y && posZ >= portal.START_Z
            && posX <= portal.END_X && posY <= portal.END_Y && posZ <= portal.END_Z) {
          this.handleAction(portal.ACTION);
          switch (portal.ACTION.TYPE) {
            case DO_NOTHING:
            case OPEN_MENU:
            case SEND_MESSAGE:
            case CLOSE_MENU:
              this.teleportToSpawn();
              break;

            default:
              break;
          }
        }
      }
    }
  }

  @Override
  public void onGeneric(Object packet) {
    if (packet instanceof ClickContainer) {
      ClickContainer clickContainer = (ClickContainer) packet;

      if (this.currentMenu == null) {
        return;
      }

      Container container = this.currentMenu.getContainer();
      int containerId = container.getId();
      if (containerId != clickContainer.getWindowId()) {
        return;
      }

      if (this.proxyPlayer.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_17) <= 0) {
        this.player.writePacketAndFlush(new SetContainerSlot(-1, -1, ItemStack.EMPTY));
      }

      this.player.writePacketAndFlush(new SetContainerSlot(0, 45, ItemStack.EMPTY));
      this.player.writePacketAndFlush(new SetContainerContent(null));

      int slot = clickContainer.getSlot();

      if (slot < 0 || slot >= container.getContents().length) {
        this.player.writePacketAndFlush(new SetContainerContent(container));
        return;
      }

      Settings.MAIN.ACTION action = this.currentMenu.getAction(slot);
      switch (action.TYPE) {
        case DO_NOTHING:
          this.player.writePacketAndFlush(new SetContainerContent(container));
          break;

        case OPEN_MENU:
          this.handleAction(action);
          break;

        default:
          container.close(this.player);
          this.currentMenu = null;
          this.handleAction(action);
          break;
      }
    } else if (packet instanceof Interact) {
      Interact interact = (Interact) packet;

      NPC npc = Optional.ofNullable(this.plugin.getNpcs().get(interact.getEntityId()))
          .orElseGet(() -> this.plugin.getNpcs().get(interact.getEntityId() - 1));

      if (npc != null) {
        if (npc.getCooldown() == 0) {
          this.handleAction(npc.getAction());
        } else {
          synchronized (this.npcCooldowns) {
            if (this.npcCooldowns.contains(npc)) {
              return;
            }

            this.handleAction(npc.getAction());
            this.npcCooldowns.add(npc);
          }

          this.plugin.getServer().getScheduler()
              .buildTask(this.plugin, () -> {
                synchronized (this.npcCooldowns) {
                  this.npcCooldowns.remove(npc);
                }
              })
              .delay(npc.getCooldown(), TimeUnit.MILLISECONDS)
              .schedule();
        }
      }
    }
  }

  @Override
  public void onChat(String chat) {
    if (!chat.startsWith("/")) {
      return;
    }

    Settings.MAIN.ACTION action = this.plugin.getCommands().get(chat.substring(1));
    if (action == null) {
      return;
    }

    this.handleAction(action);
  }

  private void handleAction(Settings.MAIN.ACTION action) {
    switch (action.TYPE) {
      case DO_NOTHING:
      case CLOSE_MENU:
        break;

      case SEND_MESSAGE:
        for (String line : action.DATA.split("\n")) {
          this.proxyPlayer.sendMessage(LimboHub.getSerializer().deserialize(line));
        }
        break;

      case OPEN_MENU:
        this.currentMenu = this.plugin.getMenus().get(action.DATA);
        this.currentMenu.getContainer().open(this.player);
        break;

      case CONNECT_TO_SERVER:
        RegisteredServer server = this.plugin.getServer().getServer(action.DATA).orElseThrow(
            () -> new IllegalArgumentException("No such server with name " + action.DATA));
        this.player.disconnect(server);
        break;

      case KICK_PLAYER:
        this.proxyPlayer.disconnect(LimboHub.getSerializer().deserialize(action.DATA));
        break;

      case TELEPORT_PLAYER:
        String[] data = action.DATA.split(" ");
        this.player.teleport(
            Double.parseDouble(data[0]), Double.parseDouble(data[1]), Double.parseDouble(data[2]),
            Float.parseFloat(data[3]), Float.parseFloat(data[4])
        );
        break;

      default:
        throw new IllegalStateException();
    }
  }
}
