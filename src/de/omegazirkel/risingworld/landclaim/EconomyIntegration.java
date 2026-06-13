package de.omegazirkel.risingworld.landclaim;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.omegazirkel.risingworld.LandClaim;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;

public class EconomyIntegration {
    private final Plugin owner;

    public EconomyIntegration(Plugin owner) {
        this.owner = owner;
    }

    public boolean isWalletAvailable() {
        return isPluginAvailable("OZ - Wallet", "de.omegazirkel.risingworld.Wallet");
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
            Object callback = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[] { callbackType },
                    extraClaimCallback());
            Object priceResolver = Proxy.newProxyInstance(priceResolverType.getClassLoader(),
                    new Class<?>[] { priceResolverType }, extraClaimPriceResolver(settings));
            Method registerOffer = shopPlugin.getClass().getMethod("registerOffer",
                    String.class, String.class, String.class, long.class, String.class, String.class, String.class,
                    callbackType, priceResolverType);
            Object result = registerOffer.invoke(shopPlugin,
                    "ozlandclaim.extra-claim",
                    "Extra claim capacity",
                    "Adds one extra LandClaim claim capacity.",
                    Math.max(0, settings.extraClaimBasePrice),
                    settings.extraClaimShopCurrencyIdentifier,
                    "icon-ki-owned-on",
                    "OZ - Land Claim",
                    callback,
                    priceResolver);
            if (!resultSuccess(result)) {
                LandClaim.logger().warn("Could not register LandClaim extra-claim Shop offer: " + resultMessage(result));
            }
        } catch (ReflectiveOperationException ex) {
            LandClaim.logger().warn("Could not register LandClaim extra-claim Shop offer: " + ex.getMessage());
        }
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
            return shopResult(true, "Extra claim capacity purchased. Total purchased capacity: " + total + ".", offer);
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
