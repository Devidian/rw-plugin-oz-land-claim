package de.omegazirkel.risingworld.landclaim;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.utils.AreaPermissionTemplates;
import net.risingworld.api.Plugin;

public class PermissionFileUtil {

    private final Plugin plugin;

    public static OZLogger logger() {
        return LandClaim.logger();
    }

    public PermissionFileUtil(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Copies a permission file from inside the plugin JAR into the server's
     * Permissions/Areas directory.
     *
     * @param sourceName The resource file name inside the JAR (e.g.
     *                   "ozlc-owner.json")
     * @param overwrite  If true, overwrite existing file
     * @return true if file was copied, false if skipped
     */
    public boolean copyPermissionFile(String sourceName, boolean overwrite) {
        return AreaPermissionTemplates.copy(plugin, logger(), sourceName, overwrite);
    }

    /**
     * Copies multiple permission files from plugin resources.
     *
     * @param overwrite If true, overwrite existing files
     * @param files     File names relative to permissions/
     */
    public boolean copyPermissionFiles(boolean overwrite, String... files) {
        boolean someCopied = false;
        for (String file : files) {
            someCopied |= copyPermissionFile(file, overwrite);
        }
        return someCopied;
    }
}
