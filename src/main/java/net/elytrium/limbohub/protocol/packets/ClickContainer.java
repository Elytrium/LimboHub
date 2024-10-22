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
import net.elytrium.limbohub.utils.ProtocolTools;

public class ClickContainer implements MinecraftPacket {

  private int windowId;
  private int stateId;
  private short slot;

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    this.windowId = ProtocolTools.readContainerId(buf, protocolVersion);
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17_1) >= 0) {
      this.stateId = ProtocolUtils.readVarInt(buf);
    }
    this.slot = buf.readShort();
    buf.skipBytes(buf.readableBytes());
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    handler.handleGeneric(this);
    return true;
  }

  public int getWindowId() {
    return this.windowId;
  }

  public int getStateId() {
    return this.stateId;
  }

  public short getSlot() {
    return this.slot;
  }
}
