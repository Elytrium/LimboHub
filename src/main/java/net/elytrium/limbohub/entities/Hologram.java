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

package net.elytrium.limbohub.entities;

import java.util.List;
import java.util.UUID;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limbohub.LimboHub;
import net.elytrium.limbohub.protocol.entities.ArmorStand;
import net.elytrium.limbohub.protocol.packets.SetEntityMetadata;
import net.elytrium.limbohub.protocol.packets.SpawnEntity;
import net.kyori.adventure.text.Component;

public class Hologram {

  private static final double LINE_HEIGHT = 0.27;

  public final int entityId;
  public final double positionX;
  public final double positionY;
  public final double positionZ;
  public final List<Component> lines;

  public Hologram(int entityId, double positionX, double positionY, double positionZ, List<Component> lines) {
    this.entityId = entityId;
    this.positionX = positionX;
    this.positionY = positionY;
    this.positionZ = positionZ;
    this.lines = lines;
  }

  public Hologram(double positionX, double positionY, double positionZ, List<Component> lines) {
    this(LimboHub.reserveEntityIds(lines.size()), positionX, positionY, positionZ, lines);
  }

  public void spawn(LimboPlayer player) {
    double hologramY = this.positionY + LINE_HEIGHT * this.lines.size() - 1.975;
    int currentEntityId = this.entityId;
    for (Component line : this.lines) {
      hologramY -= LINE_HEIGHT;

      player.writePacketAndFlush(
              new SpawnEntity(currentEntityId, UUID.randomUUID(), ArmorStand::getEntityType, this.positionX, hologramY, this.positionZ, 0, 0, 0, 0));
      player.writePacketAndFlush(new SetEntityMetadata(currentEntityId, version -> ArmorStand.buildHologramMetadata(version, line)));

      ++currentEntityId;
    }
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

  public List<Component> getLines() {
    return this.lines;
  }
}
