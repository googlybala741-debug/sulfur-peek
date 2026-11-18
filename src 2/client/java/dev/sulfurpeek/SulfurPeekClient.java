package dev.sulfurpeek;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SulfurPeek: shows the REAL block inside a Bucket of Sulfur Cube.
 *
 * It reads only the item's minecraft:sulfur_cube_content data component, which is the actual
 * game data. Seller-written lore (like a "Contains: ..." line) and custom names are ignored,
 * so they can't fake it. Hold F3+H (advanced tooltips) to also see the block's item id.
 */
public class SulfurPeekClient implements ClientModInitializer {

    // Matches e.g.  ResourceKey[minecraft:item / minecraft:observer]
    private static final Pattern ITEM_KEY =
            Pattern.compile("ResourceKey\\[[^/\\]]*/\\s*([a-z0-9_.:/-]+)\\]");

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            Item held = findHeldBlock(stack);
            if (held == null) return;

            Component line = Component.literal("Real block: ").withStyle(s -> s.withColor(0xAAAAAA))
                    .append(Component.translatable(held.getDescriptionId())
                            .withStyle(s -> s.withColor(0xE6D84A)));
            if (flag.isAdvanced()) {
                line = line.copy().append(Component.literal(" (" + BuiltInRegistries.ITEM.getKey(held) + ")")
                        .withStyle(s -> s.withColor(0x777777)));
            }
            lines.add(line);
        });
    }

    public static Item findHeldBlock(ItemStack stack) {
        for (var entry : stack.getComponents()) {
            DataComponentType<?> type = entry.type();
            Identifier key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (key == null || !key.getPath().equals("sulfur_cube_content")) continue;

            Matcher m = ITEM_KEY.matcher(String.valueOf(entry.value()));
            while (m.find()) {
                Identifier id = Identifier.tryParse(m.group(1));
                if (id == null) continue;
                var item = BuiltInRegistries.ITEM.getOptional(id);
                if (item.isPresent()) return item.get();
            }
        }
        return null;
    }
}
