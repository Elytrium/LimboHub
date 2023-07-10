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
import io.netty.buffer.ByteBuf;

public class EntityMetadataBooleanEntry implements EntityMetadataEntry {

  private final boolean value;

  public EntityMetadataBooleanEntry(boolean value) {
    this.value = value;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion protocolVersion) {
    buf.writeBoolean(this.value);
  }

  @Override
  public int getType(ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
      return 8;
    } else if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
      return 7;
    } else {
      return 6;
    }
  }

  public boolean getValue() {
    return this.value;
  }
}
