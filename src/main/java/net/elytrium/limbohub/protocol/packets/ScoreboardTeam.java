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

package net.elytrium.limbohub.protocol.packets;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ScoreboardTeam implements MinecraftPacket {

  private final String teamName;
  private final byte flags;
  private final String nameTagVisibility;
  private final String collisionRule;
  private final int teamColor;
  private final Component teamPrefix;
  private final Component teamSuffix;
  private final List<String> entities;

  public ScoreboardTeam(String teamName, byte flags, String nameTagVisibility, String collisionRule,
                        int teamColor, Component teamPrefix, Component teamSuffix, List<String> entities) {
    this.teamName = teamName;
    this.flags = flags;
    this.nameTagVisibility = nameTagVisibility;
    this.collisionRule = collisionRule;
    this.teamColor = teamColor;
    this.teamPrefix = teamPrefix;
    this.teamSuffix = teamSuffix;
    this.entities = entities;
  }

  public ScoreboardTeam() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, this.teamName);
    buf.writeByte(0);
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
      new ComponentHolder(protocolVersion, Component.empty()).write(buf);
      buf.writeByte(this.flags);
      ProtocolUtils.writeString(buf, this.nameTagVisibility);
      ProtocolUtils.writeString(buf, this.collisionRule);
      ProtocolUtils.writeVarInt(buf, this.teamColor);
      new ComponentHolder(protocolVersion, this.teamPrefix).write(buf);
      new ComponentHolder(protocolVersion, this.teamSuffix).write(buf);
    } else {
      LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
      buf.writeByte(0);
      ProtocolUtils.writeString(buf, serializer.serialize(this.teamPrefix));
      ProtocolUtils.writeString(buf, serializer.serialize(this.teamSuffix));
      buf.writeByte(this.flags);
      ProtocolUtils.writeString(buf, this.nameTagVisibility);
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
        ProtocolUtils.writeString(buf, this.collisionRule);
      }
      ProtocolUtils.writeVarInt(buf, this.teamColor);
    }
    ProtocolUtils.writeVarInt(buf, this.entities.size());
    this.entities.forEach(entity -> ProtocolUtils.writeString(buf, entity));
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  public String getTeamName() {
    return this.teamName;
  }

  public byte getFlags() {
    return this.flags;
  }

  public String getNameTagVisibility() {
    return this.nameTagVisibility;
  }

  public String getCollisionRule() {
    return this.collisionRule;
  }

  public int getTeamColor() {
    return this.teamColor;
  }

  public Component getTeamPrefix() {
    return this.teamPrefix;
  }

  public Component getTeamSuffix() {
    return this.teamSuffix;
  }

  public List<String> getEntities() {
    return this.entities;
  }
}
