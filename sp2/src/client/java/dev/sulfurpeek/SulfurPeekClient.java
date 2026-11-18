package dev.sulfurpeek;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SulfurPeek: adds a "Contains: <block>" line to the tooltip of any item that carries a
 * Sulfur Cube (Bucket of Sulfur Cube). Works anywhere a tooltip shows, including the
 * DonutSMP auction house, because it only reads item data the server already sends you.
 *
 * Lookup order:
 *   1. the minecraft:sulfur_cube_content data component (if the item has it)
 *   2. fallback: scan the bucket's entity data (NBT text) for the held item id
 */
public class SulfurPeekClient implements ClientModInitializer {

    private static final Pattern ID = Pattern.compile("id\\s*:\\s*\"?(minecraft:[a-z0-9_/.-]+)\"?");

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            ItemStack held = findHeldBlock(stack);
            if (held != null && !held.isEmpty()) {
                // Use the item's real name from its type, never the stack's custom/renamed name.
                Item real = held.getItem();
                Identifier realId = BuiltInRegistries.ITEM.getKey(real);
                lines.add(Component.literal("Contains: ")
                        .withStyle(s -> s.withColor(0xAAAAAA))
                        .append(Component.translatable(real.getDescriptionId())
                                .withStyle(s -> s.withColor(0xE6D84A)))
                        .append(Component.literal(" (" + realId + ")")
                                .withStyle(s -> s.withColor(0x777777))));
            }
        });
    }

    private static ItemStack findHeldBlock(ItemStack stack) {
        // 1) Direct component lookup, by id so we don't depend on a compile-time constant name.
        for (var entry : stack.getComponents()) {
            DataComponentType<?> type = entry.type();
            Identifier key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (key != null && key.getPath().equals("sulfur_cube_content")) {
                Object v = entry.value();
                if (v instanceof ItemStack s) return s;
                if (v instanceof Item i) return new ItemStack(i);
                ItemStack parsed = fromText(String.valueOf(v));
                if (parsed != null) return parsed;
            }
        }

        // 2) Fallback: bucket entity data holds the mob's NBT, including what it swallowed.
        var data = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        if (data != null) {
            String text = data.copyTag().toString().replace("minecraft:sulfur_cube", "");
            return fromText(text);
        }
        return null;
    }

    private static ItemStack fromText(String text) {
        Matcher m = ID.matcher(text);
        while (m.find()) {
            Identifier id = Identifier.tryParse(m.group(1));
            if (id == null) continue;
            var item = BuiltInRegistries.ITEM.getOptional(id);
            if (item.isPresent() && !item.get().equals(net.minecraft.world.item.Items.AIR)) {
                return new ItemStack(item.get());
            }
        }
        return null;
    }
}
