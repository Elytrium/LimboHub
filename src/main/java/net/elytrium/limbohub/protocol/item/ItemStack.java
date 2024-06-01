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

package net.elytrium.limbohub.protocol.item;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limbohub.protocol.item.meta.ItemMeta;

public class ItemStack {

  public static final ItemStack EMPTY = new ItemStack();

  private final boolean present;
  private final List<VirtualItem> items;
  private final int count;
  private final int data;
  private final ItemMeta meta;

  public ItemStack(boolean present, List<VirtualItem> items, int count, int data, ItemMeta meta) {
    this.present = present;
    this.items = items;
    this.count = count;
    this.data = data;
    this.meta = meta;
  }

  public ItemStack(List<VirtualItem> item, int count, int data, ItemMeta meta) {
    this(true, item, count, data, meta);
  }

  public ItemStack(VirtualItem item, List<VirtualItem> fallback, int count, int data, ItemMeta meta) {
    this(Stream.concat(Stream.of(item), fallback.stream()).collect(Collectors.toUnmodifiableList()), count, data, meta);
  }

  public ItemStack() {
    this(false, null, 0, 0, null);
  }

  public void encode(ByteBuf buf, ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_20_5) >= 0) {
      this.encodeModern(buf, protocolVersion);
    } else {
      this.encodeLegacy(buf, protocolVersion);
    }
  }

  private void encodeModern(ByteBuf buf, ProtocolVersion protocolVersion) {
    if (!this.present) {
      ProtocolUtils.writeVarInt(buf, 0);
      return;
    }

    VirtualItem item = this.items.stream()
        .dropWhile(i -> !i.isSupportedOn(protocolVersion))
        .findFirst().orElseThrow(() ->
            new IllegalArgumentException("Item " + this.items.get(0).getModernID() + " is not supported on " + protocolVersion));

    int id = item.getID(protocolVersion);
    if (id == 0) {
      ProtocolUtils.writeVarInt(buf, 0);
    } else {
      ProtocolUtils.writeVarInt(buf, this.count);
      ProtocolUtils.writeVarInt(buf, id);

      if (this.meta != null) {
        this.meta.buildComponents(protocolVersion).write(protocolVersion, buf);
      } else {
        ProtocolUtils.writeVarInt(buf, 0);
        ProtocolUtils.writeVarInt(buf, 0);
      }
    }
  }

  private void encodeLegacy(ByteBuf buf, ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13_2) >= 0) {
      buf.writeBoolean(this.present);
    }

    if (!this.present && protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13_2) < 0) {
      buf.writeShort(-1);
    }

    if (this.present) {
      VirtualItem item = this.items.stream()
          .dropWhile(i -> !i.isSupportedOn(protocolVersion))
          .findFirst().orElseThrow(() ->
              new IllegalArgumentException("Item " + this.items.get(0).getModernID() + " is not supported on " + protocolVersion));

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13_2) < 0) {
        buf.writeShort(item.getID(protocolVersion));
      } else {
        ProtocolUtils.writeVarInt(buf, item.getID(protocolVersion));
      }
      buf.writeByte(this.count);
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
        buf.writeShort(this.data);
      }

      if (this.meta == null) {
        buf.writeByte(0);
      } else {
        ProtocolUtils.writeBinaryTag(buf, protocolVersion, this.meta.buildNbt(protocolVersion));
      }
    }
  }

  public boolean isPresent() {
    return this.present;
  }

  public List<VirtualItem> getItems() {
    return this.items;
  }

  public int getCount() {
    return this.count;
  }

  public int getData() {
    return this.data;
  }

  public ItemMeta getMeta() {
    return this.meta;
  }
}
