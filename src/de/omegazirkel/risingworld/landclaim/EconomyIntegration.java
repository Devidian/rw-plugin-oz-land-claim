package de.omegazirkel.risingworld.landclaim;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.bridge.WalletBridge;
import de.omegazirkel.risingworld.tools.bridge.MailBridge;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;
import java.util.List;

public class EconomyIntegration {
    private final Plugin owner;
    private final WalletBridge walletBridge;
    private final MailBridge mailBridge;

    public EconomyIntegration(Plugin owner) {
        this.owner = owner;
        this.walletBridge = new WalletBridge(owner);
        this.mailBridge = new MailBridge(owner);
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
        return isPluginAvailable("OZ - Shop", "de.omegazirkel.risingworld.Shop");
    }

    public void logStatus() {
        LandClaim.logger().info("LandClaim economy integrations: Wallet="
                + (isWalletAvailable() ? "available" : "missing")
                + ", Shop=" + (isShopAvailable() ? "available" : "missing"));
    }

    public void registerExtraClaimOffer(PluginSettings settings) {
        if (!settings.enableExtraClaimShopOffer || !isShopAvailable()) {
            return;
        }
        Plugin shopPlugin = owner.getPluginByName("OZ - Shop");
        if (shopPlugin == null) {
            return;
        }
        try {
            Class<?> callbackType = Class.forName("de.omegazirkel.risingworld.shop.ShopPurchaseCallback");
            Class<?> priceResolverType = Class.forName("de.omegazirkel.risingworld.shop.ShopPriceResolver");
            Class<?> localizationType = Class.forName("de.omegazirkel.risingworld.shop.ShopOfferLocalization");
            Object callback = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[] { callbackType },
                    extraClaimCallback());
            Object priceResolver = Proxy.newProxyInstance(priceResolverType.getClassLoader(),
                    new Class<?>[] { priceResolverType }, extraClaimPriceResolver(settings));
            Object localization = Proxy.newProxyInstance(localizationType.getClassLoader(),
                    new Class<?>[] { localizationType }, extraClaimLocalization());
            Method registerOffer = shopPlugin.getClass().getMethod("registerOffer",
                    String.class, String.class, String.class, long.class, String.class, String.class, String.class,
                    callbackType, priceResolverType, localizationType);
            Object result = registerOffer.invoke(shopPlugin,
                    "ozlandclaim.extra-claim",
                    "Extra claim capacity",
                    "Adds one extra LandClaim claim capacity.",
                    Math.max(0, settings.extraClaimBasePrice),
                    settings.extraClaimShopCurrencyIdentifier,
                    "zone-visibility-owned-on",
                    "OZ - Land Claim",
                    callback,
                    priceResolver,
                    localization);
            if (!resultSuccess(result)) {
                LandClaim.logger().warn("Could not register LandClaim extra-claim Shop offer: " + resultMessage(result));
            }
        } catch (ReflectiveOperationException ex) {
            LandClaim.logger().warn("Could not register LandClaim extra-claim Shop offer: " + ex.getMessage());
        }
    }

    private InvocationHandler extraClaimLocalization() {
        I18n translations = I18n.getInstance(owner);
        return (proxy, method, args) -> {
            Player player = args != null && args.length > 0 && args[0] instanceof Player p ? p : null;
            return switch (method.getName()) {
                case "title" -> translations.get("landclaim.shop.extra.claim.title", player);
                case "description" -> translations.get("landclaim.shop.extra.claim.desc", player);
                default -> objectMethodValue(proxy, method);
            };
        };
    }

    public WalletOperationResult withdrawDefault(int playerDbId, long value, String reason) {
        return invokeWalletTransaction("withdrawDefault", playerDbId, value, reason);
    }

    public WalletOperationResult depositDefault(int playerDbId, long value, String reason) {
        return invokeWalletTransaction("depositDefault", playerDbId, value, reason);
    }

    private WalletOperationResult invokeWalletTransaction(String methodName, int playerDbId, long value, String reason) {
        Plugin walletPlugin = owner.getPluginByName("OZ - Wallet");
        if (walletPlugin == null) {
            return new WalletOperationResult(false, "Wallet is not available.");
        }
        try {
            Method method = walletPlugin.getClass().getMethod(methodName, int.class, long.class, String.class,
                    String.class);
            Object result = method.invoke(walletPlugin, playerDbId, value, reason, LandClaim.name);
            return new WalletOperationResult(resultSuccess(result), resultMessage(result));
        } catch (ReflectiveOperationException ex) {
            LandClaim.logger().warn("Could not call Wallet " + methodName + ": " + ex.getMessage());
            return new WalletOperationResult(false, "Wallet transaction failed.");
        }
    }

    private InvocationHandler extraClaimPriceResolver(PluginSettings settings) {
        return (Object proxy, Method method, Object[] args) -> {
            if (!"price".equals(method.getName())) {
                return objectMethodValue(proxy, method);
            }
            Player player = args != null && args.length > 0 && args[0] instanceof Player ? (Player) args[0] : null;
            int purchased = LandClaim.extraClaimCapacityService() == null ? 0
                    : LandClaim.extraClaimCapacityService().getPurchasedCapacity(player);
            double multiplier = 1d + purchased * (Math.max(0, settings.extraClaimPriceIncreasePercent) / 100d);
            return Math.max(0L, Math.round(Math.max(0, settings.extraClaimBasePrice) * multiplier));
        };
    }

    private InvocationHandler extraClaimCallback() {
        return (Object proxy, Method method, Object[] args) -> {
            if (!"complete".equals(method.getName())) {
                return objectMethodValue(proxy, method);
            }
            Player player = args != null && args.length > 0 && args[0] instanceof Player ? (Player) args[0] : null;
            Object offer = args != null && args.length > 1 ? args[1] : null;
            if (player == null || LandClaim.extraClaimCapacityService() == null) {
                return shopResult(false, "Extra claim purchase has no player or persistence service.", offer);
            }
            int total = LandClaim.extraClaimCapacityService().addPurchasedCapacity(player, 1);
            String message = I18n.getInstance(owner).get("landclaim.shop.extra.claim.success", player)
                    .replace("PH_TOTAL", String.valueOf(total));
            return shopResult(true, message, offer);
        };
    }

    private Object shopResult(boolean success, String message, Object offer) {
        try {
            Class<?> resultType = Class.forName("de.omegazirkel.risingworld.shop.ShopPurchaseResult");
            if (success) {
                Class<?> offerType = Class.forName("de.omegazirkel.risingworld.shop.ShopOffer");
                return resultType.getMethod("success", String.class, offerType).invoke(null, message, offer);
            }
            Class<?> errorType = Class.forName("de.omegazirkel.risingworld.shop.ShopErrorCode");
            Object errorCode = Enum.valueOf(errorType.asSubclass(Enum.class), "CALLBACK_FAILED");
            return resultType.getMethod("failure", errorType, String.class).invoke(null, errorCode, message);
        } catch (ReflectiveOperationException ex) {
            LandClaim.logger().warn("Could not create ShopPurchaseResult: " + ex.getMessage());
            return null;
        }
    }

    private boolean resultSuccess(Object result) {
        try {
            return result != null && Boolean.TRUE.equals(result.getClass().getField("success").get(result));
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private String resultMessage(Object result) {
        try {
            Object message = result == null ? null : result.getClass().getField("message").get(result);
            return message instanceof String ? (String) message : "";
        } catch (ReflectiveOperationException ex) {
            return "";
        }
    }

    private boolean isPluginAvailable(String pluginName, String className) {
        try {
            return owner.getPluginByName(pluginName) != null && Class.forName(className) != null;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private Object objectMethodValue(Object proxy, Method method) {
        return switch (method.getName()) {
            case "toString" -> "LandClaimShopProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> false;
            default -> null;
        };
    }

    public record WalletOperationResult(boolean success, String message) {
    }
}
