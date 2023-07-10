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

package net.elytrium.limbohub.protocol.container;

import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limbohub.protocol.item.ItemStack;
import net.elytrium.limbohub.protocol.packets.CloseContainer;
import net.elytrium.limbohub.protocol.packets.OpenScreen;
import net.elytrium.limbohub.protocol.packets.SetContainerContent;
import net.kyori.adventure.text.Component;

public class Container {

  private static int ID_COUNTER = 0;

  private final int id;
  private final ContainerType type;
  private final Component title;
  private final ItemStack[] contents;

  public Container(int id, ContainerType type, Component title, ItemStack[] contents) {
    this.id = id;
    this.type = type;
    this.title = title;
    this.contents = contents;
  }

  public void open(LimboPlayer player) {
    player.writePacket(new OpenScreen(this));
    player.writePacket(new SetContainerContent(this));
    player.flushPackets();
  }

  public void close(LimboPlayer player) {
    player.writePacketAndFlush(new CloseContainer(this));
  }

  public void setItem(int index, ItemStack item) {
    this.contents[index] = item;
  }

  public void setItem(int column, int row, ItemStack item) {
    this.contents[row * this.type.getColumns() + column] = item;
  }

  public void setItems(ItemStack[] items) {
    System.arraycopy(items, 0, this.contents, 0, Math.min(items.length, this.contents.length));
  }

  public int getId() {
    return this.id;
  }

  public ContainerType getType() {
    return this.type;
  }

  public Component getTitle() {
    return this.title;
  }

  public ItemStack[] getContents() {
    return this.contents;
  }

  public static Container genericContainer(int columns, int rows, Component title) {
    return new Container(++ID_COUNTER, ContainerType.generic(columns, rows), title, new ItemStack[rows * columns]);
  }
}
