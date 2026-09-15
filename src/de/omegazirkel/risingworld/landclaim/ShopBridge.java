package de.omegazirkel.risingworld.landclaim;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;

/** Optional, reflection-only bridge to OZ - Shop. */
final class ShopBridge {
    private static final String SHOP_PLUGIN = "OZ - Shop";
    private final Plugin owner;

    ShopBridge(Plugin owner) {
        this.owner = owner;
    }

    boolean isAvailable() {
        return shop() != null;
    }

    double systemOfferBaseUnitPrice(String itemName, int variant) {
        if (itemName == null || itemName.isBlank()) return 0.0d;
        Object value = call("systemOfferBaseUnitPrice", new Class<?>[] { String.class, int.class }, itemName,
                Math.max(0, variant));
        return value instanceof Number number ? Math.max(0.0d, number.doubleValue()) : 0.0d;
    }

    void registerExtraClaimOffer(PluginSettings settings) {
        if (settings == null || !settings.enableExtraClaimShopOffer || !isAvailable()) return;
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
            Object result = call("registerOffer", new Class<?>[] { String.class, String.class, String.class,
                    long.class, String.class, String.class, String.class, callbackType, priceResolverType,
                    localizationType }, "ozlandclaim.extra-claim", "Extra claim capacity",
                    "Adds one extra LandClaim claim capacity.", Math.max(0, settings.extraClaimBasePrice),
                    settings.extraClaimShopCurrencyIdentifier, "zone-visibility-owned-on", LandClaim.name, callback,
                    priceResolver, localization);
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

    private InvocationHandler extraClaimPriceResolver(PluginSettings settings) {
        return (proxy, method, args) -> {
            if (!"price".equals(method.getName())) return objectMethodValue(proxy, method);
            Player player = args != null && args.length > 0 && args[0] instanceof Player p ? p : null;
            int purchased = LandClaim.extraClaimCapacityService() == null ? 0
                    : LandClaim.extraClaimCapacityService().getPurchasedCapacity(player);
            double multiplier = 1d + purchased * (Math.max(0, settings.extraClaimPriceIncreasePercent) / 100d);
            return Math.max(0L, Math.round(Math.max(0, settings.extraClaimBasePrice) * multiplier));
        };
    }

    private InvocationHandler extraClaimCallback() {
        return (proxy, method, args) -> {
            if (!"complete".equals(method.getName())) return objectMethodValue(proxy, method);
            Player player = args != null && args.length > 0 && args[0] instanceof Player p ? p : null;
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

    private Object call(String method, Class<?>[] types, Object... values) {
        Plugin shop = shop();
        if (shop == null) return null;
        try {
            return shop.getClass().getMethod(method, types).invoke(shop, values);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private Plugin shop() {
        return owner == null ? null : owner.getPluginByName(SHOP_PLUGIN);
    }

    private static boolean resultSuccess(Object result) {
        try {
            return result != null && Boolean.TRUE.equals(result.getClass().getField("success").get(result));
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private static String resultMessage(Object result) {
        try {
            Object message = result == null ? null : result.getClass().getField("message").get(result);
            return message instanceof String text ? text : "";
        } catch (ReflectiveOperationException ex) {
            return "";
        }
    }

    private static Object objectMethodValue(Object proxy, Method method) {
        return switch (method.getName()) {
            case "toString" -> "LandClaimShopProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> false;
            default -> null;
        };
    }
}
