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

public class DisplayObjective implements MinecraftPacket {

  private final byte position;
  private final String scoreName;

  public DisplayObjective(byte position, String scoreName) {
    this.position = position;
    this.scoreName = scoreName;
  }

  public DisplayObjective() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0) {
      ProtocolUtils.writeVarInt(buf, this.position);
    } else {
      buf.writeByte(this.position);
    }

    ProtocolUtils.writeString(buf, this.scoreName);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  public byte getPosition() {
    return this.position;
  }

  public String getScoreName() {
    return this.scoreName;
  }
}
