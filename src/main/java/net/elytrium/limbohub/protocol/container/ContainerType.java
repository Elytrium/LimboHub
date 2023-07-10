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

public enum ContainerType {
  GENERIC_9X1(9, 1),
  GENERIC_9X2(9, 2),
  GENERIC_9X3(9, 3),
  GENERIC_9X4(9, 4),
  GENERIC_9X5(9, 5),
  GENERIC_9X6(9, 6),
  GENERIC_3X3(3, 3);

  private final int columns;
  private final int rows;

  ContainerType(int columns, int rows) {
    this.columns = columns;
    this.rows = rows;
  }

  public int getColumns() {
    return this.columns;
  }

  public int getRows() {
    return this.rows;
  }

  public static ContainerType generic(int columns, int rows) {
    return ContainerType.valueOf("GENERIC_" + columns + "X" + rows);
  }
}
