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

package net.elytrium.limbohub;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.elytrium.commons.kyori.serialization.Serializer;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.commons.utils.updates.UpdatesChecker;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.command.LimboCommandMeta;
import net.elytrium.limboapi.api.file.WorldFile;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.protocol.PacketDirection;
import net.elytrium.limboapi.api.protocol.packets.PacketMapping;
import net.elytrium.limbohub.command.HubCommand;
import net.elytrium.limbohub.command.ReloadCommand;
import net.elytrium.limbohub.data.LinkedBossBar;
import net.elytrium.limbohub.data.Sidebar;
import net.elytrium.limbohub.entities.Hologram;
import net.elytrium.limbohub.entities.NPC;
import net.elytrium.limbohub.handler.HubSessionHandler;
import net.elytrium.limbohub.listener.HubListener;
import net.elytrium.limbohub.menu.Menu;
import net.elytrium.limbohub.protocol.container.Container;
import net.elytrium.limbohub.protocol.item.ItemStack;
import net.elytrium.limbohub.protocol.item.meta.StaticItemMeta;
import net.elytrium.limbohub.protocol.packets.ClickContainer;
import net.elytrium.limbohub.protocol.packets.CloseContainer;
import net.elytrium.limbohub.protocol.packets.DisplayObjective;
import net.elytrium.limbohub.protocol.packets.Interact;
import net.elytrium.limbohub.protocol.packets.OpenScreen;
import net.elytrium.limbohub.protocol.packets.ScoreboardTeam;
import net.elytrium.limbohub.protocol.packets.SetContainerContent;
import net.elytrium.limbohub.protocol.packets.SetContainerSlot;
import net.elytrium.limbohub.protocol.packets.SetEntityMetadata;
import net.elytrium.limbohub.protocol.packets.SetHeadRotation;
import net.elytrium.limbohub.protocol.packets.SpawnEntity;
import net.elytrium.limbohub.protocol.packets.SpawnPlayer;
import net.elytrium.limbohub.protocol.packets.UpdateObjectives;
import net.elytrium.limbohub.protocol.packets.UpdateScore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.slf4j.Logger;

@Plugin(
    id = "limbohub",
    name = "LimboHub",
    version = BuildConstants.VERSION,
    authors = {
        "JNNGL"
    },
    dependencies = {
        @Dependency(id = "limboapi")
    }
)
public class LimboHub {

  private static int ENTITY_ID_COUNTER;

  @MonotonicNonNull
  private static Logger LOGGER;
  @MonotonicNonNull
  private static Serializer SERIALIZER;
  @MonotonicNonNull
  @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
  private static LimboHub INSTANCE;

  private final Map<String, Menu> menus = new HashMap<>();
  private final Map<String, Settings.MAIN.ACTION> commands = new HashMap<>();
  private final Map<Integer, NPC> npcs = new HashMap<>();
  private final List<Hologram> holograms = new ArrayList<>();
  private final Path dataDirectory;
  private final File configFile;
  private final ProxyServer server;
  private final Metrics.Factory metricsFactory;
  private final LimboFactory limboFactory;

  private String currentHubCommand = null;
  private LinkedBossBar bossBar;
  private Menu defaultMenu;
  private Sidebar sidebar;
  private Limbo hubServer;

  @Inject
  public LimboHub(Logger logger, ProxyServer server, Metrics.Factory metricsFactory, @DataDirectory Path dataDirectory) {
    INSTANCE = this;
    setLogger(logger);

    this.server = server;
    this.metricsFactory = metricsFactory;
    this.dataDirectory = dataDirectory;
    this.configFile = this.dataDirectory.resolve("config.yml").toFile();

    this.limboFactory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
  }

