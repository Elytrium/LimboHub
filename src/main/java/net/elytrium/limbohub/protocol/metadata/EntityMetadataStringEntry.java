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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class EntityMetadataStringEntry implements EntityMetadataEntry {

  private final String value;

  public EntityMetadataStringEntry(String value) {
    this.value = value;
  }

  public EntityMetadataStringEntry(Component component) {
    this(LegacyComponentSerializer.legacySection().serialize(component));
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, this.value);
  }

  @Override
  public int getType(ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
      return 3;
    } else {
      return 4;
    }
  }

  public String getValue() {
    return this.value;
  }
}
