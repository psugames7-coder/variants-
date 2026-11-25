package com.macelimit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class MaceRegistry {

    private MaceRegistry() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String KEY = "macelimit_id";

    public static final class Entry {
        public boolean found;
        public boolean op;
        public boolean pendingDelete;
        public String holder = "";
        public String holderName = "";
        public String where = "unknown";
        public String dimension = "";
        public int x;
        public int y;
        public int z;
        public long seen;
    }

    public static final class Data {
        public int limit = 3;
        public Map<String, Entry> maces = new LinkedHashMap<>();
    }

    private static Path path;
    static Data data = new Data();
    private static boolean dirty;

    static void load(Path file) {
        path = file;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                Data loaded = GSON.fromJson(reader, Data.class);
                if (loaded != null) data = loaded;
            } catch (Exception e) {
                MaceLimit.LOGGER.error("macelimit: could not read {}", file, e);
            }
        }
        if (data.maces == null) data.maces = new LinkedHashMap<>();
        if (data.limit < 0) data.limit = 3;
        save();
    }

    static void save() {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
            dirty = false;
        } catch (Exception e) {
            MaceLimit.LOGGER.error("macelimit: could not save {}", path, e);
        }
    }

    static void saveIfDirty() {
        if (dirty) save();
    }

    public static String id(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isOf(Items.MACE)) return null;
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) return null;
        String id = custom.copyNbt().getString(KEY, "");
        return id.isEmpty() ? null : id;
    }

    public static boolean counts(Entry e) {
        return !e.found && !e.op && !e.pendingDelete;
    }

    public static int counted() {
        int n = 0;
        for (Entry e : data.maces.values()) {
            if (counts(e)) n++;
        }
        return n;
    }

    public static boolean full() {
        return counted() >= data.limit;
    }

    private static String tag(ItemStack stack, boolean found) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putString(KEY, id));
        Entry e = new Entry();
        e.found = found;
        e.seen = System.currentTimeMillis();
        data.maces.put(id, e);
        dirty = true;
        return id;
    }

    public static void register(ItemStack stack, ServerPlayerEntity crafter) {
        if (stack == null || !stack.isOf(Items.MACE) || id(stack) != null) return;
        String id = tag(stack, false);
        if (crafter != null) {
            Entry e = data.maces.get(id);
            atPlayer(e, crafter, "inventory");
            e.op = isOp(crafter);
        }
        save();
    }

    public static void destroyed(ItemStack stack) {
        String id = id(stack);
        if (id != null) destroyed(id);
    }

    public static void destroyed(String id) {
        if (data.maces.remove(id) != null) save();
    }

    static boolean isOp(ServerPlayerEntity p) {
        try {
            MinecraftServer s = p.getEntityWorld().getServer();
            if (s == null) return false;
            return s.getPlayerManager().isOperator(new PlayerConfigEntry(p.getGameProfile()));
        } catch (Throwable t) {
            return false;
        }
    }

    private static void at(Entry e, String where, ServerWorld world, BlockPos pos) {
        e.where = where;
        e.dimension = world.getRegistryKey().getValue().toString();
        e.x = pos.getX();
        e.y = pos.getY();
        e.z = pos.getZ();
        e.seen = System.currentTimeMillis();
        dirty = true;
    }

    private static void atPlayer(Entry e, ServerPlayerEntity p, String part) {
        e.holder = p.getUuid().toString();
        e.holderName = p.getGameProfile().name();
        at(e, "player " + e.holderName + " (" + part + ")", (ServerWorld) p.getEntityWorld(), p.getBlockPos());
    }

    private static String see(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isOf(Items.MACE)) return null;
        String id = id(stack);
        if (id == null) return tag(stack, true);
        if (!data.maces.containsKey(id)) {
            Entry e = new Entry();
            e.found = true;
            data.maces.put(id, e);
            dirty = true;
        }
        return id;
    }

    private static boolean pending(String id) {
        Entry e = data.maces.get(id);
        return e != null && e.pendingDelete;
    }

    private static final int MAX_DEPTH = 3;

    private static boolean handleList(List<ItemStack> list, Consumer<String> locate, String removeId, int depth) {
        boolean changed = false;
        for (int i = 0; i < list.size(); i++) {
            ItemStack in = list.get(i);
            if (in.isEmpty()) continue;
            if (in.isOf(Items.MACE)) {
                String id = id(in);
                if (removeId != null) {
                    if (removeId.equals(id)) {
                        list.set(i, ItemStack.EMPTY);
                        changed = true;
                    }
                    continue;
                }
                if (id == null) {
                    id = tag(in, true);
                    changed = true;
                } else {
                    see(in);
                }
                if (pending(id)) {
                    list.set(i, ItemStack.EMPTY);
                    data.maces.remove(id);
                    dirty = true;
                    changed = true;
                    continue;
                }
                locate.accept(id);
            } else if (nested(in, locate, removeId, depth + 1)) {
                changed = true;
            }
        }
        return changed;
    }

    private static boolean nested(ItemStack stack, Consumer<String> locate, String removeId, int depth) {
        if (stack == null || stack.isEmpty() || depth > MAX_DEPTH) return false;
        boolean changed = false;
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            DefaultedList<ItemStack> list = DefaultedList.ofSize(27, ItemStack.EMPTY);
            container.copyTo(list);
            if (handleList(list, locate, removeId, depth)) {
                stack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(list));
                changed = true;
            }
        }
        BundleContentsComponent bundle = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundle != null) {
            List<ItemStack> list = new ArrayList<>();
            for (ItemStack in : bundle.iterateCopy()) list.add(in);
            if (handleList(list, locate, removeId, depth)) {
                list.removeIf(ItemStack::isEmpty);
                stack.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(list));
                changed = true;
            }
        }
        return changed;
    }

    static void seeContainer(Inventory inv, ServerWorld world, BlockPos pos) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (nested(stack, id -> at(data.maces.get(id), "container (inside a shulker box or bundle)", world, pos), null, 1)) {
                inv.markDirty();
            }
            String id = see(stack);
            if (id == null) continue;
            if (pending(id)) {
                inv.setStack(i, ItemStack.EMPTY);
                inv.markDirty();
                data.maces.remove(id);
                dirty = true;
                continue;
            }
            at(data.maces.get(id), "container", world, pos);
        }
        saveIfDirty();
    }

    static void seeEntity(Entity entity) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) return;
        if (entity instanceof ItemEntity item) {
            nested(item.getStack(), id -> at(data.maces.get(id), "dropped on the ground (inside a shulker box or bundle)", world, item.getBlockPos()), null, 1);
            String id = see(item.getStack());
            if (id == null) return;
            if (pending(id)) {
                data.maces.remove(id);
                item.discard();
                dirty = true;
            } else {
                at(data.maces.get(id), "dropped on the ground", world, item.getBlockPos());
            }
        } else if (entity instanceof ItemFrameEntity frame) {
            nested(frame.getHeldItemStack(), id -> at(data.maces.get(id), "item frame (inside a shulker box or bundle)", world, frame.getBlockPos()), null, 1);
            String id = see(frame.getHeldItemStack());
            if (id == null) return;
            if (pending(id)) {
                data.maces.remove(id);
                frame.setHeldItemStack(ItemStack.EMPTY);
                dirty = true;
            } else {
                at(data.maces.get(id), "item frame", world, frame.getBlockPos());
            }
        }
        saveIfDirty();
    }

    private static void seePlayerInventory(Inventory inv, ServerPlayerEntity p, String part, Map<String, ServerPlayerEntity> found) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            nested(stack, id -> {
                found.put(id, p);
                atPlayer(data.maces.get(id), p, part + ", inside a shulker box or bundle");
            }, null, 1);
            String id = see(stack);
            if (id == null) continue;
            if (pending(id)) {
                inv.setStack(i, ItemStack.EMPTY);
                data.maces.remove(id);
                dirty = true;
                continue;
            }
            found.put(id, p);
            atPlayer(data.maces.get(id), p, part);
        }
    }

    private static void seeOpenScreen(ServerPlayerEntity p, Map<String, ServerPlayerEntity> found) {
        ScreenHandler handler = p.currentScreenHandler;
        if (handler == null) return;
        String cursor = see(handler.getCursorStack());
        if (cursor != null) {
            if (pending(cursor)) {
                handler.setCursorStack(ItemStack.EMPTY);
                data.maces.remove(cursor);
                dirty = true;
            } else {
                found.put(cursor, p);
                atPlayer(data.maces.get(cursor), p, "cursor");
            }
        }
        if (handler == p.playerScreenHandler) return;
        for (Slot slot : handler.slots) {
            if (slot.inventory == p.getInventory()) continue;
            nested(slot.getStack(), id -> at(data.maces.get(id), "container opened by " + p.getGameProfile().name()
                    + " (inside a shulker box or bundle)", (ServerWorld) p.getEntityWorld(), p.getBlockPos()), null, 1);
            String id = see(slot.getStack());
            if (id == null) continue;
            if (pending(id)) {
                slot.setStack(ItemStack.EMPTY);
                data.maces.remove(id);
                dirty = true;
                continue;
            }
            Entry e = data.maces.get(id);
            if (slot.inventory instanceof BlockEntity be && be.getWorld() instanceof ServerWorld world) {
                at(e, "container", world, be.getPos());
            } else {
                at(e, "container opened by " + p.getGameProfile().name(), (ServerWorld) p.getEntityWorld(), p.getBlockPos());
            }
        }
    }

    static void scan(MinecraftServer server) {
        Map<String, ServerPlayerEntity> found = new HashMap<>();
        Map<String, ServerPlayerEntity> online = new HashMap<>();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            online.put(p.getUuid().toString(), p);
            seePlayerInventory(p.getInventory(), p, "inventory", found);
            seePlayerInventory(p.getEnderChestInventory(), p, "ender chest", found);
            seeOpenScreen(p, found);
        }
        for (Map.Entry<String, Entry> me : data.maces.entrySet()) {
            Entry e = me.getValue();
            ServerPlayerEntity holder = found.get(me.getKey());
            if (holder != null) {
                boolean op = isOp(holder);
                if (e.op != op) {
                    e.op = op;
                    dirty = true;
                }
            } else if (e.op && online.containsKey(e.holder)) {
                e.op = false;
                dirty = true;
            }
        }
        saveIfDirty();
    }

    static String delete(MinecraftServer server, String id) {
        Entry e = data.maces.get(id);
        if (e == null) return null;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (removeFrom(p.getInventory(), id) || removeFrom(p.getEnderChestInventory(), id)) {
                destroyed(id);
                return "removed from " + p.getGameProfile().name();
            }
            ScreenHandler handler = p.currentScreenHandler;
            if (handler != null && id.equals(id(handler.getCursorStack()))) {
                handler.setCursorStack(ItemStack.EMPTY);
                destroyed(id);
                return "removed from " + p.getGameProfile().name() + "'s cursor";
            }
            if (handler != null) {
                for (Slot slot : handler.slots) {
                    if (id.equals(id(slot.getStack()))) {
                        slot.setStack(ItemStack.EMPTY);
                        destroyed(id);
                        return "removed from the container " + p.getGameProfile().name() + " has open";
                    }
                    if (removeNested(slot.getStack(), id)) {
                        slot.markDirty();
                        destroyed(id);
                        return "removed from a shulker box or bundle in the container " + p.getGameProfile().name() + " has open";
                    }
                }
            }
        }
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof ItemEntity item && id.equals(id(item.getStack()))) {
                    item.discard();
                    destroyed(id);
                    return "removed dropped mace";
                }
                if (entity instanceof ItemEntity item && removeNested(item.getStack(), id)) {
                    destroyed(id);
                    return "removed from a dropped shulker box or bundle";
                }
                if (entity instanceof ItemFrameEntity frame && id.equals(id(frame.getHeldItemStack()))) {
                    frame.setHeldItemStack(ItemStack.EMPTY);
                    destroyed(id);
                    return "removed from item frame";
                }
            }
            if (world.getRegistryKey().getValue().toString().equals(e.dimension)) {
                BlockPos pos = new BlockPos(e.x, e.y, e.z);
                if (world.isChunkLoaded(pos) && world.getBlockEntity(pos) instanceof Inventory inv && removeFrom(inv, id)) {
                    inv.markDirty();
                    destroyed(id);
                    return "removed from container at " + e.x + " " + e.y + " " + e.z;
                }
            }
        }
        e.pendingDelete = true;
        save();
        return "not loaded right now, it will be deleted the next time it's seen";
    }

    private static boolean removeNested(ItemStack stack, String id) {
        return nested(stack, ignored -> { }, id, 1);
    }

    private static boolean removeFrom(Inventory inv, String id) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (id.equals(id(stack))) {
                inv.setStack(i, ItemStack.EMPTY);
                return true;
            }
            if (removeNested(stack, id)) {
                inv.markDirty();
                return true;
            }
        }
        return false;
    }
}
