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
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class UpdateObjectives implements MinecraftPacket {

  private final String objectiveName;
  private final byte mode;
  private final Component objectiveValue;
  private final int type;

  public UpdateObjectives(String objectiveName, byte mode, Component objectiveValue, int type) {
    this.objectiveName = objectiveName;
    this.mode = mode;
    this.objectiveValue = objectiveValue;
    this.type = type;
  }

  public UpdateObjectives() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, this.objectiveName);
    buf.writeByte(this.mode);
    if (this.mode == 0 || this.mode == 2) {
      ComponentSerializer<Component, ?, String> serializer = protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0
          ? ProtocolUtils.getJsonChatSerializer(protocolVersion) : LegacyComponentSerializer.legacySection();
      ProtocolUtils.writeString(buf, serializer.serialize(this.objectiveValue));
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
        ProtocolUtils.writeVarInt(buf, this.type);
      } else {
        if (this.type == 0) {
          ProtocolUtils.writeString(buf, "integer");
        } else if (this.type == 1) {
          ProtocolUtils.writeString(buf, "hearts");
        } else {
          throw new IllegalArgumentException("Invalid type: " + this.type);
        }
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  public String getObjectiveName() {
    return this.objectiveName;
  }

  public byte getMode() {
    return this.mode;
  }

  public Component getObjectiveValue() {
    return this.objectiveValue;
  }

  public int getType() {
    return this.type;
  }
}
