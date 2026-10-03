package de.omegazirkel.risingworld.landclaim;

/**
 * Area permission flags a player may edit on a custom permission group.
 * The editor UI and the group file IO only use this list.
 */
public enum CustomPermissionFlag {
    // general
    PVE("general", "pve", "tc.ui.custom.flag.pve"),
    PVP("general", "pvp", "tc.ui.custom.flag.pvp"),
    KEEP_INVENTORY("general", "keepinventory", "tc.ui.custom.flag.keepinventory"),
    RIDE_MOUNTS("general", "ridemounts", "tc.ui.custom.flag.ridemounts"),
    UNLOCK_ALL_DOORS("general", "unlockalldoors", "tc.ui.custom.flag.unlockalldoors"),
    USE_ALL_FURNACES("general", "useallfurnaces", "tc.ui.custom.flag.useallfurnaces"),
    USE_ALL_DOORS("general", "usealldoors", "tc.ui.custom.flag.usealldoors"),
    USE_ALL_CHESTS("general", "useallchests", "tc.ui.custom.flag.useallchests"),
    USE_ALL_LIGHTS("general", "usealllights", "tc.ui.custom.flag.usealllights"),
    USE_ALL_OBJECTS("general", "useallobjects", "tc.ui.custom.flag.useallobjects"),
    USE_ALL_VEHICLES("general", "useallvehicles", "tc.ui.custom.flag.useallvehicles"),

    // world (boolean keys only; placeobjectsexception is not editable here)
    DESTROY_CONSTRUCTIONS("world", "destroyconstructions", "tc.ui.custom.flag.destroyconstructions"),
    DESTROY_OBJECTS("world", "destroyobjects", "tc.ui.custom.flag.destroyobjects"),
    DESTROY_OWN_CONSTRUCTIONS("world", "destroyownconstructions", "tc.ui.custom.flag.destroyownconstructions"),
    DESTROY_OWN_OBJECTS("world", "destroyownobjects", "tc.ui.custom.flag.destroyownobjects"),
    DESTROY_OWN_VEGETATIONS("world", "destroyownvegetations", "tc.ui.custom.flag.destroyownvegetations"),
    DESTROY_TERRAIN("world", "destroyterrain", "tc.ui.custom.flag.destroyterrain"),
    DESTROY_VEGETATIONS("world", "destroyvegetations", "tc.ui.custom.flag.destroyvegetations"),
    EDIT_CONSTRUCTIONS("world", "editconstructions", "tc.ui.custom.flag.editconstructions"),
    EDIT_OBJECTS("world", "editobjects", "tc.ui.custom.flag.editobjects"),
    EDIT_OWN_CONSTRUCTIONS("world", "editownconstructions", "tc.ui.custom.flag.editownconstructions"),
    EDIT_OWN_OBJECTS("world", "editownobjects", "tc.ui.custom.flag.editownobjects"),
    EDIT_OWN_VEGETATIONS("world", "editownvegetations", "tc.ui.custom.flag.editownvegetations"),
    EDIT_VEGETATIONS("world", "editvegetations", "tc.ui.custom.flag.editvegetations"),
    PLACE_CONSTRUCTIONS("world", "placeconstructions", "tc.ui.custom.flag.placeconstructions"),
    PLACE_OBJECTS("world", "placeobjects", "tc.ui.custom.flag.placeobjects"),
    PLACE_TERRAIN("world", "placeterrain", "tc.ui.custom.flag.placeterrain"),
    PLACE_VEGETATIONS("world", "placevegetations", "tc.ui.custom.flag.placevegetations");

    /** Top-level object in the permission file, e.g. "general" or "world". */
    public final String section;
    public final String key;
    public final String i18nKey;

    CustomPermissionFlag(String section, String key, String i18nKey) {
        this.section = section;
        this.key = key;
        this.i18nKey = i18nKey;
    }
}
