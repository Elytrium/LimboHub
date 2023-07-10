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

package net.elytrium.limbohub.data;

import java.util.List;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limbohub.protocol.packets.DisplayObjective;
import net.elytrium.limbohub.protocol.packets.UpdateObjectives;
import net.elytrium.limbohub.protocol.packets.UpdateScore;
import net.kyori.adventure.text.Component;

public class Sidebar {

  private final Component title;
  private final List<Component> lines;

  public Sidebar(Component title, List<Component> lines) {
    this.title = title;
    this.lines = lines;
  }

  public void show(LimboPlayer player) {
    player.writePacketAndFlush(new UpdateObjectives("sidebar_obj", (byte) 0, this.title, 0));
    player.writePacketAndFlush(new DisplayObjective((byte) 1, "sidebar_obj"));
    int score = this.lines.size();
    for (Component line : this.lines) {
      player.writePacketAndFlush(new UpdateScore(line, 0, "sidebar_obj", --score));
    }
  }

  public Component getTitle() {
    return this.title;
  }

  public List<Component> getLines() {
    return this.lines;
  }
}
