package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Map;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.CustomAreaPermissionService;
import de.omegazirkel.risingworld.landclaim.CustomPermissionFlag;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlay;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.SwitchButton;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UIScrollView;
import net.risingworld.api.ui.UIScrollView.ScrollViewMode;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

/** Edits the flags of the area's shared custom group; the file is only written on apply. */
public final class CustomPermissionOverlay extends BasePluginOverlay {
    public static final String ATTRIBUTE_KEY = "landclaim-custom-permission-overlay";
    private static final int ROW_HEIGHT = 32;
    private static final int SECTION_HEIGHT = 28;
    private static final int PADDING = 12;
    private static final int BUTTON_HEIGHT = 30;
    private static final int SCROLL_HEIGHT = 360;
    private static final int LABEL_WIDTH = 340;
    private static final int TOGGLE_X = 360;

    private final Area area;
    private final String groupName;
    private final Map<CustomPermissionFlag, Boolean> draft;
    private final Callback<Player> reopenPermissionOnClose;

    private CustomPermissionOverlay(Player player, Area area, Callback<Player> reopenPermissionOnClose) {
        super(player, p -> {
        });
        this.area = area;
        this.groupName = CustomAreaPermissionService.groupName(area.getID());
        this.draft = CustomAreaPermissionService.readFlags(groupName);
        this.reopenPermissionOnClose = reopenPermissionOnClose != null ? reopenPermissionOnClose : p -> {
        };
        rebuild();
        populate();
    }

    /**
     * Opens the editor if the given player currently holds the area's custom group.
     * Closes the permission manager while the editor is open and reopens it on close.
     */
    public static void open(Player player, Area area, int playerDbId) {
        if (!CustomAreaPermissionService.isAvailable(area)
                || !CustomAreaPermissionService.groupName(area.getID()).equals(area.getPlayerPermission(playerDbId)))
            return;

        Callback<Player> permissionOnClose = p -> {
        };
        UIElement permissionUi = (UIElement) player.getAttribute(PermissionOverlay.ATTRIBUTE_KEY);
        if (permissionUi instanceof PermissionOverlay permissionOverlay) {
            permissionOnClose = permissionOverlay.closeCallback();
            player.deleteAttribute(PermissionOverlay.ATTRIBUTE_KEY);
            player.removeUIElement(permissionOverlay);
        }

        UIElement existing = (UIElement) player.getAttribute(ATTRIBUTE_KEY);
        if (existing != null)
            player.removeUIElement(existing);
        CustomPermissionOverlay overlay = new CustomPermissionOverlay(player, area, permissionOnClose);
        player.addUIElement(overlay, UITarget.Modal);
        player.setAttribute(ATTRIBUTE_KEY, overlay);
    }

    @Override
    protected I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    @Override
    protected float panelWidthPercent() {
        return 52f;
    }

    @Override
    protected float bodyHeightPixels() {
        return PADDING + SCROLL_HEIGHT + PADDING + BUTTON_HEIGHT + PADDING;
    }

    @Override
    protected float panelHeightPixels() {
        return 124 + bodyHeightPixels() + 44;
    }

    @Override
    protected String titleText() {
        return t().get("tc.ui.custom.title", uiPlayer);
    }

    @Override
    protected String descriptionText() {
        return t().get("tc.ui.custom.subtitle", uiPlayer)
                .replace("PH_AREA_NAME", area.getName() != null ? area.getName() : "N/A");
    }

    @Override
    protected String legendText() {
        return t().get("tc.ui.custom.legend", uiPlayer);
    }

    /** Closes the editor and brings the permission manager back. */
    @Override
    protected void close() {
        uiPlayer.deleteAttribute(ATTRIBUTE_KEY);
        uiPlayer.removeUIElement(this);
        PermissionOverlay.open(uiPlayer, area, reopenPermissionOnClose);
    }

    private void populate() {
        UIScrollView scroll = new UIScrollView(ScrollViewMode.Vertical);
        scroll.setPivot(Pivot.UpperLeft);
        scroll.setPosition(0, 0, false);
        scroll.style.width.set(100, Unit.Percent);
        scroll.style.height.set(SCROLL_HEIGHT, Unit.Pixel);
        scroll.style.paddingLeft.set(8);
        scroll.style.paddingRight.set(8);
        scroll.style.paddingTop.set(4);
        scroll.style.paddingBottom.set(4);
        body.addChild(scroll);

        OZUIElement list = new OZUIElement();
        list.setPivot(Pivot.UpperLeft);
        list.setPosition(0, 0, false);
        list.style.width.set(100, Unit.Percent);

        int top = 0;
        String lastSection = null;
        for (CustomPermissionFlag flag : CustomPermissionFlag.values()) {
            if (!flag.section.equals(lastSection)) {
                lastSection = flag.section;
                UILabel section = new UILabel(t().get("tc.ui.custom.section." + flag.section, uiPlayer));
                section.setFont(Font.DefaultBold);
                section.setFontSize(14);
                section.setTextAlign(TextAnchor.MiddleLeft);
                section.setPivot(Pivot.UpperLeft);
                section.setPosition(8, top, false);
                section.setSize(LABEL_WIDTH, SECTION_HEIGHT, false);
                list.addChild(section);
                top += SECTION_HEIGHT;
            }

            UILabel label = new UILabel(t().get(flag.i18nKey, uiPlayer));
            label.setFontSize(13);
            label.setTextAlign(TextAnchor.MiddleLeft);
            label.setPivot(Pivot.UpperLeft);
            label.setPosition(8, top, false);
            label.setSize(LABEL_WIDTH, ROW_HEIGHT - 4, false);
            list.addChild(label);

            SwitchButton toggle = new SwitchButton(Boolean.TRUE.equals(draft.get(flag)), value -> draft.put(flag, value));
            toggle.setPivot(Pivot.UpperLeft);
            toggle.setPosition(TOGGLE_X, top + 4, false);
            list.addChild(toggle);
            top += ROW_HEIGHT;
        }
        list.style.height.set(top, Unit.Pixel);
        scroll.addChild(list);

        int buttonY = SCROLL_HEIGHT + PADDING;
        AdvancedButton cancel = AdvancedButtonFactory.cancel(t().get("tc.ui.custom.cancel", uiPlayer), event -> close());
        button(cancel, 18, buttonY);
        AdvancedButton apply = AdvancedButtonFactory.ok(t().get("tc.ui.custom.apply", uiPlayer), event -> {
            if (!CustomAreaPermissionService.writeFlags(groupName, draft)) {
                uiPlayer.sendTextMessage(t().get("tc.ui.custom.failed", uiPlayer));
                return;
            }
            close();
        });
        button(apply, 150, buttonY);
    }

    private void button(AdvancedButton button, int x, int y) {
        button.setPivot(Pivot.UpperLeft);
        button.setPosition(x, y, false);
        button.setSize(120, BUTTON_HEIGHT, false);
        body.addChild(button);
    }
}
