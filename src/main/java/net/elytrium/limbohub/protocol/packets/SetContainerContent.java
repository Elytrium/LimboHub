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
import java.util.Objects;
import net.elytrium.limbohub.protocol.container.Container;
import net.elytrium.limbohub.protocol.item.ItemStack;
import net.elytrium.limbohub.utils.ProtocolTools;

public class SetContainerContent implements MinecraftPacket {

  private final Container container;

  public SetContainerContent(Container container) {
    this.container = container;
  }

  public SetContainerContent() {
    throw new IllegalArgumentException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolTools.writeContainerId(buf, protocolVersion, this.container != null ? this.container.getId() : 0);
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17_1) >= 0) {
      buf.writeByte(0);
    }
    if (this.container != null) {
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17_1) >= 0) {
        ProtocolUtils.writeVarInt(buf, this.container.getContents().length);
      } else {
        buf.writeShort(this.container.getContents().length);
      }
      for (ItemStack item : this.container.getContents()) {
        Objects.requireNonNullElse(item, ItemStack.EMPTY)
            .encode(buf, protocolVersion);
      }
    } else {
      int slots = 45;
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_16_4) <= 0) {
        slots = 46;
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17_1) >= 0) {
        ProtocolUtils.writeVarInt(buf, slots);
      } else {
        buf.writeShort(slots);
      }
      for (int i = 0; i < slots; ++i) {
        ItemStack.EMPTY.encode(buf, protocolVersion);
      }
    }
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17_1) >= 0) {
      ItemStack.EMPTY.encode(buf, protocolVersion);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  public Container getContainer() {
    return this.container;
  }
}
