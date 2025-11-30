package com.example.pandatribe.constants;

/**
 * Constants for EVE Online game mechanics and IDs.
 * Centralizes magic numbers and game-specific values.
 */
public final class EveConstants {

    private EveConstants() {
        // Prevent instantiation
    }

    // ========== Activity IDs ==========
    /**
     * Manufacturing activity ID
     */
    public static final Integer MANUFACTURING_ACTIVITY_ID = 1;

    /**
     * Reaction activity ID
     */
    public static final Integer REACTION_ACTIVITY_ID = 11;

    /**
     * Special activity ID for fuel blocks
     */
    public static final Integer FUEL_BLOCK_ACTIVITY_ID = 105;

    // ========== Group IDs ==========
    /**
     * Ship group ID - used to determine if we should use render or icon
     */
    public static final Integer SHIP_GROUP_ID = 541;

    // ========== Image Sizes ==========
    /**
     * Small icon size (32x32)
     */
    public static final Integer ICON_SIZE_SMALL = 32;

    /**
     * Large icon size (64x64)
     */
    public static final Integer ICON_SIZE_LARGE = 64;

    /**
     * Huge icon size (256x256) - used for initial display
     */
    public static final Integer ICON_SIZE_HUGE = 256;

    // ========== Tier Values ==========
    /**
     * Special tier value for fuel blocks
     */
    public static final Integer FUEL_BLOCK_TIER = 105;

    // ========== Order Types ==========
    /**
     * All market orders
     */
    public static final String ORDER_TYPE_ALL = "all";

    /**
     * Buy orders only
     */
    public static final String ORDER_TYPE_BUY = "buy";

    /**
     * Sell orders only
     */
    public static final String ORDER_TYPE_SELL = "sell";

    // ========== Activity Names ==========
    /**
     * Manufacturing activity name
     */
    public static final String MANUFACTURING = "manufacturing";

    /**
     * Reaction activity name
     */
    public static final String REACTION = "reaction";
}
