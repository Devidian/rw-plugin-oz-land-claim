package de.omegazirkel.risingworld.landclaim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.bridge.MailBridge;
import net.risingworld.api.World;
import net.risingworld.api.definitions.Crafting.Recipe;
import net.risingworld.api.definitions.Crafting.Recipe.Ingredient;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.definitions.Objects.ObjectDefinition;
import net.risingworld.api.definitions.Constructions.ConstructionDefinition;
import net.risingworld.api.definitions.Items.ItemDefinition;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.MetaObject;
import net.risingworld.api.objects.Item;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.Storage;
import net.risingworld.api.objects.WorldItem;
import net.risingworld.api.objects.world.Chunk;
import net.risingworld.api.objects.world.ConstructionElement;
import net.risingworld.api.objects.world.ObjectElement;
import net.risingworld.api.utils.Vector3i;

/** Reads and clears placed elements whose placement anchor is inside a claim area. */
public final class PropertyClearanceService {
    private static final int MAX_CHUNK_COLUMNS = 4_096;
    private static final int MAX_ELEMENTS = 200_000;

    private final PluginSettings settings;
    private final EconomyIntegration economy;

    public PropertyClearanceService(PluginSettings settings, EconomyIntegration economy) {
        this.settings = settings;
        this.economy = economy;
    }

