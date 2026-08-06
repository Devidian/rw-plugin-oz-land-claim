package de.omegazirkel.risingworld.landclaim.ui;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.CursorManager;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITextField;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;

public class UIDialogFactory {

    protected static I18n t = I18n.getInstance(LandClaim.name);

    private static final int TEXT_INPUT_DIALOG_WIDTH = 460;
    private static final int CONFIRM_DIALOG_WIDTH = 580;
    private static final int CONFIRM_DIALOG_HEIGHT = 232;
    private static final int TEXT_INPUT_DIALOG_HEIGHT = 232;
    private static final int CONFIRM_TITLE_Y = 5;
    private static final int TEXT_INPUT_TITLE_Y = 5;
    private static final int BODY_X = 24;
    private static final int CONFIRM_BODY_Y = 62;
    private static final int TEXT_INPUT_BODY_Y = 62;
    private static final int CONFIRM_BODY_WIDTH = 532;
    private static final int TEXT_INPUT_BODY_WIDTH = 412;
    private static final int CONFIRM_BODY_HEIGHT = 104;
    private static final int INPUT_BODY_HEIGHT = 98;
    private static final int CONFIRM_FOOTER_Y = 218;
    private static final int TEXT_INPUT_FOOTER_Y = 218;
    private static final int BUTTON_OFFSET_X = 24;
    private static final int FOOTER_BUTTON_WIDTH = 120;
    private static final int FOOTER_BUTTON_HEIGHT = 36;

    private static UIElement getDialogWindow(int width, int height) {
        UIElement window = new UIElement();
        window.setPivot(Pivot.MiddleCenter);
        window.setPosition(50f, 50f, true);
        window.setSize(width, height, false);
        window.setBackgroundColor(0, 0, 0, 0.86f);
        window.setBorderColor(0.95f, 0.75f, 0.25f, 0.6f);
        window.setBorder(1);
        window.setBorderEdgeRadius(6, false);
        return window;
    }

    private static void addTitle(UIElement window, String title, int y) {
        UILabel lblTitle = new UILabel(title);
        lblTitle.setFont(Font.DefaultBold);
        lblTitle.setFontSize(24);
        lblTitle.setTextAlign(TextAnchor.MiddleCenter);
        lblTitle.setPivot(Pivot.UpperCenter);
        lblTitle.setPosition(50f, y, true);
        lblTitle.setSize(90f, 30f, true);
        window.addChild(lblTitle);
    }

    private static UIElement addBody(UIElement window, int y, int width, int height) {
        UIElement body = new UIElement();
        body.setPivot(Pivot.UpperLeft);
        body.setPosition(BODY_X, y, false);
        body.setSize(width, height, false);
        body.setBackgroundColor(0.08f, 0.08f, 0.08f, 0.55f);
        body.setBorder(1);
        body.setBorderColor(0.95f, 0.75f, 0.25f, 0.48f);
        body.setBorderEdgeRadius(4, false);
        window.addChild(body);
        return body;
    }

    private static void styleFooterButton(UIElement button) {
        // AdvancedButton has no intrinsic dimensions. Preserve the legacy dialog
        // button size so its container receives a real clickable surface.
        button.setSize(FOOTER_BUTTON_WIDTH, FOOTER_BUTTON_HEIGHT, false);
        button.setBorderEdgeRadius(4, false);
    }

    public static UIElement getConfirmDangerDialog(
            Player player,
            String title,
            String i18nId,
            Callback<Boolean> onOk,
            Callback<Player> onCancel) {
        return getConfirmDangerDialog(player, title, i18nId, t.get("TC_UI_BTN_YES", player), onOk, onCancel);
    }

