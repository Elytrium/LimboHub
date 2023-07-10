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
import java.util.UUID;
import net.elytrium.limbohub.protocol.metadata.EntityMetadata;

public class SpawnPlayer implements MinecraftPacket {

  private final int entityId;
  private final UUID entityUuid;
  private final double positionX;
  private final double positionY;
  private final double positionZ;
  private final float yaw;
  private final float pitch;

  public SpawnPlayer(int entityId, UUID entityUuid, double positionX,
                     double positionY, double positionZ, float yaw, float pitch) {
    this.entityId = entityId;
    this.entityUuid = entityUuid;
    this.positionX = positionX;
    this.positionY = positionY;
    this.positionZ = positionZ;
    this.yaw = yaw;
    this.pitch = pitch;
  }

  public SpawnPlayer() {
    throw new IllegalStateException();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, this.entityId);
    ProtocolUtils.writeUuid(buf, this.entityUuid);
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
      buf.writeDouble(this.positionX);
      buf.writeDouble(this.positionY);
      buf.writeDouble(this.positionZ);
    } else {
      buf.writeInt((int) (this.positionX * 32.0D));
      buf.writeInt((int) (this.positionY * 32.0D));
      buf.writeInt((int) (this.positionZ * 32.0D));
    }
    buf.writeByte((int) (this.yaw * (256.0F / 360.0F)));
    buf.writeByte((int) (this.pitch * (256.0F / 360.0F)));
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) <= 0) {
      buf.writeShort(0);
    }
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_14_4) <= 0) {
      EntityMetadata.DUMMY_METADATA.encode(buf, protocolVersion);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return true;
  }

  public int getEntityId() {
    return this.entityId;
  }

  public UUID getEntityUuid() {
    return this.entityUuid;
  }

  public double getPositionX() {
    return this.positionX;
  }

  public double getPositionY() {
    return this.positionY;
  }

  public double getPositionZ() {
    return this.positionZ;
  }

  public float getYaw() {
    return this.yaw;
  }

  public float getPitch() {
    return this.pitch;
  }
}
