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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.GameProfile.Property;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import it.unimi.dsi.fastutil.Pair;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.elytrium.limboapi.api.protocol.item.ItemComponentMap;
import net.elytrium.limbohub.LimboHub;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public abstract class AbstractItemMeta implements ItemMeta {

  private final boolean hasColor;
  private final int color;
  private final boolean enchanted;

  private final String skinName = "npc" + ThreadLocalRandom.current().nextInt();
  private final String skinValue;


  public AbstractItemMeta(boolean hasColor, int color, boolean enchanted, String skullOwner) {
    this.color = color;

    if (skullOwner == null || skullOwner.isBlank()) {
      this.enchanted = enchanted;
      this.hasColor = hasColor;
      this.skinValue = null;
      return;
    } else {
      this.enchanted = false;
      this.hasColor = false;
    }

    if (skullOwner.contains(";")) {
      String[] data = skullOwner.split(";", 2);
      this.skinValue = data[1];
    } else {
      this.skinValue = skullOwner;
    }
  }

  @Override
  public CompoundBinaryTag buildNbt(ProtocolVersion protocolVersion) {
    CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();

    GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(protocolVersion);

    Component name = this.getName();
    List<Component> lore = this.getLore();

    CompoundBinaryTag.Builder displayBuilder = CompoundBinaryTag.builder();

    if (name != null) {
      ComponentSerializer<Component, ?, String> nameSerializer =
          protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0
              ? serializer : LegacyComponentSerializer.legacySection();

      displayBuilder.putString("Name", nameSerializer.serialize(name));
    }

    if (lore != null && !lore.isEmpty()) {
      ComponentSerializer<Component, ?, String> loreSerializer =
          protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0
              ? serializer : LegacyComponentSerializer.legacySection();

      ListBinaryTag.Builder<StringBinaryTag> loreBuilder = ListBinaryTag.builder(BinaryTagTypes.STRING);
      this.getLore().forEach(component -> loreBuilder.add(StringBinaryTag.of(loreSerializer.serialize(component))));
      displayBuilder.put("Lore", loreBuilder.build());
    }

    if (this.hasColor) {
      displayBuilder.putInt("color", this.color);
    }

    builder.put("display", displayBuilder.build());

    builder.putInt("HideFlags", 255);

    if (this.enchanted) {
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
        builder.put("Enchantments",
            ListBinaryTag.builder(BinaryTagTypes.COMPOUND)
                .add(CompoundBinaryTag.builder()
                    .putString("id", "minecraft:sharpness")
                    .putShort("lvl", (short) 1)
                    .build())
                .build()
        );
      } else {
        builder.put("ench",
            ListBinaryTag.builder(BinaryTagTypes.COMPOUND)
                .add(CompoundBinaryTag.builder()
                    .putInt("id", 0)
                    .putShort("lvl", (short) 1)
                    .build())
                .build()
        );
      }
    }

    if (this.skinValue != null) {
      builder.put("SkullOwner",
          CompoundBinaryTag.builder()
              .putString("Name", this.skinName)
              .put("Properties",
                  CompoundBinaryTag.builder()
                      .put("textures",
                          ListBinaryTag.builder(BinaryTagTypes.COMPOUND)
                              .add(CompoundBinaryTag.builder()
                                  .putString("Value", this.skinValue)
                                  .build())
                              .build())
                      .build())
              .build()
      );
    }

    return builder.build();
  }

  @Override
  public ItemComponentMap buildComponents(ProtocolVersion protocolVersion) {
    ItemComponentMap componentMap = LimboHub.getInstance().getLimboFactory().createItemComponentMap();

    Component name = this.getName();
    List<Component> lore = this.getLore();

    if (name != null) {
      componentMap.add(protocolVersion, "minecraft:item_name", name);
    }

    if (lore != null && !lore.isEmpty()) {
      componentMap.add(protocolVersion, "minecraft:lore", lore);
    }

    if (this.hasColor) {
      componentMap.add(protocolVersion, "minecraft:base_color", this.color);
      componentMap.add(protocolVersion, "minecraft:dyed_color", Pair.of(this.color, true));
    }

    componentMap.add(protocolVersion, "minecraft:hide_additional_tooltip", true);

    if (this.enchanted) {
      componentMap.add(protocolVersion, "minecraft:enchantment_glint_override", true);
    }

    if (this.skinValue != null) {
      componentMap.add(protocolVersion, "minecraft:profile", new GameProfile(
          new UUID(0, 0), this.skinName, List.of(new Property("textures", this.skinValue, ""))));
    }

    return componentMap;
  }

  public abstract Component getName();

  public abstract List<Component> getLore();

  public int getColor() {
    return this.color;
  }

  public boolean isEnchanted() {
    return this.enchanted;
  }

  public boolean isHasColor() {
    return this.hasColor;
  }

  public String getSkinName() {
    return this.skinName;
  }

  public String getSkinValue() {
    return this.skinValue;
  }
}
