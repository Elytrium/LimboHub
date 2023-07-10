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

package net.elytrium.limbohub.menu;

import java.util.Map;
import net.elytrium.limbohub.Settings;
import net.elytrium.limbohub.protocol.container.Container;

public class Menu {

  private final Container container;
  private final Map<Integer, Settings.MAIN.ACTION> actions;
  private final Settings.MAIN.ACTION defaultAction;

  public Menu(Container container, Map<Integer, Settings.MAIN.ACTION> actions, Settings.MAIN.ACTION defaultAction) {
    this.container = container;
    this.actions = actions;
    this.defaultAction = defaultAction;
  }

  public Settings.MAIN.ACTION getAction(int slot) {
    return this.actions.getOrDefault(slot, this.defaultAction);
  }

  public Settings.MAIN.ACTION getAction(int column, int row) {
    return this.getAction(row * this.container.getType().getColumns() + column);
  }

  public Container getContainer() {
    return this.container;
  }

  public Map<Integer, Settings.MAIN.ACTION> getActions() {
    return this.actions;
  }

  public Settings.MAIN.ACTION getDefaultAction() {
    return this.defaultAction;
  }
}