    public Preview preview(Area area, String language) {
        if (area == null || !area.isValid()) return Preview.unavailable("AREA_UNAVAILABLE");
        Vector3i start = area.getStartChunkPosition();
        Vector3i end = area.getEndChunkPosition();
        if (start == null || end == null) return Preview.unavailable("AREA_BOUNDS_UNAVAILABLE");
        int minX = Math.min(start.x, end.x), maxX = Math.max(start.x, end.x);
        int minZ = Math.min(start.z, end.z), maxZ = Math.max(start.z, end.z);
        long columns = (long) (maxX - minX + 1) * (long) (maxZ - minZ + 1);
        if (columns > MAX_CHUNK_COLUMNS) return Preview.unavailable("AREA_TOO_LARGE");

        List<ElementRef> elements = new ArrayList<>();
        List<UnresolvedElement> unresolved = new ArrayList<>();
        Map<ItemKey, Integer> returned = new LinkedHashMap<>();
        Map<RecipeMaterialKey, Integer> recipeMaterials = new LinkedHashMap<>();
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            Chunk chunk = World.getChunk(x, z);
            if (chunk == null || !chunk.isValid()) return Preview.unavailable("CHUNK_UNAVAILABLE");
            ObjectElement[] objects = chunk.getAllObjects();
            if (objects != null) for (ObjectElement object : objects) {
                if (object == null || !object.isValid() || !area.isPointInArea(object.getWorldPosition())) continue;
                int unresolvedBefore = unresolved.size();
                appendObjectMaterials(object, returned, recipeMaterials, unresolved);
                appendContainerItems(object, returned, unresolved);
                elements.add(ElementRef.object(object, unresolved.size() > unresolvedBefore));
            }
            ConstructionElement[] constructions = chunk.getAllConstructionElements();
            if (constructions != null) for (ConstructionElement construction : constructions) {
                if (construction == null || !construction.isValid() || !area.isPointInArea(construction.getWorldPosition())) continue;
                int unresolvedBefore = unresolved.size();
                appendConstructionMaterials(construction, returned, recipeMaterials, unresolved);
                elements.add(ElementRef.construction(construction, unresolved.size() > unresolvedBefore));
            }
            if (elements.size() > MAX_ELEMENTS) return Preview.unavailable("TOO_MANY_ELEMENTS");
        }
        for (Map.Entry<RecipeMaterialKey, Integer> recipeMaterial : recipeMaterials.entrySet())
            add(returned, recipeMaterial.getKey().item(), recipeMaterial.getKey().returnedAmount(recipeMaterial.getValue()));
        List<ReturnItem> items = returned.entrySet().stream().map(entry -> new ReturnItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(item -> item.key().displayName(language), String.CASE_INSENSITIVE_ORDER)).toList();
        return new Preview(true, "", List.copyOf(elements), items, List.copyOf(unresolved));
    }

    public Result dispose(Player actor, Area area) { return execute(actor, area, Mode.DISPOSE); }
    public Result demolish(Player actor, Area area) { return execute(actor, area, Mode.DEMOLISH); }
    public Result recycle(Player actor, Area area) { return execute(actor, area, Mode.RECYCLE); }
    /** Removes only placed hosts with an unresolved construction material, without a fee or return. */
    public Result removeUnresolved(Player actor, Area area) { return execute(actor, area, Mode.REMOVE_UNRESOLVED); }
    /** Administrator demolition always refunds by mail and never charges a fee. */
    public Result adminDemolish(Player actor, Area area) { return execute(actor, area, Mode.DEMOLISH, true); }
    /** Administrator recycling pays the identified owner, or the administrator when no owner is available. */
    public Result adminRecycle(Player actor, Area area) { return execute(actor, area, Mode.RECYCLE, true); }
    /** Administrator removal of unresolved hosts never charges a fee or returns materials. */
    public Result adminRemoveUnresolved(Player actor, Area area) { return execute(actor, area, Mode.REMOVE_UNRESOLVED, true); }
    public String defaultCurrencyIdentifier() { return economy.defaultCurrencyIdentifier(); }
    public boolean isRecyclingAvailable() { return economy.isShopAvailable() && economy.isWalletAvailable(); }

    private Result execute(Player actor, Area area, Mode mode) { return execute(actor, area, mode, false); }

    private Result execute(Player actor, Area area, Mode mode, boolean administrator) {
        if (actor == null || actor.getDbID() <= 0 || (!administrator && !Boolean.TRUE.equals(settings.enablePropertyClearance))
                || (administrator && !actor.isAdmin()))
            return failed(area, mode, "DISABLED");
        Preview current = preview(area, actor.getLanguage());
        if (!current.available()) return failed(area, mode, current.reason());
        if ((mode == Mode.DEMOLISH || mode == Mode.RECYCLE) && !current.unresolved().isEmpty())
            return failed(area, mode, "UNRESOLVED_MATERIALS");
        List<ElementRef> targets = mode == Mode.REMOVE_UNRESOLVED
                ? current.elements().stream().filter(ElementRef::unresolved).toList() : current.elements();
        if (targets.isEmpty()) return failed(area, mode, "UNRESOLVED_NONE");
        if (mode == Mode.RECYCLE && !isRecyclingAvailable()) return failed(area, mode, "SHOP_UNAVAILABLE");
        int recipientId = ownerDbId(area, null);
        if (!administrator && recipientId <= 0) recipientId = actor.getDbID();
        String recipientName = recipientId == actor.getDbID() ? actor.getName()
                : recipientId <= 0 ? null : net.risingworld.api.Server.getLastKnownPlayerName(recipientId);
        if (mode == Mode.DEMOLISH && administrator && (recipientId <= 0 || recipientName == null || recipientName.isBlank()
                || !economy.canReceiveMail(recipientId))) {
            recipientId = actor.getDbID();
            recipientName = actor.getName();
        }
        if (mode == Mode.RECYCLE && administrator && recipientId <= 0) {
            recipientId = actor.getDbID();
            recipientName = actor.getName();
        }
        if (mode == Mode.DEMOLISH && (recipientId <= 0 || recipientName == null || recipientName.isBlank()
                || !economy.canReceiveMail(recipientId))) return failed(area, mode, "MAIL_UNAVAILABLE");
        long fee = administrator || mode == Mode.RECYCLE || mode == Mode.REMOVE_UNRESOLVED ? 0L : fee(current.resourceCount(), mode);
        String correlation = "landclaim-clear:" + area.getID() + ":" + mode + ":" + java.util.UUID.randomUUID();
        if (fee > 0) {
            EconomyIntegration.WalletOperationResult payment = economy.transferPlayerToWorld(actor.getDbID(), fee,
                    "LandClaim property clearance " + mode + " area " + area.getID(), correlation + ":fee");
            if (!payment.success()) return failed(area, mode, "PAYMENT_FAILED:" + payment.message());
        }
        if (mode == Mode.DEMOLISH) {
            List<MailBridge.PluginAttachment> attachments = current.items().stream().map(item -> item.key().attachment(item.amount())).toList();
            MailBridge.BridgeResult delivery = economy.sendAttachments(recipientId, recipientName,
                    I18n.getInstance(LandClaim.name).get("tc.property.clearance.mail.subject", actor),
                    I18n.getInstance(LandClaim.name).get("tc.property.clearance.mail.body", actor)
                            .replace("PH_AREA_NAME", areaName(area)), correlation + ":mail", attachments);
            if (!delivery.success()) {
                if (fee > 0) economy.reverseTransfer(correlation + ":fee", correlation + ":refund", "Refund failed property dismantling mail");
                return failed(area, mode, "MAIL_DELIVERY_FAILED:" + delivery.code());
            }
        }
        long recycledValue = mode == Mode.RECYCLE ? recycleValue(current) : 0L;
        if (mode == Mode.RECYCLE && recycledValue > 0) {
            EconomyIntegration.WalletOperationResult payout = economy.depositDefault(recipientId, recycledValue,
                    "LandClaim property recycling area " + area.getID());
            if (!payout.success()) return failed(area, mode, "PAYMENT_FAILED:" + payout.message());
        }
        // Once all demolition materials are in durable mail custody, remove
        // container custody before object destruction so the world cannot drop
        // the same content as loose items.
        for (ElementRef element : targets) if (element.object() != null && element.object().isValid())
            emptyContainer(element.object());
        int removed = 0;
        for (ElementRef element : targets) {
            if (element.object() != null && element.object().isValid()) { element.object().destroy(true); removed++; }
            if (element.construction() != null && element.construction().isValid()) { element.construction().destroy(true); removed++; }
        }
        LandClaim.logger().info("Property clearance " + mode + " by " + actor.getDbID() + " in area " + area.getID()
                + ": " + removed + " elements, fee " + fee + ", correlation " + correlation);
        return new Result(true, "", removed, fee, recycledValue);
    }

    private String areaName(Area area) {
        return area == null || area.getName() == null || area.getName().isBlank() ? "#" + (area == null ? "?" : area.getID()) : area.getName();
    }

    private void emptyContainer(ObjectElement object) {
        Set<Long> seenStorages = new HashSet<>();
        Set<Long> seenMetaObjects = new HashSet<>();
        for (long reference : new long[] { object.getGlobalID(), object.getInfo() }) {
            if (reference <= 0) continue;
            Storage storage = World.getStorage(reference);
            if (storage != null && seenStorages.add(storage.getID())) storage.clear();
            MetaObject meta = World.getMetaObject(reference);
            if (meta != null && meta.isValid() && seenMetaObjects.add(meta.getID())) {
                meta.setItems(new WorldItem[0]);
                meta.triggerUpdate();
            }
        }
    }

    private long fee(long resources, Mode mode) {
        double rate = mode == Mode.DISPOSE ? Math.max(0d, settings.propertyClearanceDisposeFeePerItem)
                : Math.max(0d, settings.propertyClearanceDemolishFeePerItem);
        return (long) Math.min(Long.MAX_VALUE, Math.ceil(resources * rate));
    }

    public long recycleValue(Preview preview) {
        if (preview == null || !preview.unresolved().isEmpty()) return 0L;
        double total = 0.0d;
        for (ReturnItem item : preview.items())
            total += economy.systemOfferBaseUnitPrice(item.key().name(), item.key().variant()) * item.amount();
        double rate = Math.min(1.0d, Math.max(0.0d, settings.propertyClearanceRecycleRate));
        return (long) Math.min(Long.MAX_VALUE, Math.floor(total * rate));
    }

    private Result failed(Area area, Mode mode, String reason) {
        LandClaim.logger().warn("Property clearance " + mode + " for area "
                + (area == null ? "?" : area.getID()) + " failed: " + reason);
        return Result.failed(reason);
    }

    private int ownerDbId(Area area, Player fallback) {
        if (area != null && area.getAllPlayerPermissions() != null) for (Map.Entry<Integer, String> entry : area.getAllPlayerPermissions().entrySet())
            if (settings.ownerAreaPermission.equals(entry.getValue()) && entry.getKey() != null && entry.getKey() > 0) return entry.getKey();
        return fallback == null ? 0 : fallback.getDbID();
    }

    private void appendObjectMaterials(ObjectElement object, Map<ItemKey, Integer> returned,
            Map<RecipeMaterialKey, Integer> recipeMaterials, List<UnresolvedElement> unresolved) {
        ObjectDefinition definition = object.getDefinition();
        // ObjectElement#getVariant is not implemented by the native server at
        // the moment. Object definitions still identify their recipe safely.
        Recipe recipe = definition == null ? null : findRecipe(definition.relateditem, definition.name, -1);
        appendRecipe(recipe, recipeMaterials, unresolved, UnresolvedElement.object(
                definition == null ? "object:" + object.getTypeID() : definition.name, object.getTypeID()));
    }

    private void appendConstructionMaterials(ConstructionElement construction, Map<ItemKey, Integer> returned,
            Map<RecipeMaterialKey, Integer> recipeMaterials, List<UnresolvedElement> unresolved) {
        // Rising World construction texture 100 is the wooden block. Its texture
        // does not identify a particular tree, so any ordinary tree log is an
        // equivalent one-item return by the agreed gameplay rule.
        if (construction.getTexture() == 100) {
            add(returned, new ItemKey("treelog", 0, 0, (short) 0, "", 0), 1);
            return;
        }
        ConstructionDefinition definition = Definitions.getConstructionDefinition(construction.getTypeID());
        Recipe recipe = definition == null ? null : findRecipe(definition.relateditem, definition.name, construction.getTexture());
        appendRecipe(recipe, recipeMaterials, unresolved, UnresolvedElement.construction(
                definition == null ? "construction:" + construction.getTypeID() : definition.name,
                construction.getTypeID(), construction.getTexture()));
    }

    private Recipe findRecipe(String relatedItem, String definitionName, int texture) {
        if (relatedItem == null || relatedItem.isBlank()) return null;
        Recipe fallback = null;
        Recipe[] recipes = Definitions.getAllRecipes();
        if (recipes == null) return null;
        for (Recipe recipe : recipes) {
            if (recipe == null || recipe.itemDef == null || recipe.itemDef.name == null || !recipe.itemDef.name.equalsIgnoreCase(relatedItem)) continue;
            if (fallback == null) fallback = recipe;
            if (texture >= 0 && recipe.texture == texture) return recipe;
            if (recipe.name != null && recipe.name.equalsIgnoreCase(definitionName) && texture < 0) return recipe;
        }
        return fallback;
    }

    private void appendRecipe(Recipe recipe, Map<RecipeMaterialKey, Integer> recipeMaterials,
            List<UnresolvedElement> unresolved, UnresolvedElement source) {
        if (recipe == null || recipe.ingredients == null || recipe.amount <= 0) { unresolved.add(source); return; }
        for (Ingredient ingredient : recipe.ingredients) {
            if (ingredient == null || !ingredient.consume) continue;
            String materialName = ingredientMaterialName(ingredient);
            if (materialName == null) {
                unresolved.add(unresolvedIngredient(source, ingredient)); continue;
            }
            RecipeMaterialKey key = new RecipeMaterialKey(new ItemKey(materialName, 0, 0, (short) 0, "", 0),
                    recipe.amount, ingredient.count);
            recipeMaterials.merge(key, 1, Math::addExact);
        }
    }

    private String ingredientMaterialName(Ingredient ingredient) {
        if (ingredient.itemDef != null && ingredient.itemDef.name != null && !ingredient.itemDef.name.isBlank())
            return ingredient.itemDef.name;
        if (ingredient.group == null || "None".equals(ingredient.group.name())) return null;
        // Cloth is an internal anchor-only group on the dedicated server. It
        // cannot be restored as an inventory item, so do not promise it in mail.
        if ("Cloth".equals(ingredient.group.name())) return null;
        // Group ingredients deliberately permit any member. Prefer the canonical
        // item named after its group (e.g. Glass -> glass), then use a stable
        // fallback member so the returned mail attachment is always concrete.
        String canonical = ingredient.group.name().toLowerCase(Locale.ROOT);
        ItemDefinition exact = Definitions.getItemDefinition(canonical);
        if (exact != null && exact.group == ingredient.group) return exact.name;
        ItemDefinition[] definitions = Definitions.getAllItemDefinitions();
        return definitions == null ? null : Arrays.stream(definitions)
                .filter(item -> item != null && item.group == ingredient.group && item.name != null && !item.name.isBlank())
                .map(item -> item.name).sorted(String.CASE_INSENSITIVE_ORDER).findFirst().orElse(null);
    }

    private UnresolvedElement unresolvedIngredient(UnresolvedElement source, Ingredient ingredient) {
        return ingredient.group != null && "Cloth".equals(ingredient.group.name())
                ? source.withName("item.cloth.name") : source;
    }

    private void appendContainerItems(ObjectElement object, Map<ItemKey, Integer> returned, List<UnresolvedElement> unresolved) {
        // Container implementations use either the world-object ID or its info
        // ID as their storage key. Try both and de-duplicate resolved stores.
        long[] references = { object.getGlobalID(), object.getInfo() };
        Set<Long> seenStorages = new HashSet<>();
        Set<Long> seenMetaObjects = new HashSet<>();
        for (long reference : references) {
            if (reference <= 0) continue;
            Storage storage = World.getStorage(reference);
            if (storage != null && seenStorages.add(storage.getID()) && storage.getItems() != null)
                for (Item item : storage.getItems()) appendStoredItem(item, returned, unresolved);
            MetaObject meta = World.getMetaObject(reference);
            if (meta != null && meta.isValid() && seenMetaObjects.add(meta.getID())) appendMetaItems(meta, returned, unresolved);
        }
    }

    private void appendMetaItems(MetaObject meta, Map<ItemKey, Integer> returned, List<UnresolvedElement> unresolved) {
        if (meta.getItems() == null) return;
        for (WorldItem item : meta.getItems()) {
            if (item == null || !item.isValid() || item.getDefinition() == null || item.getDefinition().name == null) { unresolved.add(UnresolvedElement.containerUnknown()); continue; }
            add(returned, new ItemKey(item.getDefinition().name, item.getVariant(), item.getDurability(), item.getStatus(),
                    item.getModifier() == null ? "" : item.getModifier().name(), 0), item.getStack());
        }
    }

    private void appendStoredItem(Item item, Map<ItemKey, Integer> returned, List<UnresolvedElement> unresolved) {
        if (item == null || !item.isValid() || item.getStack() <= 0) return;
        String name = item instanceof Item.ConstructionItem construction ? construction.getConstructionName()
                : item instanceof Item.ObjectItem object ? object.getObjectName()
                : item instanceof Item.ClothingItem clothing ? clothing.getClothingName()
                : item.getDefinition() == null ? null : item.getDefinition().name;
        if (name == null || name.isBlank()) { unresolved.add(UnresolvedElement.containerUnknown()); return; }
        int color = item instanceof Item.ConstructionItem construction ? construction.getColor() : 0;
        add(returned, new ItemKey(name, item.getVariant(), item.getDurability(), item.getStatus(),
                item.getModifier() == null ? "" : item.getModifier().name(), color), item.getStack());
    }

    private static void add(Map<ItemKey, Integer> items, ItemKey key, int amount) {
        if (amount > 0) items.merge(key, amount, Math::addExact);
    }

    static int returnedRecipeAmount(int placements, int ingredientAmount, int recipeOutputAmount) {
        if (placements <= 0 || ingredientAmount <= 0 || recipeOutputAmount <= 0) return 0;
        long total = Math.multiplyExact((long) placements, ingredientAmount);
        return Math.toIntExact((total + recipeOutputAmount - 1L) / recipeOutputAmount);
    }

    public enum Mode { DISPOSE, DEMOLISH, RECYCLE, REMOVE_UNRESOLVED }
    public record ElementRef(ObjectElement object, ConstructionElement construction, boolean unresolved) {
        static ElementRef object(ObjectElement value, boolean unresolved) { return new ElementRef(value, null, unresolved); }
        static ElementRef construction(ConstructionElement value, boolean unresolved) { return new ElementRef(null, value, unresolved); }
    }
    public record ItemKey(String name, int variant, int durability, short status, String modifier, int color) {
        MailBridge.PluginAttachment attachment(int amount) {
            // The world API reports its unspecified/default variant as -1,
            // whereas Mail's durable attachment contract represents it as 0.
            return new MailBridge.PluginAttachment(name, Math.max(0, variant), amount, durability, status, modifier, color);
        }
        public String displayName(String language) {
            var item = Definitions.getItemDefinition(name);
            if (item != null) return localizedOrFallback(item.getLocalizedName(language));
            var object = Definitions.getObjectDefinition(name);
            if (object != null) return localizedOrFallback(object.getLocalizedName(language));
            var construction = Definitions.getConstructionDefinition(name);
            if (construction != null) return localizedOrFallback(construction.getLocalizedName(language));
            return name == null || name.isBlank() ? "Item" : name;
        }
        private String localizedOrFallback(String value) {
            return value == null || value.isBlank() ? (name == null || name.isBlank() ? "Item" : name) : value;
        }
    }
    public record ReturnItem(ItemKey key, int amount) { }
    /** Aggregates recipe input across equal recipes before rounding their output ratio. */
    private record RecipeMaterialKey(ItemKey item, int recipeOutputAmount, int ingredientAmount) {
        int returnedAmount(int placements) {
            return returnedRecipeAmount(placements, ingredientAmount, recipeOutputAmount);
        }
    }
    /** Material that cannot be reconstructed, retaining the placed source for the player preview. */
    public record UnresolvedElement(String name, String sourceName, int typeId, int blockId, boolean construction, boolean container) {
        static UnresolvedElement object(String sourceName, int typeId) { return new UnresolvedElement("", sourceName, typeId, -1, false, false); }
        static UnresolvedElement construction(String sourceName, int typeId, int blockId) { return new UnresolvedElement("", sourceName, typeId, blockId, true, false); }
        static UnresolvedElement containerUnknown() { return new UnresolvedElement("container", "", -1, -1, false, true); }
        UnresolvedElement withName(String value) { return new UnresolvedElement(value, sourceName, typeId, blockId, construction, container); }
    }
    public record Preview(boolean available, String reason, List<ElementRef> elements, List<ReturnItem> items, List<UnresolvedElement> unresolved) {
        public long resourceCount() {
            long result = 0;
            for (ReturnItem item : items) result = Math.addExact(result, item.amount());
            return result;
        }
        public long unresolvedHostCount() {
            return elements.stream().filter(ElementRef::unresolved).count();
        }
        static Preview unavailable(String reason) { return new Preview(false, reason, List.of(), List.of(), List.of()); }
    }
    public record Result(boolean success, String reason, int removed, long fee, long recycledValue) {
        static Result failed(String reason) { return new Result(false, reason, 0, 0, 0); }
    }
}
