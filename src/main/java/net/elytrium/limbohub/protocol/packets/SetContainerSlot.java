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
import net.elytrium.limbohub.protocol.container.Container;
import net.elytrium.limbohub.protocol.item.ItemStack;
import net.elytrium.limbohub.utils.ProtocolTools;

public class SetContainerSlot implements MinecraftPacket {

  private final int window;
  private final int slot;
  private final ItemStack item;

  public SetContainerSlot(int window, int slot, ItemStack item) {
    this.window = window;
    this.slot = slot;
    this.item = item;
  }

  public SetContainerSlot(Container container, int slot) {
    this(container.getId(), slot, container.getContents()[slot]);
  }

  public SetContainerSlot() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolTools.writeContainerId(buf, protocolVersion, this.window);
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17_1) >= 0) {
      buf.writeByte(0);
    }
    buf.writeShort(this.slot);
    this.item.encode(buf, protocolVersion);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  public int getWindow() {
    return this.window;
  }

  public int getSlot() {
    return this.slot;
  }

  public ItemStack getItem() {
    return this.item;
  }
}
