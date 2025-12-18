package de.omegazirkel.risingworld.landclaim;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.OZLogger;
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
        File targetDir = new File(plugin.getPath() + "/../../Permissions/Areas/");
        if (!targetDir.exists()) {
            if (targetDir.mkdirs()) {
                logger().info("Created permission target directory: " + targetDir.getAbsolutePath());
            } else {
                logger().error("Failed to create Permissions/Areas directory: " + targetDir.getAbsolutePath());
                return false;
            }
        }

        File targetFile = new File(targetDir, sourceName);

        // Skip copy if file exists and overwrite == false
        if (targetFile.exists() && !overwrite) {
            logger().info("Permission file already exists (skipped): " + targetFile.getName());
            return false;
        }

        // Try loading the resource from inside plugin JAR
        try (InputStream in = plugin.getClass().getClassLoader().getResourceAsStream("permissions/" + sourceName)) {
            if (in == null) {
                logger().error("Permission resource not found in JAR: permissions/" + sourceName);
                return false;
            }

            // Copy resource to target file
            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            logger().info("Copied permission file: " + targetFile.getAbsolutePath());
            return true;

        } catch (IOException ex) {
            logger().error("Failed to copy permission file " + sourceName + ": " + ex.getMessage());
            return false;
        }
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
