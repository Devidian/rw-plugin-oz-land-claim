package de.omegazirkel.risingworld;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.Plugin;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

public class MavenTemplate extends Plugin implements Listener, FileChangeListener {
	static final String pluginCMD = "mt";
	static final Colors c = Colors.getInstance();
	private static I18n t = null;

	public static OZLogger logger() {
		return OZLogger.getInstance("MavenTemplate");
	}

	@Override
	public void onEnable() {
		t = new I18n(this);
		registerEventListener(this);
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void onSettingsChanged(Path settingsPath) {
		initSettings(settingsPath.toString());
	}

	@EventMethod
	public void onPlayerCommand(PlayerCommandEvent event) {
		Player player = event.getPlayer();
		String lang = player.getSystemLanguage();
		String commandLine = event.getCommand();
		Vector3f pos = player.getPosition();

		String[] cmdParts = commandLine.split(" ", 2);
		String command = cmdParts[0];

		if (command.equals("/" + pluginCMD)) {
			// Invalid number of arguments (0)
			if (cmdParts.length < 2) {
				return;
			}
			String option = cmdParts[1];
			switch (option) {
				case "status":
					String statusMessage = t.get("CMD_STATUS", lang)
							.replace("PH_VERSION", c.okay + this.getDescription("version") + c.endTag)
							.replace("PH_LANGUAGE",
									c.info + player.getLanguage() + " / " + player.getSystemLanguage() + c.endTag)
							.replace("PH_USEDLANG", c.okay + t.getLanguageUsed(lang) + c.endTag)
							.replace("PH_LANG_AVAILABLE", c.warning + t.getLanguageAvailable() + c.endTag);
					player.sendTextMessage(c.okay + this.getName() + ":> " + c.text + statusMessage);
					break;
				default:
					break;
			}
		}
	}

	private void initSettings(String filePath) {
		Path settingsFile = Paths.get(filePath);
		Path defaultSettingsFile = settingsFile.resolveSibling("settings.default.properties");

		try {
			if (Files.notExists(settingsFile) && Files.exists(defaultSettingsFile)) {
				logger().info("settings.properties not found, copying from settings.default.properties...");
				Files.copy(defaultSettingsFile, settingsFile);
			}

			Properties settings = new Properties();
			if (Files.exists(settingsFile)) {
				try (FileInputStream in = new FileInputStream(settingsFile.toFile())) {
					settings.load(new InputStreamReader(in, "UTF8"));
				}
			} else {
				logger().warn(
						"⚠️ Neither settings.properties nor settings.default.properties found. Using default values.");
			}
			// fill properties
			// myBooleanProperty = settings.getProperty("booleanProperty",
			// "false").contentEquals("true");
			// myStringProperty = settings.getProperty("stringProperty", "");

			logger().info(this.getName() + " Plugin settings loaded");

		} catch (IOException ex) {
			logger().error("IOException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
		} catch (NumberFormatException ex) {
			logger().error("NumberFormatException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

}