    public static UIElement getConfirmDangerDialog(
            Player player,
            String title,
            String i18nId,
            String confirmLabel,
            Callback<Boolean> onOk,
            Callback<Player> onCancel) {

        // --- Window ---
        UIElement window = getDialogWindow(CONFIRM_DIALOG_WIDTH, CONFIRM_DIALOG_HEIGHT);

        // --- Title ---
        addTitle(window, title, CONFIRM_TITLE_Y);

        // --- Label ---
        UIElement body = addBody(window, CONFIRM_BODY_Y, CONFIRM_BODY_WIDTH, CONFIRM_BODY_HEIGHT);
        UILabel lbl = new UILabel(t.get(i18nId, player));
        lbl.setRichTextEnabled(true);
        lbl.setTextWrap(true);
        lbl.setFontSize(16);
        lbl.setTextAlign(TextAnchor.UpperLeft);
        lbl.setPivot(Pivot.UpperLeft);
        lbl.setPosition(16, 12, false);
        lbl.setSize(CONFIRM_BODY_WIDTH - 32, CONFIRM_BODY_HEIGHT - 20, false);
        body.addChild(lbl);

        AdvancedButton btnOk = AdvancedButtonFactory.danger(confirmLabel, event -> {
            player.removeUIElement(window);
            CursorManager.hide(player);
            onOk.onCall(true);
        });

        btnOk.setPivot(Pivot.LowerRight);
        btnOk.setPosition(CONFIRM_DIALOG_WIDTH - BUTTON_OFFSET_X, CONFIRM_FOOTER_Y, false);
        styleFooterButton(btnOk);
        window.addChild(btnOk);

        AdvancedButton btnCancel = AdvancedButtonFactory.ok(t.get("TC_UI_BTN_NO", player), event -> {
            player.removeUIElement(window);
            CursorManager.hide(player);
            onCancel.onCall(player);
        });

        btnCancel.setPivot(Pivot.LowerLeft);
        btnCancel.setPosition(BUTTON_OFFSET_X, CONFIRM_FOOTER_Y, false);
        styleFooterButton(btnCancel);
        window.addChild(btnCancel);

        return window;
    }

    public static UIElement getConfirmDialog(
            Player player,
            String title,
            String i18nId,
            Callback<Boolean> onOk,
            Callback<Player> onCancel) {

        // --- Window ---
        UIElement window = getDialogWindow(CONFIRM_DIALOG_WIDTH, CONFIRM_DIALOG_HEIGHT);

        // --- Title ---
        addTitle(window, title, CONFIRM_TITLE_Y);

        // --- Label ---
        UIElement body = addBody(window, CONFIRM_BODY_Y, CONFIRM_BODY_WIDTH, CONFIRM_BODY_HEIGHT);
        UILabel lbl = new UILabel(t.get(i18nId, player));
        lbl.setRichTextEnabled(true);
        lbl.setTextWrap(true);
        lbl.setFontSize(16);
        lbl.setTextAlign(TextAnchor.UpperLeft);
        lbl.setPivot(Pivot.UpperLeft);
        lbl.setPosition(16, 12, false);
        lbl.setSize(CONFIRM_BODY_WIDTH - 32, CONFIRM_BODY_HEIGHT - 20, false);
        body.addChild(lbl);

        AdvancedButton btnOk = AdvancedButtonFactory.ok(t.get("TC_UI_BTN_YES", player), event -> {
            player.removeUIElement(window);
            CursorManager.hide(player);
            onOk.onCall(true);
        });

        btnOk.setPivot(Pivot.LowerRight);
        btnOk.setPosition(CONFIRM_DIALOG_WIDTH - BUTTON_OFFSET_X, CONFIRM_FOOTER_Y, false);
        styleFooterButton(btnOk);
        window.addChild(btnOk);

        AdvancedButton btnCancel = AdvancedButtonFactory.cancel(t.get("TC_UI_BTN_NO", player), event -> {
            player.removeUIElement(window);
            CursorManager.hide(player);
            onCancel.onCall(player);
        });

        btnCancel.setPivot(Pivot.LowerLeft);
        btnCancel.setPosition(BUTTON_OFFSET_X, CONFIRM_FOOTER_Y, false);
        styleFooterButton(btnCancel);
        window.addChild(btnCancel);

        return window;
    }

