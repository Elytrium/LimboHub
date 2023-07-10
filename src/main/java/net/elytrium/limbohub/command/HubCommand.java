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

package net.elytrium.limbohub.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limbohub.LimboHub;
import net.elytrium.limbohub.Settings;

public class HubCommand implements SimpleCommand {

  private final LimboHub plugin;

  public HubCommand(LimboHub plugin) {
    this.plugin = plugin;
  }

  @Override
  public void execute(Invocation invocation) {
    if (invocation.source() instanceof Player) {
      this.plugin.sendToHub((Player) invocation.source());
    }
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    return !Settings.IMP.MAIN.HUB_COMMAND.REQUIRE_PERMISSION
        || invocation.source().getPermissionValue("limbohub.command.hub") == Tristate.TRUE;
  }
}
