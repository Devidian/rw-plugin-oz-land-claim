package de.omegazirkel.risingworld.landclaim;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.Server;
import net.risingworld.api.Timer;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;

/**
 * Custom area permission groups. Every area has at most one shared group file
 * ({@code <prefix><areaId>.json}) under Permissions/Areas; all players set to
 * Custom on that area share its permissions.
 */
public class CustomAreaPermissionService {

    private static final PluginSettings s = PluginSettings.getInstance();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int ASSIGN_RETRY_COUNT = 10;
    private static final float ASSIGN_RETRY_INTERVAL = 0.4f;
    private static final String RELOAD_PERMISSION_KEY = "command_reloadpermissions";

    public static OZLogger logger() {
        return LandClaim.logger();
    }

    public static String groupName(long areaId) {
        return s.customAreaPermissionPrefix + areaId;
    }

    public static boolean isCustomGroup(String permission) {
        return permission != null && permission.startsWith(s.customAreaPermissionPrefix)
                && !permission.equals(s.customAreaPermissionTemplate);
    }

    /**
     * Custom is only offered on normal player claims. Leased areas are excluded because
     * the tenant would otherwise edit or delete the landlord's shared group.
     */
    public static boolean isAvailable(Area area) {
        if (!s.defaultAreaPermission.equals(area.getDefaultPermission()))
            return false;
        if (LandClaim.unclaimedLeaseService() != null
                && LandClaim.unclaimedLeaseService().find(area.getID()).isPresent())
            return false;
        return LandClaim.playerLeaseService() == null
                || LandClaim.playerLeaseService().find(area.getID()).map(lease -> !lease.occupied()).orElse(true);
    }

    /** Creates the area's group from the template if needed and assigns it to the player. */
    public static void assignCustom(Area area, int playerDbId, Callback<Boolean> done) {
        if (!ensureGroupFile(groupName(area.getID()))) {
            done.onCall(false);
            return;
        }
        assignWhenLoaded(Map.of(area, List.of(playerDbId)), done);
    }

    /**
     * Assigns each area's custom group to the given players. New group files are only
     * known to the engine after an asynchronous reload, so failed assignments are retried.
     */
    public static void assignWhenLoaded(Map<Area, List<Integer>> assignments, Callback<Boolean> done) {
        if (trySetPlayerPermissions(assignments)) {
            finishAssign(assignments, true, done);
            return;
        }
        reloadPermissions();
        AtomicBoolean finished = new AtomicBoolean(false);
        final int[] attempt = { 0 };
        Timer timer = new Timer(ASSIGN_RETRY_INTERVAL, ASSIGN_RETRY_INTERVAL, ASSIGN_RETRY_COUNT, () -> {
            if (finished.get())
                return;
            attempt[0]++;
            if (attempt[0] == 1 || attempt[0] == 4 || attempt[0] == 7)
                reloadPermissions();
            boolean ok = trySetPlayerPermissions(assignments);
            if (ok || attempt[0] >= ASSIGN_RETRY_COUNT) {
                if (finished.compareAndSet(false, true)) {
                    if (!ok)
                        logger().error("Failed to assign custom permission groups after " + attempt[0] + " attempts");
                    finishAssign(assignments, ok, done);
                }
            }
        });
        timer.start();
    }

    private static void finishAssign(Map<Area, List<Integer>> assignments, boolean ok, Callback<Boolean> done) {
        // Drop copies that nobody ended up using (failed assign, or area already removed).
        for (Area area : assignments.keySet()) {
            releaseIfUnused(area, groupName(area.getID()));
        }
        done.onCall(ok);
    }

    /**
     * Copies a custom group to another area, falling back to the template when
     * the source file is missing. Assign the copy with {@link #assignWhenLoaded}.
     */
    public static boolean copyToArea(String sourceGroupName, long areaId) {
        String source = groupFile(sourceGroupName).exists() ? sourceGroupName : s.customAreaPermissionTemplate;
        if (!groupFile(source).exists())
            ensureTemplatePresent();
        return copyGroupFile(source, groupName(areaId));
    }

    /**
     * Deletes the given custom group once no player on the area and no pending
     * player lease snapshot of the area uses it anymore. Missing areas always drop
     * their file.
     */
    public static void releaseIfUnused(Area area, String groupName) {
        if (!isCustomGroup(groupName))
            return;
        if (area == null || Server.getArea(area.getID()) == null) {
            deleteGroupFile(groupName);
            return;
        }
        Map<Integer, String> permissions = area.getAllPlayerPermissions();
        if (permissions != null && permissions.containsValue(groupName))
            return;
        if (LandClaim.playerLeaseService() != null
                && LandClaim.playerLeaseService().permissionSnapshot(area.getID()).containsValue(groupName))
            return;
        deleteGroupFile(groupName);
    }

    private static void deleteGroupFile(String groupName) {
        if (!isCustomGroup(groupName))
            return;
        try {
            Files.deleteIfExists(groupFile(groupName).toPath());
        } catch (IOException ex) {
            logger().error("Failed to delete custom permission file " + groupName + ": " + ex.getMessage());
        }
    }

