package com.variantsmp;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class Menu {

    private Menu() {}

    private static final int SLOT_HEAD = 4;

    public static void open(ServerPlayerEntity admin) {
        SimpleInventory inv = new SimpleInventory(54);
        admin.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, ignored) -> new Handler(syncId, playerInv, inv),
                Text.literal("PvP settings")));
    }

    private static final class Handler extends GenericContainerScreenHandler {

        private final SimpleInventory inv;

        Handler(int syncId, PlayerInventory playerInv, SimpleInventory inv) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6);
            this.inv = inv;
            render();
        }

        @Override
        public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
            if (slot < 0 || slot >= 54) return;
            boolean left = button == 0;
            boolean shift = action == SlotActionType.QUICK_MOVE;
            if (action != SlotActionType.PICKUP && !shift) return;
            try {
                click(slot, left, shift);
            } catch (Throwable t) {
                VariantSMP.LOGGER.warn("variantSMP: menu click failed", t);
            }
            render();
            syncState();
        }

        private void click(int slot, boolean left, boolean shift) {
            ModConfig c = VariantSMP.get().config;
            for (Settings.Def s : Settings.defs()) {
                if (s.slot() != slot) continue;
                double cur = s.get().applyAsDouble(c);
                switch (s.kind()) {
                    case BOOL -> s.set().accept(c, cur > 0 ? 0 : 1);
                    case MODE -> {
                        int n = Settings.MACE_MODES.length;
                        int i = (int) cur;
                        s.set().accept(c, ((i + (left ? 1 : -1)) % n + n) % n);
                    }
                    default -> {
                        double delta = s.step() * (shift ? 10 : 1) * (left ? 1 : -1);
                        double v = Math.max(s.min(), Math.min(s.max(), cur + delta));
                        v = s.kind() == Settings.Kind.INT ? Math.rint(v) : Math.round(v * 100.0) / 100.0;
                        s.set().accept(c, v);
                    }
                }
                c.save();
                return;
            }
        }

        private void render() {
            ModConfig c = VariantSMP.get().config;
            for (int i = 0; i < 54; i++) inv.setStack(i, filler());
            inv.setStack(SLOT_HEAD, named(Items.COMPARATOR, "§6Settings",
                    line("§eLeft §7up   §eRight §7down"),
                    line("§eShift §7moves 10 steps"),
                    line("§7Toggles just need a click")));
            for (Settings.Def s : Settings.defs()) {
                boolean on = s.kind() == Settings.Kind.BOOL && s.get().applyAsDouble(c) > 0;
                ItemStack stack = named(s.icon(), "§e" + s.label() + ": " + s.display(c),
                        line("§7" + s.desc()),
                        line("§7Default: " + s.display(c.defaults)),
                        line(switch (s.kind()) {
                            case BOOL -> "§7Click to toggle";
                            case MODE -> "§7Left next, right previous";
                            default -> "§7Step " + Abilities.fmt(s.step());
                        }));
                if (on) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
                inv.setStack(s.slot(), stack);
            }
        }
    }

    private static ItemStack named(Item item, String name, Text... lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, line(name));
        if (lore.length > 0) s.set(DataComponentTypes.LORE, new LoreComponent(List.of(lore)));
        return s;
    }

    private static ItemStack filler() {
        ItemStack s = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        return s;
    }

    private static Text line(String s) {
        return Text.literal(s).styled(st -> st.withItalic(false));
    }
}
