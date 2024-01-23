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

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class UpdateScore implements MinecraftPacket {

  private final Component entityName;
  private final int action;
  private final String objectiveName;
  private final int value;

  public UpdateScore(Component entityName, int action, String objectiveName, int value) {
    this.entityName = entityName;
    this.action = action;
    this.objectiveName = objectiveName;
    this.value = value;
  }

  public UpdateScore() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    String serializedName = LegacyComponentSerializer.legacySection().serialize(this.entityName);
    if (serializedName.length() > 40) {
      serializedName = serializedName.substring(0, 40);
    }
    ProtocolUtils.writeString(buf, serializedName);
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_20_3) >= 0) {
      Preconditions.checkState(this.action == 0, "unsupported action");
      ProtocolUtils.writeString(buf, this.objectiveName);
      ProtocolUtils.writeVarInt(buf, this.value);

      buf.writeBoolean(true);
      new ComponentHolder(protocolVersion, this.entityName).write(buf);

      buf.writeBoolean(false); // no custom format
    } else {
      buf.writeByte(this.action);
      ProtocolUtils.writeString(buf, this.objectiveName);
      if (this.action != 1) {
        ProtocolUtils.writeVarInt(buf, this.value);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  public Component getEntityName() {
    return this.entityName;
  }

  public int getAction() {
    return this.action;
  }

  public String getObjectiveName() {
    return this.objectiveName;
  }

  public int getValue() {
    return this.value;
  }
}
