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

package net.elytrium.limbohub.protocol.metadata;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Map;

public class EntityMetadata {

  public static final EntityMetadata EMPTY = new EntityMetadata(Map.of());
  public static final EntityMetadata DUMMY_METADATA = new EntityMetadata(Map.of((byte) 0, new EntityMetadataByteEntry((byte) 0)));

  private final Map<Byte, EntityMetadataEntry> entries;

  public EntityMetadata(Map<Byte, EntityMetadataEntry> entries) {
    this.entries = entries;
  }

  public void encode(ByteBuf buf, ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) <= 0) {
      this.entries.forEach((index, value) -> {
        buf.writeByte((index & 0x1F) | (value.getType(protocolVersion) << 5));
        value.encode(buf, protocolVersion);
      });
      buf.writeByte(0x7F);
    } else {
      this.entries.forEach((index, value) -> {
        buf.writeByte(index);
        ProtocolUtils.writeVarInt(buf, value.getType(protocolVersion));
        value.encode(buf, protocolVersion);
      });
      buf.writeByte(0xFF);
    }
  }
}
