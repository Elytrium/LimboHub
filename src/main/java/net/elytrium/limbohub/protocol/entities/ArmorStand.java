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
import net.elytrium.limbohub.protocol.metadata.EntityMetadataBooleanEntry;
import net.elytrium.limbohub.protocol.metadata.EntityMetadataByteEntry;
import net.elytrium.limbohub.protocol.metadata.EntityMetadataOptionalComponentEntry;
import net.elytrium.limbohub.protocol.metadata.EntityMetadataStringEntry;
import net.kyori.adventure.text.Component;

public class ArmorStand {

  public static int getEntityType(ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_21_2) >= 0) {
      return 5;
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_5) >= 0) {
      return 3;
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      return 2;
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
      return 1;
    } else {
      return 78;
    }
  }

  public static EntityMetadata buildHologramMetadata(ProtocolVersion version, Component displayName) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
      return new EntityMetadata(Map.of(
          (byte) 0, new EntityMetadataByteEntry((byte) 0x20),
          (byte) 2, new EntityMetadataOptionalComponentEntry(displayName),
          (byte) 3, new EntityMetadataBooleanEntry(true)
      ));
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
      return new EntityMetadata(Map.of(
          (byte) 0, new EntityMetadataByteEntry((byte) 0x20),
          (byte) 2, new EntityMetadataStringEntry(displayName),
          (byte) 3, new EntityMetadataBooleanEntry(true)
      ));
    } else {
      return new EntityMetadata(Map.of(
          (byte) 0, new EntityMetadataByteEntry((byte) 0x20),
          (byte) 2, new EntityMetadataStringEntry(displayName),
          (byte) 3, new EntityMetadataByteEntry((byte) 1)
      ));
    }
  }
}
