package com.macelimit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public final class WorldScan {

    private WorldScan() {}

    private static final String MACE = "minecraft:mace";
    private static final int CHAT_LINES = 40;
    private static volatile boolean running;

    private record Hit(String where, String id) {}

    public static boolean running() {
        return running;
    }

    public static void start(MinecraftServer server, ServerCommandSource source) {
        running = true;
        try {
            server.saveAll(true, true, true);
        } catch (Throwable t) {
            MaceLimit.LOGGER.warn("macelimit: save before scan failed", t);
        }
        Path root = server.getSavePath(WorldSavePath.ROOT);
        Thread thread = new Thread(() -> {
            List<Hit> hits = new ArrayList<>();
            int[] skipped = {0};
            try {
                scanWorld(root, hits, skipped);
                scanPlayers(root, hits);
            } catch (Throwable t) {
                MaceLimit.LOGGER.warn("macelimit: world scan failed", t);
            }
            writeReport(hits, skipped[0]);
            server.execute(() -> {
                running = false;
                report(source, hits, skipped[0]);
            });
        }, "macelimit-scan");
        thread.setDaemon(true);
        thread.start();
    }

    private static void report(ServerCommandSource source, List<Hit> hits, int skipped) {
        source.sendFeedback(() -> Text.literal("§eWorld scan done: §f" + hits.size() + " §emaces found in the save files"
                + (skipped > 0 ? " §7(" + skipped + " chunks couldn't be read)" : "")), false);
        int shown = 0;
        for (Hit h : hits) {
            if (shown++ >= CHAT_LINES) {
                source.sendFeedback(() -> Text.literal("§7...full list in config/macelimit_scan.txt"), false);
                break;
            }
            String line = "§7" + h.where() + (h.id() != null ? " §fid " + h.id() : " §8(no id yet)");
            source.sendFeedback(() -> Text.literal(line), false);
        }
    }

    private static void writeReport(List<Hit> hits, int skipped) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Maces found: " + hits.size() + (skipped > 0 ? " (" + skipped + " chunks couldn't be read)" : ""));
            for (Hit h : hits) lines.add(h.where() + (h.id() != null ? " | id " + h.id() : " | no id yet"));
            Files.write(FabricLoader.getInstance().getConfigDir().resolve("macelimit_scan.txt"), lines);
        } catch (IOException e) {
            MaceLimit.LOGGER.warn("macelimit: could not write scan report", e);
        }
    }

    private static void scanWorld(Path root, List<Hit> hits, int[] skipped) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(p -> p.getFileName().toString().endsWith(".mca"))
                    .filter(p -> {
                        String dir = p.getParent().getFileName().toString();
                        return dir.equals("region") || dir.equals("entities");
                    })
                    .toList();
        }
        for (Path file : files) {
            String dim = dimension(root, file.getParent().getParent());
            boolean entities = file.getParent().getFileName().toString().equals("entities");
            try {
                scanRegion(file, dim, entities, hits, skipped);
            } catch (Throwable t) {
                skipped[0]++;
            }
        }
    }

    private static String dimension(Path root, Path dimDir) {
        String rel = root.relativize(dimDir).toString().replace('\\', '/');
        if (rel.isEmpty()) return "minecraft:overworld";
        if (rel.equals("DIM-1")) return "minecraft:the_nether";
        if (rel.equals("DIM1")) return "minecraft:the_end";
        if (rel.startsWith("dimensions/")) {
            String[] parts = rel.substring("dimensions/".length()).split("/", 2);
            if (parts.length == 2) return parts[0] + ":" + parts[1];
        }
        return rel;
    }

    private static void scanRegion(Path file, String dim, boolean entities, List<Hit> hits, int[] skipped) throws IOException {
        String[] name = file.getFileName().toString().split("\\.");
        int rx = Integer.parseInt(name[1]);
        int rz = Integer.parseInt(name[2]);
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            if (raf.length() < 8192) return;
            byte[] header = new byte[4096];
            raf.readFully(header);
            for (int i = 0; i < 1024; i++) {
                int offset = ((header[i * 4] & 0xFF) << 16) | ((header[i * 4 + 1] & 0xFF) << 8) | (header[i * 4 + 2] & 0xFF);
                int sectors = header[i * 4 + 3] & 0xFF;
                if (offset == 0 || sectors == 0) continue;
                try {
                    raf.seek(offset * 4096L);
                    int length = raf.readInt();
                    if (length <= 1) continue;
                    int type = raf.readByte() & 0xFF;
                    byte[] data;
                    if ((type & 0x80) != 0) {
                        int cx = rx * 32 + (i % 32);
                        int cz = rz * 32 + (i / 32);
                        Path external = file.getParent().resolve("c." + cx + "." + cz + ".mcc");
                        if (!Files.exists(external)) continue;
                        data = Files.readAllBytes(external);
                        type &= 0x7F;
                    } else {
                        if (length - 1 > sectors * 4096) continue;
                        data = new byte[length - 1];
                        raf.readFully(data);
                    }
                    InputStream in = switch (type) {
                        case 1 -> new GZIPInputStream(new ByteArrayInputStream(data));
                        case 2 -> new InflaterInputStream(new ByteArrayInputStream(data));
                        case 3 -> new ByteArrayInputStream(data);
                        default -> null;
                    };
                    if (in == null) {
                        skipped[0]++;
                        continue;
                    }
                    Object chunk = readRoot(in);
                    if (entities) scanEntities(chunk, dim, hits);
                    else scanChunk(chunk, dim, hits);
                } catch (Throwable t) {
                    skipped[0]++;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void scanChunk(Object chunk, String dim, List<Hit> hits) {
        if (!(chunk instanceof Map<?, ?> root)) return;
        Object list = root.get("block_entities");
        if (!(list instanceof List<?> blockEntities)) return;
        for (Object o : blockEntities) {
            if (!(o instanceof Map<?, ?> be)) continue;
            String where = str(be.get("id")) + " at " + num(be.get("x")) + " " + num(be.get("y")) + " " + num(be.get("z")) + " " + dim;
            walk(be, where, hits);
        }
    }

    private static void scanEntities(Object chunk, String dim, List<Hit> hits) {
        if (!(chunk instanceof Map<?, ?> root)) return;
        Object list = root.get("Entities");
        if (!(list instanceof List<?> entities)) return;
        for (Object o : entities) {
            if (!(o instanceof Map<?, ?> entity)) continue;
            String pos = "";
            if (entity.get("Pos") instanceof List<?> p && p.size() == 3) {
                pos = Math.round(dbl(p.get(0))) + " " + Math.round(dbl(p.get(1))) + " " + Math.round(dbl(p.get(2)));
            }
            walk(entity, "entity " + str(entity.get("id")) + " at " + pos + " " + dim, hits);
        }
    }

    private static void scanPlayers(Path root, List<Hit> hits) throws IOException {
        Path dir = root.resolve("playerdata");
        if (!Files.isDirectory(dir)) return;
        Map<String, String> names = names();
        List<Path> files;
        try (Stream<Path> list = Files.list(dir)) {
            files = list.filter(p -> p.getFileName().toString().endsWith(".dat")).toList();
        }
        for (Path file : files) {
            String uuid = file.getFileName().toString().replace(".dat", "");
            String name = names.getOrDefault(uuid, uuid);
            try (InputStream in = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                Object data = readRoot(in);
                if (!(data instanceof Map<?, ?> player)) continue;
                for (Map.Entry<?, ?> e : player.entrySet()) {
                    String key = String.valueOf(e.getKey());
                    String part = key.equals("EnderItems") ? "ender chest" : key.equals("Inventory") ? "inventory" : key;
                    walk(e.getValue(), "player " + name + " (" + part + ")", hits);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static Map<String, String> names() {
        Map<String, String> out = new HashMap<>();
        Path cache = FabricLoader.getInstance().getGameDir().resolve("usercache.json");
        if (!Files.exists(cache)) return out;
        try (Reader reader = Files.newBufferedReader(cache)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root instanceof JsonArray array) {
                for (JsonElement el : array) {
                    if (el instanceof JsonObject obj && obj.has("uuid") && obj.has("name")) {
                        out.put(obj.get("uuid").getAsString(), obj.get("name").getAsString());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static void walk(Object node, String where, List<Hit> hits) {
        if (node instanceof Map<?, ?> map) {
            if (MACE.equals(map.get("id"))) {
                hits.add(new Hit(where, maceId(map)));
            }
            boolean nestedHolder = map.containsKey("components");
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String suffix = nestedHolder && "components".equals(e.getKey()) && !MACE.equals(map.get("id"))
                        ? " (inside " + str(map.get("id")) + ")" : "";
                walk(e.getValue(), where + suffix, hits);
            }
        } else if (node instanceof List<?> list) {
            for (Object o : list) walk(o, where, hits);
        }
    }

    private static String maceId(Map<?, ?> item) {
        if (item.get("components") instanceof Map<?, ?> comps && comps.get("minecraft:custom_data") instanceof Map<?, ?> custom) {
            Object id = custom.get("macelimit_id");
            if (id instanceof String s && !s.isEmpty()) return s;
        }
        return null;
    }

    private static String str(Object o) {
        return o == null ? "?" : String.valueOf(o);
    }

    private static String num(Object o) {
        return o instanceof Number n ? String.valueOf(n.longValue()) : "?";
    }

    private static double dbl(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0;
    }

    private static Object readRoot(InputStream raw) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(raw));
        int type = in.readByte();
        if (type != 10) return null;
        in.readUTF();
        return read(in, 10, 0);
    }

    private static Object read(DataInputStream in, int type, int depth) throws IOException {
        if (depth > 512) throw new IOException("NBT too deep");
        switch (type) {
            case 1: return in.readByte();
            case 2: return in.readShort();
            case 3: return in.readInt();
            case 4: return in.readLong();
            case 5: return in.readFloat();
            case 6: return in.readDouble();
            case 7: {
                int n = in.readInt();
                in.skipNBytes(n);
                return null;
            }
            case 8: return in.readUTF();
            case 9: {
                int child = in.readByte();
                int n = in.readInt();
                List<Object> list = new ArrayList<>(Math.max(0, Math.min(n, 4096)));
                for (int i = 0; i < n; i++) list.add(read(in, child, depth + 1));
                return list;
            }
            case 10: {
                Map<String, Object> map = new LinkedHashMap<>();
                while (true) {
                    int child = in.readByte();
                    if (child == 0) break;
                    String key = in.readUTF();
                    map.put(key, read(in, child, depth + 1));
                }
                return map;
            }
            case 11: {
                int n = in.readInt();
                in.skipNBytes(n * 4L);
                return null;
            }
            case 12: {
                int n = in.readInt();
                in.skipNBytes(n * 8L);
                return null;
            }
            default:
                throw new IOException("Bad NBT type " + type);
        }
    }
}
