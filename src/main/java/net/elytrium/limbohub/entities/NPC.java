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

package net.elytrium.limbohub.entities;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItem;
import com.velocitypowered.proxy.protocol.packet.RemovePlayerInfo;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfo;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limbohub.LimboHub;
import net.elytrium.limbohub.Settings;
import net.elytrium.limbohub.protocol.entities.ArmorStand;
import net.elytrium.limbohub.protocol.entities.Player;
import net.elytrium.limbohub.protocol.packets.ScoreboardTeam;
import net.elytrium.limbohub.protocol.packets.SetEntityMetadata;
import net.elytrium.limbohub.protocol.packets.SetHeadRotation;
import net.elytrium.limbohub.protocol.packets.SpawnEntity;
import net.elytrium.limbohub.protocol.packets.SpawnPlayer;
import net.kyori.adventure.text.Component;

public class NPC {

  private final int entityId;
  private final UUID uuid;
  private final String username;
  private final Component displayName;
  private final double positionX;
  private final double positionY;
  private final double positionZ;
  private final float yaw;
  private final float pitch;
  private final Settings.MAIN.NPC.SKIN_DATA skinData;
  private final Settings.MAIN.ACTION action;

  public NPC(int entityId, UUID uuid, String username, Component displayName, double positionX, double positionY,
             double positionZ, float yaw, float pitch, Settings.MAIN.NPC.SKIN_DATA skinData, Settings.MAIN.ACTION action) {
    this.entityId = entityId;
    this.uuid = uuid;
    this.username = username;
    this.displayName = displayName;
    this.positionX = positionX;
    this.positionY = positionY;
    this.positionZ = positionZ;
    this.yaw = yaw;
    this.pitch = pitch;
    this.skinData = skinData;
    this.action = action;
  }

  public NPC(int entityId, Component displayName, double positionX, double positionY, double positionZ,
             float yaw, float pitch, Settings.MAIN.NPC.SKIN_DATA skinData, Settings.MAIN.ACTION action) {
    this(entityId, skinData != null ? UUID.fromString(skinData.UUID) : UUID.nameUUIDFromBytes(("NPC:" + entityId).getBytes(StandardCharsets.UTF_8)),
        "_npc" + entityId, displayName, positionX, positionY, positionZ, yaw, pitch, skinData, action);
  }

  public NPC(Component displayUsername, double positionX, double positionY, double positionZ,
             float yaw, float pitch, Settings.MAIN.NPC.SKIN_DATA skinData, Settings.MAIN.ACTION action) {
    this(LimboHub.reserveEntityIds(2), displayUsername, positionX, positionY, positionZ, yaw, pitch, skinData, action);
  }

  public void spawn(LimboPlayer player) {
    GameProfile profile = new GameProfile(this.uuid, this.username, List.of());
    if (this.skinData != null) {
      profile = profile.withProperties(List.of(new GameProfile.Property("textures", this.skinData.VALUE, this.skinData.SIGNATURE)));
    }

    if (player.getProxyPlayer().getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
      UpsertPlayerInfo.Entry entry = new UpsertPlayerInfo.Entry(this.uuid);
      entry.setProfile(profile);
      entry.setListed(false);

      player.writePacketAndFlush(
          new UpsertPlayerInfo(
              EnumSet.of(UpsertPlayerInfo.Action.ADD_PLAYER, UpsertPlayerInfo.Action.UPDATE_LISTED),
              List.of(entry)
          )
      );
    } else {
      player.writePacketAndFlush(
          new LegacyPlayerListItem(
              LegacyPlayerListItem.ADD_PLAYER,
              List.of(
                  new LegacyPlayerListItem.Item(this.uuid)
                      .setName(profile.getName())
                      .setProperties(profile.getProperties())
              )
          )
      );
    }

    player.writePacketAndFlush(new SpawnPlayer(this.entityId, this.uuid, this.positionX, this.positionY, this.positionZ, this.yaw, this.pitch));
    player.writePacketAndFlush(new SetHeadRotation(this.entityId, this.yaw));
    player.writePacketAndFlush(new ScoreboardTeam("sb" + this.entityId, (byte) 0, "never",
            "always", 0, Component.empty(), Component.empty(), List.of(this.username)));

    if (this.skinData != null) {
      byte skinParts = (byte) this.skinData.DISPLAYED_SKIN_PARTS;
      player.writePacketAndFlush(new SetEntityMetadata(this.entityId, version -> Player.buildSkinPartsMetadata(version, skinParts)));
    }

    if (this.displayName != null) {
      player.writePacketAndFlush(new SpawnEntity(this.entityId + 1, UUID.randomUUID(), ArmorStand::getEntityType,
          this.positionX, this.positionY - 0.175, this.positionZ, this.pitch, this.yaw, this.yaw, 0));
      player.writePacketAndFlush(new SetEntityMetadata(this.entityId + 1, version -> ArmorStand.buildHologramMetadata(version, this.displayName)));
    }
  }

  public void cleanUp(LimboPlayer player) {
    if (player.getProxyPlayer().getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
      player.writePacketAndFlush(new RemovePlayerInfo(List.of(this.uuid)));
    } else {
      player.writePacketAndFlush(
          new LegacyPlayerListItem(
              LegacyPlayerListItem.REMOVE_PLAYER,
              List.of(new LegacyPlayerListItem.Item(this.uuid))
          )
      );
    }
  }

  public int getEntityId() {
    return this.entityId;
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public String getUsername() {
    return this.username;
  }

  public Component getDisplayName() {
    return this.displayName;
  }

  public double getPositionX() {
    return this.positionX;
  }

  public double getPositionY() {
    return this.positionY;
  }

  public double getPositionZ() {
    return this.positionZ;
  }

  public float getYaw() {
    return this.yaw;
  }

  public float getPitch() {
    return this.pitch;
  }

  public Settings.MAIN.ACTION getAction() {
    return this.action;
  }
}
