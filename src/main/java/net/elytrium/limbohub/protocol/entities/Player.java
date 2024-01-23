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

package net.elytrium.limbohub.protocol.entities;

import com.velocitypowered.api.network.ProtocolVersion;
import java.util.Map;
import net.elytrium.limbohub.protocol.metadata.EntityMetadata;
import net.elytrium.limbohub.protocol.metadata.EntityMetadataByteEntry;

public class Player {

  public static int getEntityType(ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_3) >= 0) {
      return 124;
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0) {
      return 122;
    } else {
      throw new IllegalArgumentException("Player is not supported on versions below Minecraft 1.20.2");
    }
  }

  public static EntityMetadata buildSkinPartsMetadata(ProtocolVersion version, byte skinParts) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
      return new EntityMetadata(Map.of((byte) 17, new EntityMetadataByteEntry(skinParts)));
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      return new EntityMetadata(Map.of((byte) 16, new EntityMetadataByteEntry(skinParts)));
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
      return new EntityMetadata(Map.of((byte) 15, new EntityMetadataByteEntry(skinParts)));
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_10) >= 0) {
      return new EntityMetadata(Map.of((byte) 13, new EntityMetadataByteEntry(skinParts)));
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
      return new EntityMetadata(Map.of((byte) 12, new EntityMetadataByteEntry(skinParts)));
    } else {
      return new EntityMetadata(Map.of((byte) 10, new EntityMetadataByteEntry(skinParts)));
    }
  }
}