  private String roundStat(int stat) {
    if (stat == 0) {
      return "0";
    }

    if (stat >= 25) {
      return "25+";
    }

    int rounded = (stat / 5) * 5;
    return Math.max(rounded, 1) + "-" + (rounded + 4);
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    Settings.IMP.setLogger(LOGGER);

    this.reload();

    Metrics metrics = this.metricsFactory.make(this, 19042);
    metrics.addCustomChart(new SimplePie("sidebar_enabled", () -> String.valueOf(Settings.IMP.MAIN.SIDEBAR.ENABLED)));
    metrics.addCustomChart(new SimplePie("boss_bars", () -> this.roundStat(Settings.IMP.MAIN.BOSSBARS.size())));
    metrics.addCustomChart(new SimplePie("commands", () -> this.roundStat(Settings.IMP.MAIN.COMMANDS.size())));
    metrics.addCustomChart(new SimplePie("menus", () -> this.roundStat(Settings.IMP.MAIN.MENUS.size())));
    metrics.addCustomChart(new SimplePie("npcs", () -> this.roundStat(Settings.IMP.MAIN.NPCS.size())));
    metrics.addCustomChart(new SimplePie("holograms", () -> this.roundStat(Settings.IMP.MAIN.HOLOGRAMS.size())));

    try {
      if (!UpdatesChecker.checkVersionByURL("https://raw.githubusercontent.com/Elytrium/LimboHub/master/VERSION", Settings.IMP.VERSION)) {
        LOGGER.error("****************************************");
        LOGGER.warn("The new LimboHub update was found, please update.");
        LOGGER.error("https://github.com/Elytrium/LimboHub/releases/");
        LOGGER.error("****************************************");
      }
    } catch (Exception e) {
      LOGGER.error("Unable to check for updates.");
    }
  }

