package com.example.pandatribe.exceptions;

import lombok.Getter;

/**
 * Exception thrown when market data is unavailable or cannot be retrieved from the EVE API.
 * This can occur due to API downtime, network issues, or when market data doesn't exist for a specific item/region.
 */
@Getter
public class MarketDataUnavailableException extends RuntimeException {

    private final Integer typeId;
    private final Integer regionId;
    private final String orderType;

    /**
     * Creates a new MarketDataUnavailableException with a general message.
     *
     * @param message The error message
     */
    public MarketDataUnavailableException(String message) {
        super(message);
        this.typeId = null;
        this.regionId = null;
        this.orderType = null;
    }

    /**
     * Creates a new MarketDataUnavailableException for a specific item and region.
     *
     * @param typeId The EVE type ID
     * @param regionId The EVE region ID
     */
    public MarketDataUnavailableException(Integer typeId, Integer regionId) {
        super(String.format("Market data unavailable for type ID %d in region %d", typeId, regionId));
        this.typeId = typeId;
        this.regionId = regionId;
        this.orderType = null;
    }

    /**
     * Creates a new MarketDataUnavailableException with full context.
     *
     * @param typeId The EVE type ID
     * @param regionId The EVE region ID
     * @param orderType The order type (buy/sell/all)
     */
    public MarketDataUnavailableException(Integer typeId, Integer regionId, String orderType) {
        super(String.format("Market data unavailable for type ID %d in region %d (order type: %s)",
            typeId, regionId, orderType));
        this.typeId = typeId;
        this.regionId = regionId;
        this.orderType = orderType;
    }

    /**
     * Creates a new MarketDataUnavailableException with a cause.
     *
     * @param message The error message
     * @param cause The underlying cause
     */
    public MarketDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.typeId = null;
        this.regionId = null;
        this.orderType = null;
    }
}
