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
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.kyori.adventure.text.Component;

public class EntityMetadataOptionalComponentEntry implements EntityMetadataEntry {

  private final Component component;

  public EntityMetadataOptionalComponentEntry(Component component) {
    this.component = component;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion protocolVersion) {
    buf.writeBoolean(this.component != null);
    if (this.component != null) {
      new ComponentHolder(protocolVersion, this.component).write(buf);
    }
  }

  @Override
  public int getType(ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
      return 6;
    } else if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
      return 5;
    } else {
      throw new IllegalStateException();
    }
  }

  public Optional<Component> getComponent() {
    return Optional.ofNullable(this.component);
  }
}
