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

package net.elytrium.limbohub.protocol.item.meta;

import java.util.List;
import net.kyori.adventure.text.Component;

public class StaticItemMeta extends AbstractItemMeta {

  private final Component name;
  private final List<Component> lore;

  public StaticItemMeta(Component name, List<Component> lore, boolean hasColor, int color, boolean enchanted, String skullOwner) {
    super(hasColor, color, enchanted, skullOwner);
    this.name = name;
    this.lore = lore;
  }

  @Override
  public Component getName() {
    return this.name;
  }

  @Override
  public List<Component> getLore() {
    return this.lore;
  }
}
