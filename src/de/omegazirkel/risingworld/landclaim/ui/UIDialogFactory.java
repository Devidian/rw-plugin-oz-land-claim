package de.omegazirkel.risingworld.landclaim.ui;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.ButtonFactory;
import de.omegazirkel.risingworld.tools.ui.CancelButton;
import de.omegazirkel.risingworld.tools.ui.CursorManager;
import de.omegazirkel.risingworld.tools.ui.DangerButton;
import de.omegazirkel.risingworld.tools.ui.OkButton;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITextField;
import net.risingworld.api.ui.style.Pivot;

public class UIDialogFactory {

    protected static I18n t = I18n.getInstance(LandClaim.name);

    private static UIElement getDialogWindow() {
        UIElement window = new UIElement();
        window.setPivot(Pivot.MiddleCenter);
        window.setPosition(50f, 50f, true);
        window.setSize(420, 180, false);
        window.setBackgroundColor(0, 0, 0, 0.85f);
        window.setBorderColor(1, 1, 1, 0.4f);
        window.setBorder(2);
        return window;
    }

    public static UIElement getConfirmDangerDialog(
            Player player,
            String title,
            String i18nId,
            Callback<Boolean> onOk,
            Callback<Player> onCancel) {

        // --- Window ---
        UIElement window = getDialogWindow();

        // --- Title ---
        UILabel lblTitle = new UILabel(title);
        lblTitle.setFontSize(22);
        lblTitle.setPivot(Pivot.MiddleCenter);
        lblTitle.setPosition(50f, 10f, true);
        window.addChild(lblTitle);

        // --- Label ---
        UILabel lbl = new UILabel(t.get(i18nId, player));
        lbl.setFontSize(16);
        lbl.setPivot(Pivot.UpperLeft);
        lbl.setPosition(5f, 28f, true);
        window.addChild(lbl);

        DangerButton btnOk = ButtonFactory.danger(t.get("TC_UI_BTN_YES", player), event -> {
            player.removeUIElement(window);
            CursorManager.hide(player);
            onOk.onCall(true);
        });

        btnOk.setPivot(Pivot.LowerRight);
        btnOk.setPosition(410, 170, false);
        window.addChild(btnOk);

        OkButton btnCancel = ButtonFactory.ok(t.get("TC_UI_BTN_NO", player), event -> {
            player.removeUIElement(window);
            CursorManager.hide(player);
            onCancel.onCall(player);
        });

        btnCancel.setPivot(Pivot.LowerLeft);
        btnCancel.setPosition(10, 170, false);
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
        UIElement window = getDialogWindow();

        // --- Title ---
        UILabel lblTitle = new UILabel(title);
        lblTitle.setFontSize(22);
        lblTitle.setPivot(Pivot.MiddleCenter);
        lblTitle.setPosition(50f, 10f, true);
        window.addChild(lblTitle);

        // --- Label ---
        UILabel lbl = new UILabel(t.get(i18nId, player));
        lbl.setFontSize(16);
        lbl.setPivot(Pivot.UpperLeft);
        lbl.setPosition(5f, 28f, true);
        window.addChild(lbl);

        OkButton btnOk = ButtonFactory.ok(t.get("TC_UI_BTN_YES", player), event -> {
            player.removeUIElement(window);
            CursorManager.hide(player);
            onOk.onCall(true);
        });

        btnOk.setPivot(Pivot.LowerRight);
        btnOk.setPosition(410, 170, false);
        window.addChild(btnOk);

        CancelButton btnCancel = ButtonFactory.cancel(t.get("TC_UI_BTN_NO", player), event -> {
            player.removeUIElement(window);
            CursorManager.hide(player);
            onCancel.onCall(player);
        });

        btnCancel.setPivot(Pivot.LowerLeft);
        btnCancel.setPosition(10, 170, false);
        window.addChild(btnCancel);

        return window;
    }

    public static UIElement getTextInput(
            Player player,
            String title,
            String defaultText,
            Callback<String> onOk,
            Callback<Player> onCancel) {

        // --- Window ---
        UIElement window = getDialogWindow();

        // --- Title ---
        UILabel lblTitle = new UILabel(title);
        lblTitle.setFontSize(22);
        lblTitle.setPivot(Pivot.MiddleCenter);
        lblTitle.setPosition(50f, 10f, true);
        window.addChild(lblTitle);

        // --- Label ---
        UILabel lbl = new UILabel(t.get("TC_UI_LABEL_INPUT", player));
        lbl.setFontSize(16);
        lbl.setPivot(Pivot.UpperLeft);
        lbl.setPosition(5f, 28f, true);
        window.addChild(lbl);

        // --- TextField ---
        UITextField txt = new UITextField();
        txt.setText(defaultText != null ? defaultText : "");
        txt.setSize(90f, 30f, true);
        txt.setPosition(5f, 45f, true);
        txt.setPivot(Pivot.UpperLeft);
        window.addChild(txt);

        OkButton btnOk = ButtonFactory.ok(t.get("TC_UI_BTN_OK", player), event -> {
            txt.getCurrentText(player, (String text) -> {
                player.removeUIElement(window);
                CursorManager.hide(player);
                onOk.onCall(text.trim());
            });
        });

        btnOk.setPivot(Pivot.LowerRight);
        btnOk.setPosition(410, 170, false);
        window.addChild(btnOk);

        CancelButton btnCancel = ButtonFactory.cancel(t.get("TC_UI_BTN_CANCEL", player), event -> {
            player.removeUIElement(window);
            CursorManager.hide(player);
            onCancel.onCall(player);
        });

        btnCancel.setPivot(Pivot.LowerLeft);
        btnCancel.setPosition(10, 170, false);
        window.addChild(btnCancel);

        return window;
    }
}
