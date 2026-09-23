package de.omegazirkel.risingworld.landclaim.ui;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.SwitchButton;
import net.risingworld.api.Timer;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITextField;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;

public class UIDialogFactory {
    public record RentalOfferInput(String purchasePrice, String dailyRent, boolean rentToOwn) { }

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
        styleFooterButton(button, FOOTER_BUTTON_WIDTH);
    }

    private static void styleFooterButton(UIElement button, int width) {
        // AdvancedButton has no intrinsic dimensions. Preserve the legacy dialog
        // button size so its container receives a real clickable surface.
        button.setSize(width, FOOTER_BUTTON_HEIGHT, false);
        button.setBorderEdgeRadius(4, false);
    }

    private static int confirmButtonWidth(String label) {
        int characters = label == null ? 0 : label.length();
        return Math.max(FOOTER_BUTTON_WIDTH, Math.min(260, 28 + characters * 9));
    }

    /**
     * Removing the element alone leaves the modal input target active until the
     * next Escape press. Clear it before any callback reopens a radial menu.
     */
    private static void closeModal(Player player, UIElement window, Runnable onClosed) {
        closeModal(player, window, true, onClosed);
    }

    private static void closeModal(Player player, UIElement window, boolean closeAllActiveUiWindows,
            Runnable onClosed) {
        player.removeUIElement(window);
        if (!closeAllActiveUiWindows) {
            onClosed.run();
            return;
        }
        player.closeAllActiveUIWindows();
        // The engine applies closeAllActiveUIWindows asynchronously. Wait for a
        // complete UI cycle before a callback adds its replacement modal/menu.
        Timer reopenTimer = new Timer(0.5f, 0f, 1, onClosed);
        reopenTimer.start();
    }

    public static UIElement getConfirmDangerDialog(
            Player player,
            String title,
            String i18nId,
            Callback<Boolean> onOk,
            Callback<Player> onCancel) {
        return getConfirmDangerDialog(player, title, i18nId, t.get("tc.ui.btn.yes", player), onOk, onCancel);
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
            closeModal(player, window, () -> onOk.onCall(true));
        });

        btnOk.setPivot(Pivot.LowerRight);
        btnOk.setPosition(CONFIRM_DIALOG_WIDTH - BUTTON_OFFSET_X, CONFIRM_FOOTER_Y, false);
        styleFooterButton(btnOk, confirmButtonWidth(confirmLabel));
        window.addChild(btnOk);

        AdvancedButton btnCancel = AdvancedButtonFactory.ok(t.get("tc.ui.btn.no", player), event -> {
            closeModal(player, window, () -> onCancel.onCall(player));
        });

        btnCancel.setPivot(Pivot.LowerLeft);
        btnCancel.setPosition(BUTTON_OFFSET_X, CONFIRM_FOOTER_Y, false);
        styleFooterButton(btnCancel);
        window.addChild(btnCancel);

        return window;
    }

    /**
     * Builds a confirmation dialog from already rendered text. This is used for
     * dynamic messages (names, calculated amounts and process descriptions),
     * which must not be looked up as translation keys again.
     */
    public static UIElement getConfirmDangerDialogText(
            Player player,
            String title,
            String message,
            int bodyHeight,
            Callback<Boolean> onOk,
            Callback<Player> onCancel) {
        return getConfirmDangerDialogText(player, title, message, bodyHeight, onOk, onCancel, true);
    }

    /**
     * A dialog opened on top of an existing overlay must remove only itself;
     * closing all active UI windows would also remove that underlying overlay.
     */
    public static UIElement getConfirmDangerDialogText(
            Player player,
            String title,
            String message,
            int bodyHeight,
            Callback<Boolean> onOk,
            Callback<Player> onCancel,
            boolean closeAllActiveUiWindows) {
        int safeBodyHeight = Math.max(CONFIRM_BODY_HEIGHT, bodyHeight);
        int height = CONFIRM_BODY_Y + safeBodyHeight + 66;
        int footerY = height - 14;
        UIElement window = getDialogWindow(CONFIRM_DIALOG_WIDTH, height);
        addTitle(window, title, CONFIRM_TITLE_Y);
        UIElement body = addBody(window, CONFIRM_BODY_Y, CONFIRM_BODY_WIDTH, safeBodyHeight);
        UILabel label = new UILabel(message == null ? "" : message);
        label.setRichTextEnabled(true);
        label.setTextWrap(true);
        label.setFontSize(16);
        label.setTextAlign(TextAnchor.UpperLeft);
        label.setPivot(Pivot.UpperLeft);
        label.setPosition(16, 12, false);
        label.setSize(CONFIRM_BODY_WIDTH - 32, safeBodyHeight - 20, false);
        body.addChild(label);

        String confirmLabel = t.get("tc.ui.btn.yes", player);
        AdvancedButton ok = AdvancedButtonFactory.danger(confirmLabel, event -> {
            closeModal(player, window, closeAllActiveUiWindows, () -> onOk.onCall(true));
        });
        ok.setPivot(Pivot.LowerRight);
        ok.setPosition(CONFIRM_DIALOG_WIDTH - BUTTON_OFFSET_X, footerY, false);
        styleFooterButton(ok, confirmButtonWidth(confirmLabel));
        window.addChild(ok);

        AdvancedButton cancel = AdvancedButtonFactory.ok(t.get("tc.ui.btn.no", player), event -> {
            closeModal(player, window, closeAllActiveUiWindows, () -> onCancel.onCall(player));
        });
        cancel.setPivot(Pivot.LowerLeft);
        cancel.setPosition(BUTTON_OFFSET_X, footerY, false);
        styleFooterButton(cancel);
        window.addChild(cancel);
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

        AdvancedButton btnOk = AdvancedButtonFactory.ok(t.get("tc.ui.btn.yes", player), event -> {
            closeModal(player, window, () -> onOk.onCall(true));
        });

        btnOk.setPivot(Pivot.LowerRight);
        btnOk.setPosition(CONFIRM_DIALOG_WIDTH - BUTTON_OFFSET_X, CONFIRM_FOOTER_Y, false);
        styleFooterButton(btnOk);
        window.addChild(btnOk);

        AdvancedButton btnCancel = AdvancedButtonFactory.cancel(t.get("tc.ui.btn.no", player), event -> {
            closeModal(player, window, () -> onCancel.onCall(player));
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
        AdvancedButton close = AdvancedButtonFactory.danger(t.get("tc.ui.btn.ok", player), event -> {
            closeModal(player, window, () -> {
                if (onClose != null) onClose.onCall(player);
            });
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
        UILabel lbl = new UILabel(t.get("tc.ui.label.input", player));
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

        AdvancedButton btnOk = AdvancedButtonFactory.ok(t.get("tc.ui.btn.ok", player), event -> {
            txt.getCurrentText(player, (String text) -> {
                closeModal(player, window, () -> onOk.onCall(text.trim()));
            });
        });

        btnOk.setPivot(Pivot.LowerRight);
        btnOk.setPosition(TEXT_INPUT_DIALOG_WIDTH - BUTTON_OFFSET_X, TEXT_INPUT_FOOTER_Y, false);
        styleFooterButton(btnOk);
        window.addChild(btnOk);

        AdvancedButton btnCancel = AdvancedButtonFactory.cancel(t.get("tc.ui.btn.cancel", player), event -> {
            closeModal(player, window, () -> onCancel.onCall(player));
        });

        btnCancel.setPivot(Pivot.LowerLeft);
        btnCancel.setPosition(BUTTON_OFFSET_X, TEXT_INPUT_FOOTER_Y, false);
        styleFooterButton(btnCancel);
        window.addChild(btnCancel);

        return window;
    }

    /** One-step configuration dialog for a player rental offer. */
    public static UIElement getRentalOfferForm(Player player, String title, String priceLabel, String rentLabel,
            String purchaseLabel, Callback<RentalOfferInput> onOk, Callback<Player> onCancel) {
        int width = TEXT_INPUT_DIALOG_WIDTH;
        UIElement window = getDialogWindow(width, 330);
        addTitle(window, title, TEXT_INPUT_TITLE_Y);
        UIElement body = addBody(window, TEXT_INPUT_BODY_Y, TEXT_INPUT_BODY_WIDTH, 196);
        UITextField price = input(body, priceLabel, 12, "0");
        UITextField rent = input(body, rentLabel, 76, "0");
        UILabel purchase = new UILabel(purchaseLabel);
        purchase.setPivot(Pivot.UpperLeft); purchase.setPosition(16, 146, false); purchase.setFontSize(16);
        body.addChild(purchase);
        final boolean[] rentToOwn = { true };
        SwitchButton enabled = new SwitchButton(true, value -> rentToOwn[0] = Boolean.TRUE.equals(value));
        enabled.setPivot(Pivot.UpperLeft); enabled.setPosition(300, 144, false); enabled.setSize(72, 28, false);
        body.addChild(enabled);
        AdvancedButton confirm = AdvancedButtonFactory.ok(t.get("tc.ui.btn.confirm", player), event ->
                price.getCurrentText(player, priceValue -> rent.getCurrentText(player, rentValue -> {
                    closeModal(player, window,
                            () -> onOk.onCall(new RentalOfferInput(priceValue.trim(), rentValue.trim(), rentToOwn[0])));
                })));
        confirm.setPivot(Pivot.LowerRight); confirm.setPosition(width - BUTTON_OFFSET_X, 316, false);
        styleFooterButton(confirm); window.addChild(confirm);
        AdvancedButton cancel = AdvancedButtonFactory.cancel(t.get("tc.ui.btn.cancel", player), event -> {
            closeModal(player, window, () -> onCancel.onCall(player));
        });
        cancel.setPivot(Pivot.LowerLeft); cancel.setPosition(BUTTON_OFFSET_X, 316, false);
        styleFooterButton(cancel); window.addChild(cancel);
        return window;
    }

    private static UITextField input(UIElement body, String label, int y, String value) {
        UILabel caption = new UILabel(label); caption.setPivot(Pivot.UpperLeft); caption.setPosition(16, y, false);
        caption.setFontSize(15); body.addChild(caption);
        UITextField field = new UITextField(); field.setText(value); field.setPivot(Pivot.UpperLeft);
        field.setPosition(200, y - 6, false); field.setSize(190, 38, false); body.addChild(field);
        return field;
    }
}
