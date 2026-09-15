package de.omegazirkel.risingworld.landclaim;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.bridge.WalletBridge;
import de.omegazirkel.risingworld.tools.bridge.MailBridge;
import net.risingworld.api.Plugin;
import java.util.List;

public class EconomyIntegration {
    private final WalletBridge walletBridge;
    private final MailBridge mailBridge;
    private final ShopBridge shopBridge;

    public EconomyIntegration(Plugin owner) {
        this.walletBridge = new WalletBridge(owner);
        this.mailBridge = new MailBridge(owner);
        this.shopBridge = new ShopBridge(owner);
    }

    public boolean isWalletAvailable() {
        return walletBridge.isAvailable();
    }

    public boolean hasSystemAccountApi() {
        return walletBridge.hasSystemAccountApi();
    }

    public WalletOperationResult transferPlayerToWorld(int playerDbId, long value, String reason,
            String correlationId) {
        String currency = defaultCurrencyIdentifier();
        if (currency.isBlank()) return new WalletOperationResult(false, "Wallet default currency is unavailable.");
        WalletBridge.WalletTransferCallResult result = walletBridge.transferPlayerToWorldIdempotent(
                playerDbId, value, reason, currency, LandClaim.name, correlationId);
        return new WalletOperationResult(result.success(), result.message());
    }

    public WalletOperationResult reverseTransfer(String originalCorrelationId, String reversalCorrelationId,
            String reason) {
        WalletBridge.WalletTransferCallResult result = walletBridge.reverseAccountTransferIdempotent(
                originalCorrelationId, reversalCorrelationId, reason, LandClaim.name);
        return new WalletOperationResult(result.success(), result.message());
    }

    public WalletOperationResult createCityAccount(long areaId, String cityName) {
        WalletBridge.SystemAccountCallResult result = walletBridge.createSystemAccount(cityAccountId(areaId),
                "CITY", cityName, LandClaim.name);
        return new WalletOperationResult(result.success(), result.message());
    }

    public WalletOperationResult renameCityAccount(long areaId, String cityName) {
        WalletBridge.SystemAccountCallResult result = walletBridge.updateSystemAccountDisplayName(
                cityAccountId(areaId), cityName, LandClaim.name);
        return new WalletOperationResult(result.success(), result.message());
    }

    public WalletOperationResult transferPlayerToCity(int playerDbId, long cityAreaId, long value, String reason,
            String correlationId) {
        String currency = defaultCurrencyIdentifier();
        if (currency.isBlank()) return new WalletOperationResult(false, "Wallet default currency is unavailable.");
        WalletBridge.WalletTransferCallResult result = walletBridge.transferPlayerToSystemIdempotent(
                playerDbId, cityAccountId(cityAreaId), value, reason, currency, LandClaim.name, correlationId);
        return new WalletOperationResult(result.success(), result.message());
    }

    public WalletOperationResult transferCityToWorld(long cityAreaId, long value, String reason,
            String correlationId) {
        String currency = defaultCurrencyIdentifier();
        if (currency.isBlank()) return new WalletOperationResult(false, "Wallet default currency is unavailable.");
        WalletBridge.WalletTransferCallResult result = walletBridge.transferSystemToSystemIdempotent(
                cityAccountId(cityAreaId), walletBridge.worldSystemAccountId(), value, reason, currency, LandClaim.name,
                correlationId);
        return new WalletOperationResult(result.success(), result.message());
    }

    public long cityBalance(long cityAreaId) {
        String currency = defaultCurrencyIdentifier();
        return walletBridge.systemAccountBalances(cityAccountId(cityAreaId)).stream()
                .filter(balance -> balance.currencyIdentifier().equalsIgnoreCase(currency))
                .mapToLong(WalletBridge.SystemBalanceInfo::balance).findFirst().orElse(0L);
    }

    public List<WalletBridge.SystemBalanceInfo> cityBalances(long cityAreaId) {
        return walletBridge.systemAccountBalances(cityAccountId(cityAreaId));
    }

    public String defaultCurrencyIdentifier() {
        return walletBridge.defaultCurrencyIdentifier();
    }

    /** Wallet-owned language for durable system-account audit entries. */
    public String walletAuditLanguage() {
        return walletBridge.walletAuditLanguage();
    }

    public String cityAccountId(long areaId) {
        return "city::area-" + areaId;
    }

    public long playerBalance(int playerDbId) {
        return walletBridge.balanceDefault(playerDbId);
    }

    public boolean sendMail(int playerDbId, String playerName, String subject, String body, String correlationId) {
        return mailBridge.sendTextMail(new MailBridge.PluginMailRequest(LandClaim.name, playerDbId, playerName,
                subject, body, correlationId)).success();
    }

    public boolean canReceiveMail(int playerDbId) { return mailBridge.canReceiveMail(playerDbId); }

    public MailBridge.BridgeResult sendAttachments(int playerDbId, String playerName, String subject, String body,
            String correlationId, List<MailBridge.PluginAttachment> attachments) {
        return mailBridge.sendAttachmentMail(new MailBridge.PluginAttachmentMailRequest(LandClaim.name, playerDbId,
                playerName, subject, body, correlationId, attachments));
    }

    public WalletOperationResult closeCityAccount(long cityAreaId, String reason, String correlationPrefix) {
        String cityAccount = cityAccountId(cityAreaId);
        String worldAccount = walletBridge.worldSystemAccountId();
        for (WalletBridge.SystemBalanceInfo balance : walletBridge.systemAccountBalances(cityAccount)) {
            if (balance.balance() <= 0) continue;
            WalletBridge.WalletTransferCallResult transfer = walletBridge.transferSystemToSystemIdempotent(
                    cityAccount, worldAccount, balance.balance(), reason, balance.currencyIdentifier(),
                    LandClaim.name, correlationPrefix + ":" + balance.currencyIdentifier());
            if (!transfer.success()) return new WalletOperationResult(false, transfer.message());
        }
        WalletBridge.SystemAccountCallResult archive = walletBridge.archiveSystemAccount(cityAccount, LandClaim.name);
        return new WalletOperationResult(archive.success(), archive.message());
    }

    public boolean isShopAvailable() {
        return shopBridge.isAvailable();
    }

    public void logStatus() {
        LandClaim.logger().info("LandClaim economy integrations: Wallet="
                + (isWalletAvailable() ? "available" : "missing")
                + ", Shop=" + (isShopAvailable() ? "available" : "missing"));
    }

    public void registerExtraClaimOffer(PluginSettings settings) {
        shopBridge.registerExtraClaimOffer(settings);
    }

    public WalletOperationResult withdrawDefault(int playerDbId, long value, String reason) {
        WalletBridge.WalletCallResult result = walletBridge.withdrawDefault(playerDbId, value, reason, LandClaim.name);
        return new WalletOperationResult(result.success(), result.message());
    }

    public WalletOperationResult depositDefault(int playerDbId, long value, String reason) {
        WalletBridge.WalletCallResult result = walletBridge.depositDefault(playerDbId, value, reason, LandClaim.name);
        return new WalletOperationResult(result.success(), result.message());
    }

    public double systemOfferBaseUnitPrice(String itemName, int variant) {
        return shopBridge.systemOfferBaseUnitPrice(itemName, variant);
    }

    public record WalletOperationResult(boolean success, String message) {
    }
}