    public static UIElement getWarningDialog(Player player, String title, String message, Callback<Player> onClose) {
        UIElement window = getDialogWindow(CONFIRM_DIALOG_WIDTH, CONFIRM_DIALOG_HEIGHT);
        addTitle(window, title, CONFIRM_TITLE_Y);
        UIElement body = addBody(window, CONFIRM_BODY_Y, CONFIRM_BODY_WIDTH, CONFIRM_BODY_HEIGHT);
        UILabel label = new UILabel(message);
        label.setRichTextEnabled(true);
        label.setTextWrap(true);
        label.setFontSize(16);
        label.setTextAlign(TextAnchor.UpperLeft);
        label.setPivot(Pivot.UpperLeft);
        label.setPosition(16, 12, false);
        label.setSize(CONFIRM_BODY_WIDTH - 32, CONFIRM_BODY_HEIGHT - 20, false);
        body.addChild(label);
        AdvancedButton close = AdvancedButtonFactory.danger(t.get("TC_UI_BTN_OK", player), event -> {
            player.removeUIElement(window);
            CursorManager.hide(player);
            if (onClose != null) onClose.onCall(player);
        });
        close.setPivot(Pivot.LowerRight);
        close.setPosition(CONFIRM_DIALOG_WIDTH - BUTTON_OFFSET_X, CONFIRM_FOOTER_Y, false);
        styleFooterButton(close);
        window.addChild(close);
        return window;
    }

    public static UIElement getTextInput(
            Player player,
            String title,
            String defaultText,
            Callback<String> onOk,
            Callback<Player> onCancel) {

        // --- Window ---
        UIElement window = getDialogWindow(TEXT_INPUT_DIALOG_WIDTH, TEXT_INPUT_DIALOG_HEIGHT);

        // --- Title ---
        addTitle(window, title, TEXT_INPUT_TITLE_Y);

        // --- Label ---
        UIElement body = addBody(window, TEXT_INPUT_BODY_Y, TEXT_INPUT_BODY_WIDTH, INPUT_BODY_HEIGHT);
        UILabel lbl = new UILabel(t.get("TC_UI_LABEL_INPUT", player));
        lbl.setFontSize(16);
        lbl.setTextAlign(TextAnchor.UpperLeft);
        lbl.setPivot(Pivot.UpperLeft);
        lbl.setPosition(16, 12, false);
        lbl.setSize(TEXT_INPUT_BODY_WIDTH - 32, 22, false);
        body.addChild(lbl);

        // --- TextField ---
        UITextField txt = new UITextField();
        txt.setText(defaultText != null ? defaultText : "");
        txt.setSize(400, 42, false);
        txt.setPosition(6, 44, false);
        txt.setPivot(Pivot.UpperLeft);
        txt.setBackgroundColor(0.02f, 0.02f, 0.02f, 0.78f);
        txt.setBorder(1);
        txt.setBorderColor(0.95f, 0.75f, 0.25f, 0.46f);
        txt.setBorderEdgeRadius(4, false);
        body.addChild(txt);

        AdvancedButton btnOk = AdvancedButtonFactory.ok(t.get("TC_UI_BTN_OK", player), event -> {
            txt.getCurrentText(player, (String text) -> {
                player.removeUIElement(window);
                CursorManager.hide(player);
                onOk.onCall(text.trim());
            });
        });

        btnOk.setPivot(Pivot.LowerRight);
        btnOk.setPosition(TEXT_INPUT_DIALOG_WIDTH - BUTTON_OFFSET_X, TEXT_INPUT_FOOTER_Y, false);
        styleFooterButton(btnOk);
        window.addChild(btnOk);

        AdvancedButton btnCancel = AdvancedButtonFactory.cancel(t.get("TC_UI_BTN_CANCEL", player), event -> {
            player.removeUIElement(window);
            CursorManager.hide(player);
            onCancel.onCall(player);
        });

        btnCancel.setPivot(Pivot.LowerLeft);
        btnCancel.setPosition(BUTTON_OFFSET_X, TEXT_INPUT_FOOTER_Y, false);
        styleFooterButton(btnCancel);
        window.addChild(btnCancel);

        return window;
    }
}