  public void reload() {
    Settings.IMP.reload(this.configFile);

    resetEntityCounter();

    ComponentSerializer<Component, Component, String> serializer = Settings.IMP.SERIALIZER.getSerializer();
    if (serializer == null) {
      LOGGER.warn("The specified serializer could not be found, using default. (LEGACY_AMPERSAND)");
      setSerializer(new Serializer(Objects.requireNonNull(Serializers.LEGACY_AMPERSAND.getSerializer())));
    } else {
      setSerializer(new Serializer(serializer));
    }

    Settings.MAIN.PLAYER_COORDS playerCoords = Settings.IMP.MAIN.PLAYER_COORDS;
    VirtualWorld hubWorld = this.limboFactory.createVirtualWorld(
        Settings.IMP.MAIN.DIMENSION,
        playerCoords.X, playerCoords.Y, playerCoords.Z,
        (float) playerCoords.YAW, (float) playerCoords.PITCH
    );

    try {
      Path worldPath = this.dataDirectory.resolve(Settings.IMP.MAIN.WORLD_FILE_PATH);
      if (Files.exists(worldPath)) {
        WorldFile worldFile = this.limboFactory.openWorldFile(Settings.IMP.MAIN.WORLD_FILE_TYPE, worldPath);

        Settings.MAIN.WORLD_COORDS worldCoords = Settings.IMP.MAIN.WORLD_COORDS;
        worldFile.toWorld(this.limboFactory, hubWorld, worldCoords.X, worldCoords.Y, worldCoords.Z, Settings.IMP.MAIN.WORLD_LIGHT_LEVEL);
      } else {
        LOGGER.warn("World '{}' could not be found.", worldPath.getFileName());
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }

    if (Settings.IMP.MAIN.WORLD_OVERRIDE_BLOCK_LIGHT_LEVEL) {
      hubWorld.fillBlockLight(Settings.IMP.MAIN.WORLD_LIGHT_LEVEL);
    }

    this.menus.clear();
    Settings.IMP.MAIN.MENUS.forEach(menu -> {
      if (menu.MENU_ID.isBlank()) {
        throw new IllegalArgumentException("Menu ID cannot be blank.");
      }

      Map<String, ItemStack> items = menu.ITEMS.stream()
          .collect(Collectors.toUnmodifiableMap(
              item -> {
                if (item.ID.length() != 1) {
                  throw new IllegalArgumentException("Item ID should be 1 character length.");
                }

                return item.ID;
              },
              item -> {
                VirtualItem virtualItem = this.limboFactory.getItem(item.ITEM);
                if (virtualItem == null) {
                  throw new IllegalArgumentException("Item '" + item.ITEM + "' not found.");
                }

                List<VirtualItem> fallbackItems = item.FALLBACK_ITEMS.stream().map(id -> {
                  VirtualItem fallbackVirtualItem = this.limboFactory.getItem(id);
                  if (fallbackVirtualItem == null) {
                    throw new IllegalArgumentException("Fallback item '" + id + "' for '" + item.ITEM + "' not found.");
                  }

                  return fallbackVirtualItem;
                }).collect(Collectors.toUnmodifiableList());

                Component nameComponent = buildResetComponent().append(getSerializer().deserialize(item.CUSTOM_NAME));
                List<Component> loreComponents = item.LORE == null ? null :
                    item.LORE.stream()
                        .map(line -> buildResetComponent().append(getSerializer().deserialize(line)))
                        .collect(Collectors.toList());

                return new ItemStack(virtualItem, fallbackItems, item.COUNT, item.DATA, new StaticItemMeta(
                    nameComponent, loreComponents, item.HAS_COLOR, item.COLOR, item.ENCHANTED, item.SKULL_OWNER));
              }
          ));

      if (menu.MENU_CONTENTS.isEmpty()) {
        throw new IllegalArgumentException("Menu (" + menu.MENU_ID + ") cannot be empty.");
      }

      int columns = menu.MENU_CONTENTS.get(0).length();
      int rows = menu.MENU_CONTENTS.size();

      Component titleComponent = getSerializer().deserialize(menu.TITLE);
      Container container = Container.genericContainer(columns, rows, titleComponent);

      for (int row = 0; row < rows; row++) {
        String format = menu.MENU_CONTENTS.get(row);
        if (format.length() != columns) {
          throw new IllegalArgumentException("Row #" + row + " length doesn't match first row length in menu " + menu.MENU_ID);
        }

        for (int column = 0; column < columns; column++) {
          String itemId = format.substring(column, column + 1);
          ItemStack item = " ".equals(itemId) ? ItemStack.EMPTY : items.get(itemId);
          if (item == null) {
            throw new IllegalArgumentException("Item with id '" + itemId + "' is not defined.");
          }

          container.setItem(column, row, item);
        }
      }

      Map<Integer, Settings.MAIN.ACTION> actions = new HashMap<>();
      menu.ACTIONS.forEach(action -> action.SLOTS.forEach(slot -> {
        int slotIndex;
        if (slot.contains(",")) {
          int[] position = Arrays.stream(slot.split(",")).mapToInt(Integer::parseInt).toArray();
          slotIndex = position[1] * columns + position[0];
        } else {
          slotIndex = Integer.parseInt(slot);
        }

        actions.put(slotIndex, action.ACTION);
      }));

      this.menus.put(menu.MENU_ID, new Menu(container, actions, menu.DEFAULT_ACTION.getAction()));
    });

    if (!Settings.IMP.MAIN.BOSSBARS.isEmpty()) {
      this.bossBar = this.buildLinkedBossBar(0, null);
    } else {
      this.bossBar = null;
    }

    if (Settings.IMP.MAIN.DEFAULT_MENU != null && !Settings.IMP.MAIN.DEFAULT_MENU.isBlank()) {
      this.defaultMenu = this.menus.get(Settings.IMP.MAIN.DEFAULT_MENU);
    } else {
      this.defaultMenu = null;
    }

    if (Settings.IMP.MAIN.SIDEBAR.ENABLED) {
      this.sidebar = new Sidebar(
          getSerializer().deserialize(Settings.IMP.MAIN.SIDEBAR.TITLE),
          Settings.IMP.MAIN.SIDEBAR.LINES.stream().map(getSerializer()::deserialize).collect(Collectors.toUnmodifiableList())
      );
    } else {
      this.sidebar = null;
    }

    this.npcs.clear();
    Settings.IMP.MAIN.NPCS.forEach(data -> {
      NPC npc = new NPC(
          data.DISPLAY_NAME.isBlank() ? null : getSerializer().deserialize(data.DISPLAY_NAME), data.X, data.Y,
          data.Z, (float) data.YAW, (float) data.PITCH, data.LOAD_SKIN ? data.SKIN_DATA : null, data.ACTION, data.COOLDOWN
      );

      this.npcs.put(npc.getEntityId(), npc);
    });

    this.holograms.clear();
    Settings.IMP.MAIN.HOLOGRAMS.forEach(hologram -> this.holograms.add(
        new Hologram(
            hologram.X, hologram.Y, hologram.Z,
            hologram.LINES.stream().map(getSerializer()::deserialize).collect(Collectors.toUnmodifiableList())
        )
    ));

    if (this.hubServer != null) {
      this.hubServer.dispose();
    }

    this.hubServer = this.limboFactory
        .createLimbo(hubWorld)
        .setName("LimboHub")
        .setWorldTime(Settings.IMP.MAIN.WORLD_TICKS)
        .setGameMode(Settings.IMP.MAIN.GAME_MODE)
        .setMaxSuppressPacketLength(Settings.IMP.MAIN.MAX_SUPPRESS_PACKET_LENGTH)
        .registerPacket(PacketDirection.CLIENTBOUND, SpawnEntity.class, SpawnEntity::new, new PacketMapping[]{
            new PacketMapping(0x0E, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x00, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x01, ProtocolVersion.MINECRAFT_1_19_4, true),
        })
        .registerPacket(PacketDirection.CLIENTBOUND, SpawnPlayer.class, SpawnPlayer::new, new PacketMapping[]{
            new PacketMapping(0x0C, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x05, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x04, ProtocolVersion.MINECRAFT_1_16, true),
            new PacketMapping(0x02, ProtocolVersion.MINECRAFT_1_19, true),
            new PacketMapping(0x03, ProtocolVersion.MINECRAFT_1_19_4, true),
            // There is no SpawnPlayer packet since Minecraft 1.20.2
        })
        .registerPacket(PacketDirection.CLIENTBOUND, CloseContainer.class, CloseContainer::new, new PacketMapping[]{
            new PacketMapping(0x2E, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x12, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x13, ProtocolVersion.MINECRAFT_1_13, true),
            new PacketMapping(0x14, ProtocolVersion.MINECRAFT_1_15, true),
            new PacketMapping(0x13, ProtocolVersion.MINECRAFT_1_16, true),
            new PacketMapping(0x12, ProtocolVersion.MINECRAFT_1_16_2, true),
            new PacketMapping(0x13, ProtocolVersion.MINECRAFT_1_17, true),
            new PacketMapping(0x10, ProtocolVersion.MINECRAFT_1_19, true),
            new PacketMapping(0x0F, ProtocolVersion.MINECRAFT_1_19_3, true),
            new PacketMapping(0x11, ProtocolVersion.MINECRAFT_1_19_4, true),
            new PacketMapping(0x12, ProtocolVersion.MINECRAFT_1_20_2, true),
        })
        .registerPacket(PacketDirection.CLIENTBOUND, SetContainerContent.class, SetContainerContent::new, new PacketMapping[]{
            new PacketMapping(0x30, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x14, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x15, ProtocolVersion.MINECRAFT_1_13, true),
            new PacketMapping(0x14, ProtocolVersion.MINECRAFT_1_14, true),
            new PacketMapping(0x15, ProtocolVersion.MINECRAFT_1_15, true),
            new PacketMapping(0x14, ProtocolVersion.MINECRAFT_1_16, true),
            new PacketMapping(0x13, ProtocolVersion.MINECRAFT_1_16_2, true),
            new PacketMapping(0x14, ProtocolVersion.MINECRAFT_1_17, true),
            new PacketMapping(0x11, ProtocolVersion.MINECRAFT_1_19, true),
            new PacketMapping(0x10, ProtocolVersion.MINECRAFT_1_19_3, true),
            new PacketMapping(0x12, ProtocolVersion.MINECRAFT_1_19_4, true),
            new PacketMapping(0x13, ProtocolVersion.MINECRAFT_1_20_2, true),
        })
        .registerPacket(PacketDirection.CLIENTBOUND, SetContainerSlot.class, SetContainerSlot::new, new PacketMapping[]{
            new PacketMapping(0x2F, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x16, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x17, ProtocolVersion.MINECRAFT_1_13, true),
            new PacketMapping(0x16, ProtocolVersion.MINECRAFT_1_14, true),
            new PacketMapping(0x17, ProtocolVersion.MINECRAFT_1_15, true),
            new PacketMapping(0x16, ProtocolVersion.MINECRAFT_1_16, true),
            new PacketMapping(0x15, ProtocolVersion.MINECRAFT_1_16_2, true),
            new PacketMapping(0x16, ProtocolVersion.MINECRAFT_1_17, true),
            new PacketMapping(0x13, ProtocolVersion.MINECRAFT_1_19, true),
            new PacketMapping(0x12, ProtocolVersion.MINECRAFT_1_19_3, true),
            new PacketMapping(0x14, ProtocolVersion.MINECRAFT_1_19_4, true),
            new PacketMapping(0x15, ProtocolVersion.MINECRAFT_1_20_2, true),
        })
        .registerPacket(PacketDirection.CLIENTBOUND, OpenScreen.class, OpenScreen::new, new PacketMapping[]{
            new PacketMapping(0x2D, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x13, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x14, ProtocolVersion.MINECRAFT_1_13, true),
            new PacketMapping(0x2E, ProtocolVersion.MINECRAFT_1_14, true),
            new PacketMapping(0x2F, ProtocolVersion.MINECRAFT_1_15, true),
            new PacketMapping(0x2E, ProtocolVersion.MINECRAFT_1_16, true),
            new PacketMapping(0x2D, ProtocolVersion.MINECRAFT_1_16_2, true),
            new PacketMapping(0x2E, ProtocolVersion.MINECRAFT_1_17, true),
            new PacketMapping(0x2B, ProtocolVersion.MINECRAFT_1_19, true),
            new PacketMapping(0x2D, ProtocolVersion.MINECRAFT_1_19_1, true),
            new PacketMapping(0x2C, ProtocolVersion.MINECRAFT_1_19_3, true),
            new PacketMapping(0x30, ProtocolVersion.MINECRAFT_1_19_4, true),
            new PacketMapping(0x31, ProtocolVersion.MINECRAFT_1_20_2, true),
            new PacketMapping(0x33, ProtocolVersion.MINECRAFT_1_20_5, true),
            new PacketMapping(0x35, ProtocolVersion.MINECRAFT_1_21_2, true),
        })
        .registerPacket(PacketDirection.CLIENTBOUND, SetHeadRotation.class, SetHeadRotation::new, new PacketMapping[]{
            new PacketMapping(0x19, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x34, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x35, ProtocolVersion.MINECRAFT_1_12, true),
            new PacketMapping(0x36, ProtocolVersion.MINECRAFT_1_12_1, true),
            new PacketMapping(0x39, ProtocolVersion.MINECRAFT_1_13, true),
            new PacketMapping(0x3B, ProtocolVersion.MINECRAFT_1_14, true),
            new PacketMapping(0x3C, ProtocolVersion.MINECRAFT_1_15, true),
            new PacketMapping(0x3B, ProtocolVersion.MINECRAFT_1_16, true),
            new PacketMapping(0x3A, ProtocolVersion.MINECRAFT_1_16_2, true),
            new PacketMapping(0x3E, ProtocolVersion.MINECRAFT_1_17, true),
            new PacketMapping(0x3C, ProtocolVersion.MINECRAFT_1_19, true),
            new PacketMapping(0x3F, ProtocolVersion.MINECRAFT_1_19_1, true),
            new PacketMapping(0x3E, ProtocolVersion.MINECRAFT_1_19_3, true),
            new PacketMapping(0x42, ProtocolVersion.MINECRAFT_1_19_4, true),
            new PacketMapping(0x44, ProtocolVersion.MINECRAFT_1_20_2, true),
            new PacketMapping(0x46, ProtocolVersion.MINECRAFT_1_20_3, true),
            new PacketMapping(0x48, ProtocolVersion.MINECRAFT_1_20_5, true),
            new PacketMapping(0x4D, ProtocolVersion.MINECRAFT_1_21_2, true),
        })
        .registerPacket(PacketDirection.CLIENTBOUND, DisplayObjective.class, DisplayObjective::new, new PacketMapping[]{
            new PacketMapping(0x3D, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x38, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x3A, ProtocolVersion.MINECRAFT_1_12, true),
            new PacketMapping(0x3B, ProtocolVersion.MINECRAFT_1_12_1, true),
            new PacketMapping(0x3E, ProtocolVersion.MINECRAFT_1_13, true),
            new PacketMapping(0x42, ProtocolVersion.MINECRAFT_1_14, true),
            new PacketMapping(0x43, ProtocolVersion.MINECRAFT_1_15, true),
            new PacketMapping(0x4C, ProtocolVersion.MINECRAFT_1_17, true),
            new PacketMapping(0x4F, ProtocolVersion.MINECRAFT_1_19_1, true),
            new PacketMapping(0x4D, ProtocolVersion.MINECRAFT_1_19_3, true),
            new PacketMapping(0x51, ProtocolVersion.MINECRAFT_1_19_4, true),
            new PacketMapping(0x53, ProtocolVersion.MINECRAFT_1_20_2, true),
            new PacketMapping(0x55, ProtocolVersion.MINECRAFT_1_20_3, true),
            new PacketMapping(0x57, ProtocolVersion.MINECRAFT_1_20_5, true),
            new PacketMapping(0x5C, ProtocolVersion.MINECRAFT_1_21_2, true),
        })
        .registerPacket(PacketDirection.CLIENTBOUND, SetEntityMetadata.class, SetEntityMetadata::new, new PacketMapping[]{
            new PacketMapping(0x1C, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x39, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x3B, ProtocolVersion.MINECRAFT_1_12, true),
            new PacketMapping(0x3C, ProtocolVersion.MINECRAFT_1_12_1, true),
            new PacketMapping(0x3F, ProtocolVersion.MINECRAFT_1_13, true),
            new PacketMapping(0x43, ProtocolVersion.MINECRAFT_1_14, true),
            new PacketMapping(0x44, ProtocolVersion.MINECRAFT_1_15, true),
            new PacketMapping(0x4D, ProtocolVersion.MINECRAFT_1_17, true),
            new PacketMapping(0x50, ProtocolVersion.MINECRAFT_1_19_1, true),
            new PacketMapping(0x4E, ProtocolVersion.MINECRAFT_1_19_3, true),
            new PacketMapping(0x52, ProtocolVersion.MINECRAFT_1_19_4, true),
            new PacketMapping(0x54, ProtocolVersion.MINECRAFT_1_20_2, true),
            new PacketMapping(0x56, ProtocolVersion.MINECRAFT_1_20_3, true),
            new PacketMapping(0x58, ProtocolVersion.MINECRAFT_1_20_5, true),
            new PacketMapping(0x5D, ProtocolVersion.MINECRAFT_1_21_2, true),
        })
        .registerPacket(PacketDirection.CLIENTBOUND, UpdateObjectives.class, UpdateObjectives::new, new PacketMapping[]{
            new PacketMapping(0x3B, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x3F, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x41, ProtocolVersion.MINECRAFT_1_12, true),
            new PacketMapping(0x42, ProtocolVersion.MINECRAFT_1_12_1, true),
            new PacketMapping(0x45, ProtocolVersion.MINECRAFT_1_13, true),
            new PacketMapping(0x49, ProtocolVersion.MINECRAFT_1_14, true),
            new PacketMapping(0x4A, ProtocolVersion.MINECRAFT_1_15, true),
            new PacketMapping(0x53, ProtocolVersion.MINECRAFT_1_17, true),
            new PacketMapping(0x56, ProtocolVersion.MINECRAFT_1_19_1, true),
            new PacketMapping(0x54, ProtocolVersion.MINECRAFT_1_19_3, true),
            new PacketMapping(0x58, ProtocolVersion.MINECRAFT_1_19_4, true),
            new PacketMapping(0x5A, ProtocolVersion.MINECRAFT_1_20_2, true),
            new PacketMapping(0x5C, ProtocolVersion.MINECRAFT_1_20_3, true),
            new PacketMapping(0x5E, ProtocolVersion.MINECRAFT_1_20_5, true),
            new PacketMapping(0x64, ProtocolVersion.MINECRAFT_1_21_2, true),
        })
        .registerPacket(PacketDirection.CLIENTBOUND, ScoreboardTeam.class, ScoreboardTeam::new, new PacketMapping[]{
            new PacketMapping(0x3E, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x41, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x43, ProtocolVersion.MINECRAFT_1_12, true),
            new PacketMapping(0x44, ProtocolVersion.MINECRAFT_1_12_1, true),
            new PacketMapping(0x47, ProtocolVersion.MINECRAFT_1_13, true),
            new PacketMapping(0x4B, ProtocolVersion.MINECRAFT_1_14, true),
            new PacketMapping(0x4C, ProtocolVersion.MINECRAFT_1_15, true),
            new PacketMapping(0x55, ProtocolVersion.MINECRAFT_1_17, true),
            new PacketMapping(0x58, ProtocolVersion.MINECRAFT_1_19_1, true),
            new PacketMapping(0x56, ProtocolVersion.MINECRAFT_1_19_3, true),
            new PacketMapping(0x5A, ProtocolVersion.MINECRAFT_1_19_4, true),
            new PacketMapping(0x5C, ProtocolVersion.MINECRAFT_1_20_2, true),
            new PacketMapping(0x5E, ProtocolVersion.MINECRAFT_1_20_3, true),
            new PacketMapping(0x60, ProtocolVersion.MINECRAFT_1_20_5, true),
            new PacketMapping(0x67, ProtocolVersion.MINECRAFT_1_21_2, true),
        })
        .registerPacket(PacketDirection.CLIENTBOUND, UpdateScore.class, UpdateScore::new, new PacketMapping[]{
            new PacketMapping(0x3C, ProtocolVersion.MINECRAFT_1_8, true),
            new PacketMapping(0x42, ProtocolVersion.MINECRAFT_1_9, true),
            new PacketMapping(0x44, ProtocolVersion.MINECRAFT_1_12, true),
            new PacketMapping(0x45, ProtocolVersion.MINECRAFT_1_12_1, true),
            new PacketMapping(0x48, ProtocolVersion.MINECRAFT_1_13, true),
            new PacketMapping(0x4C, ProtocolVersion.MINECRAFT_1_14, true),
            new PacketMapping(0x4D, ProtocolVersion.MINECRAFT_1_15, true),
            new PacketMapping(0x56, ProtocolVersion.MINECRAFT_1_17, true),
            new PacketMapping(0x59, ProtocolVersion.MINECRAFT_1_19_1, true),
            new PacketMapping(0x57, ProtocolVersion.MINECRAFT_1_19_3, true),
            new PacketMapping(0x5B, ProtocolVersion.MINECRAFT_1_19_4, true),
            new PacketMapping(0x5D, ProtocolVersion.MINECRAFT_1_20_2, true),
            new PacketMapping(0x5F, ProtocolVersion.MINECRAFT_1_20_3, true),
            new PacketMapping(0x61, ProtocolVersion.MINECRAFT_1_20_5, true),
            new PacketMapping(0x68, ProtocolVersion.MINECRAFT_1_21_2, true),
        })
        .registerPacket(PacketDirection.SERVERBOUND, Interact.class, Interact::new, new PacketMapping[]{
            new PacketMapping(0x02, ProtocolVersion.MINECRAFT_1_8, false),
            new PacketMapping(0x0A, ProtocolVersion.MINECRAFT_1_9, false),
            new PacketMapping(0x0B, ProtocolVersion.MINECRAFT_1_12, false),
            new PacketMapping(0x0A, ProtocolVersion.MINECRAFT_1_12_1, false),
            new PacketMapping(0x0D, ProtocolVersion.MINECRAFT_1_13, false),
            new PacketMapping(0x0E, ProtocolVersion.MINECRAFT_1_14, false),
            new PacketMapping(0x0D, ProtocolVersion.MINECRAFT_1_17, false),
            new PacketMapping(0x0F, ProtocolVersion.MINECRAFT_1_19, false),
            new PacketMapping(0x10, ProtocolVersion.MINECRAFT_1_19_1, false),
            new PacketMapping(0x0F, ProtocolVersion.MINECRAFT_1_19_3, false),
            new PacketMapping(0x10, ProtocolVersion.MINECRAFT_1_19_4, false),
            new PacketMapping(0x12, ProtocolVersion.MINECRAFT_1_20_2, false),
            new PacketMapping(0x13, ProtocolVersion.MINECRAFT_1_20_3, false),
            new PacketMapping(0x16, ProtocolVersion.MINECRAFT_1_20_5, false),
            new PacketMapping(0x18, ProtocolVersion.MINECRAFT_1_21_2, false),
        })
        .registerPacket(PacketDirection.SERVERBOUND, ClickContainer.class, ClickContainer::new, new PacketMapping[]{
            new PacketMapping(0x0E, ProtocolVersion.MINECRAFT_1_8, false),
            new PacketMapping(0x07, ProtocolVersion.MINECRAFT_1_9, false),
            new PacketMapping(0x08, ProtocolVersion.MINECRAFT_1_12, false),
            new PacketMapping(0x07, ProtocolVersion.MINECRAFT_1_12_1, false),
            new PacketMapping(0x08, ProtocolVersion.MINECRAFT_1_13, false),
            new PacketMapping(0x09, ProtocolVersion.MINECRAFT_1_14, false),
            new PacketMapping(0x08, ProtocolVersion.MINECRAFT_1_17, false),
            new PacketMapping(0x0A, ProtocolVersion.MINECRAFT_1_19, false),
            new PacketMapping(0x0B, ProtocolVersion.MINECRAFT_1_19_1, false),
            new PacketMapping(0x0A, ProtocolVersion.MINECRAFT_1_19_3, false),
            new PacketMapping(0x0B, ProtocolVersion.MINECRAFT_1_19_4, false),
            new PacketMapping(0x0D, ProtocolVersion.MINECRAFT_1_20_2, false),
            new PacketMapping(0x0E, ProtocolVersion.MINECRAFT_1_20_5, false),
            new PacketMapping(0x10, ProtocolVersion.MINECRAFT_1_21_2, false),
        });

    this.commands.clear();
    Settings.IMP.MAIN.COMMANDS.forEach(command -> {
      List<String> aliases = Stream.concat(Stream.of(command.COMMAND), command.ALIASES.stream()).collect(Collectors.toUnmodifiableList());
      aliases.forEach(alias -> this.commands.put(alias, command.ACTION));
      this.hubServer.registerCommand(new LimboCommandMeta(aliases));
    });

    EventManager eventManager = this.server.getEventManager();
    eventManager.unregisterListeners(this);
    eventManager.register(this, new HubListener(this));

    CommandManager commandManager = this.server.getCommandManager();
    if (this.currentHubCommand != null) {
      commandManager.unregister(this.currentHubCommand);
    }

    if (!Settings.IMP.MAIN.HUB_COMMAND.ALIASES.isEmpty()) {
      String mainAlias = Settings.IMP.MAIN.HUB_COMMAND.ALIASES.get(0);
      String[] otherAliases = Settings.IMP.MAIN.HUB_COMMAND.ALIASES.stream().skip(1).toArray(String[]::new);
      commandManager.register(mainAlias, new HubCommand(this), otherAliases);
      this.currentHubCommand = mainAlias;
    } else {
      this.currentHubCommand = null;
    }

    commandManager.unregister("limbohubreload");
    commandManager.register("limbohubreload", new ReloadCommand(this));
  }

  public LinkedBossBar buildLinkedBossBar(int index, LinkedBossBar first) {
    Settings.MAIN.BOSSBAR data = Settings.IMP.MAIN.BOSSBARS.get(index);
    LinkedBossBar linkedBossBar;

    BossBar currentBossBar;
    if (!data.HIDDEN) {
      currentBossBar = BossBar.bossBar(getSerializer().deserialize(data.NAME), (float) data.PROGRESS, data.COLOR, data.OVERLAY);
    } else {
      currentBossBar = null;
    }

    if (data.STAY_TIME_MILLIS == -1) {
      linkedBossBar = new LinkedBossBar(currentBossBar, -1, null);
    } else {
      linkedBossBar = new LinkedBossBar(currentBossBar, data.STAY_TIME_MILLIS, null);
      if (index + 1 >= Settings.IMP.MAIN.BOSSBARS.size()) {
        linkedBossBar.setNext(first);
      } else {
        linkedBossBar.setNext(this.buildLinkedBossBar(index + 1, first != null ? first : linkedBossBar));
      }
    }

    return linkedBossBar;
  }

  public void sendToHub(Player player) {
    this.hubServer.spawnPlayer(player, new HubSessionHandler(player, this));
  }

  public Path getDataDirectory() {
    return this.dataDirectory;
  }

  public File getConfigFile() {
    return this.configFile;
  }

  public ProxyServer getServer() {
    return this.server;
  }

  public LimboFactory getLimboFactory() {
    return this.limboFactory;
  }

  public Limbo getHubServer() {
    return this.hubServer;
  }

  public Map<String, Menu> getMenus() {
    return this.menus;
  }

  public Map<String, Settings.MAIN.ACTION> getCommands() {
    return this.commands;
  }

  public Map<Integer, NPC> getNpcs() {
    return this.npcs;
  }

  public List<Hologram> getHolograms() {
    return this.holograms;
  }

  public LinkedBossBar getBossBar() {
    return this.bossBar;
  }

  public Sidebar getSidebar() {
    return this.sidebar;
  }

  public Menu getDefaultMenu() {
    return this.defaultMenu;
  }

  private static void setLogger(Logger logger) {
    LOGGER = logger;
  }

  public static Logger getLogger() {
    return LOGGER;
  }

  private static void setSerializer(Serializer serializer) {
    SERIALIZER = serializer;
  }

  public static Serializer getSerializer() {
    return SERIALIZER;
  }

  public static LimboHub getInstance() {
    return INSTANCE;
  }

  public static Component buildResetComponent() {
    return Component.empty().decoration(TextDecoration.ITALIC, false);
  }

  private static void resetEntityCounter() {
    ENTITY_ID_COUNTER = 10;
  }

  public static int reserveEntityId() {
    return ENTITY_ID_COUNTER++;
  }

  public static int reserveEntityIds(int count) {
    try {
      return ENTITY_ID_COUNTER;
    } finally {
      ENTITY_ID_COUNTER += count;
    }
  }
}