    /** Reads the editable flags of a custom group; missing values are false. */
    public static Map<CustomPermissionFlag, Boolean> readFlags(String groupName) {
        Map<CustomPermissionFlag, Boolean> flags = new EnumMap<>(CustomPermissionFlag.class);
        JsonObject root = readGroup(groupName);
        for (CustomPermissionFlag flag : CustomPermissionFlag.values()) {
            JsonObject section = root == null ? null : asObject(root.get(flag.section));
            JsonElement value = section == null ? null : section.get(flag.key);
            flags.put(flag, value != null && value.isJsonPrimitive() && value.getAsBoolean());
        }
        return flags;
    }

    /** Writes the given flags into the custom group file and reloads permissions once. */
    public static boolean writeFlags(String groupName, Map<CustomPermissionFlag, Boolean> flags) {
        if (!isCustomGroup(groupName))
            return false;
        JsonObject root = readGroup(groupName);
        if (root == null)
            return false;
        for (Map.Entry<CustomPermissionFlag, Boolean> entry : flags.entrySet()) {
            JsonObject section = asObject(root.get(entry.getKey().section));
            if (section == null) {
                section = new JsonObject();
                root.add(entry.getKey().section, section);
            }
            section.addProperty(entry.getKey().key, entry.getValue());
        }
        try (Writer writer = Files.newBufferedWriter(groupFile(groupName).toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException ex) {
            logger().error("Failed to write custom permission file " + groupName + ": " + ex.getMessage());
            return false;
        }
        reloadPermissions();
        return true;
    }

    public static void cleanupArea(long areaId) {
        deleteGroupFile(groupName(areaId));
    }

    /**
     * Reloads permission JSON files through an online player's ingame console
     * command. {@link Server#sendInputCommand} cannot run this: on dedicated
     * servers {@code reloadpermissions} is not an input command.
     */
    public static void reloadPermissions() {
        Player actor = null;
        for (Player player : Server.getAllPlayers()) {
            if (player == null)
                continue;
            if (player.isAdmin()) {
                actor = player;
                break;
            }
            if (actor == null)
                actor = player;
        }
        if (actor == null) {
            logger().warn("Cannot reload permissions: no online player for reloadpermissions");
            return;
        }
        if (Boolean.TRUE.equals(actor.getPermissionValue(RELOAD_PERMISSION_KEY, false))) {
            actor.executeCommand("reloadpermissions");
            return;
        }
        // executeCommand respects the player's command permission, so grant it only briefly
        Player grantee = actor;
        grantee.setPermissionValue(RELOAD_PERMISSION_KEY, true);
        grantee.executeCommand("reloadpermissions");
        new Timer(1f, 0f, 1, () -> grantee.setPermissionValue(RELOAD_PERMISSION_KEY, false)).start();
    }

    private static boolean ensureGroupFile(String groupName) {
        if (groupFile(groupName).exists())
            return true;
        ensureTemplatePresent();
        return copyGroupFile(s.customAreaPermissionTemplate, groupName);
    }

    private static void ensureTemplatePresent() {
        new PermissionFileUtil(LandClaim.getInstance())
                .copyPermissionFile(s.customAreaPermissionTemplate + ".json", false);
    }

    private static boolean trySetPlayerPermissions(Map<Area, List<Integer>> assignments) {
        for (Map.Entry<Area, List<Integer>> entry : assignments.entrySet()) {
            // expand removes areas it has just split off
            if (Server.getArea(entry.getKey().getID()) == null)
                continue;
            String groupName = groupName(entry.getKey().getID());
            for (int playerDbId : entry.getValue()) {
                try {
                    entry.getKey().setPlayerPermission(playerDbId, groupName);
                } catch (RuntimeException ex) {
                    // "Cannot find area permission" until the engine has loaded the group file
                    return false;
                }
                if (!groupName.equals(entry.getKey().getPlayerPermission(playerDbId)))
                    return false;
            }
        }
        return true;
    }

    private static boolean copyGroupFile(String sourceGroupName, String targetGroupName) {
        File source = groupFile(sourceGroupName);
        if (!source.exists()) {
            logger().error("Custom permission source missing: " + source.getName());
            return false;
        }
        File target = groupFile(targetGroupName);
        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger().info("Created custom permission file: " + target.getName());
            return true;
        } catch (IOException ex) {
            logger().error("Failed to create custom permission file " + target.getName() + ": " + ex.getMessage());
            return false;
        }
    }

    private static JsonObject readGroup(String groupName) {
        File file = groupFile(groupName);
        if (!file.exists())
            return null;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | JsonParseException | IllegalStateException ex) {
            logger().error("Failed to read custom permission file " + groupName + ": " + ex.getMessage());
            return null;
        }
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static File groupFile(String groupName) {
        return new File(directory(), groupName + ".json");
    }

    private static File directory() {
        return new PermissionFileUtil(LandClaim.getInstance()).areaPermissionDirectory();
    }
}
