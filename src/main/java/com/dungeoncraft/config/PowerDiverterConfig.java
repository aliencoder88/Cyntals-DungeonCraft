package com.dungeoncraft.config;

/**
 * Compact, packet-friendly configuration for the four-port Power Router.
 *
 * - portModeBits stores two bits per port;
 * - routeBits stores the player's requested destination bits for each source;
 * - feedbackBlockedRouteBits marks requested routes that are held inactive
 *   because the opposite direction was already requested first.
 *
 * A requested route may also be temporarily unusable because of its source or
 * destination port mode. Requested routes are preserved through those mode
 * changes and become effective automatically when both endpoints allow them.
 */
public record PowerDiverterConfig(
        int portModeBits,
        int routeBits,
        int feedbackBlockedRouteBits
) {
    private static final int PORT_COUNT = 4;
    private static final int PORT_MODE_MASK = 0xFF;
    private static final int ROUTE_MASK = 0xFFFF;

    public PowerDiverterConfig {
        portModeBits &= PORT_MODE_MASK;
        routeBits &= ROUTE_MASK;
        feedbackBlockedRouteBits &= ROUTE_MASK;

        // A route never returns immediately to the same physical port.
        for (PowerDiverterPort source : PowerDiverterPort.values()) {
            int selfRouteBit = source.getMask() << routeShift(source);
            routeBits &= ~selfRouteBit;
            feedbackBlockedRouteBits &= ~selfRouteBit;
        }

        // A blocked marker is meaningful only while that route is requested.
        feedbackBlockedRouteBits &= routeBits;

        // Never allow both directions of a reciprocal pair to be effective.
        // Normal GUI edits mark the second request as blocked. Corrupt or older
        // data without a marker is repaired deterministically here.
        PowerDiverterPort[] ports = PowerDiverterPort.values();
        for (int firstIndex = 0; firstIndex < ports.length; firstIndex++) {
            PowerDiverterPort first = ports[firstIndex];

            for (int secondIndex = firstIndex + 1;
                    secondIndex < ports.length;
                    secondIndex++) {
                PowerDiverterPort second = ports[secondIndex];
                int firstToSecondBit = packedRouteBit(first, second);
                int secondToFirstBit = packedRouteBit(second, first);

                boolean firstToSecond = (routeBits & firstToSecondBit) != 0;
                boolean secondToFirst = (routeBits & secondToFirstBit) != 0;

                if (!firstToSecond || !secondToFirst) {
                    feedbackBlockedRouteBits &= ~firstToSecondBit;
                    feedbackBlockedRouteBits &= ~secondToFirstBit;
                    continue;
                }

                boolean firstBlocked = (feedbackBlockedRouteBits
                        & firstToSecondBit) != 0;
                boolean secondBlocked = (feedbackBlockedRouteBits
                        & secondToFirstBit) != 0;

                if (firstBlocked == secondBlocked) {
                    // Keep the lower-index direction effective and block the
                    // opposite direction when saved data cannot identify which
                    // route was requested second.
                    feedbackBlockedRouteBits &= ~firstToSecondBit;
                    feedbackBlockedRouteBits |= secondToFirstBit;
                }
            }
        }
    }

    /**
     * Safe placement default: every port and every source-to-destination route
     * starts disabled/off.
     */
    public static PowerDiverterConfig defaults() {
        return new PowerDiverterConfig(0, 0, 0);
    }

    /**
     * v1.01.04.06 and v1.01.04.07 pre-filled every source with routes to the
     * other three ports. Migrate that untouched legacy value to the explicit
     * OFF default.
     */
    public static int migrateLegacyDefaultRoutes(int storedRouteBits) {
        int sanitizedStoredRoutes = storedRouteBits & ROUTE_MASK;
        return sanitizedStoredRoutes == legacyAllOtherRoutes()
                ? 0
                : sanitizedStoredRoutes;
    }

    public static PowerDiverterConfig sanitized(
            PowerDiverterConfig config
    ) {
        return config == null
                ? defaults()
                : new PowerDiverterConfig(
                        config.portModeBits,
                        config.routeBits,
                        config.feedbackBlockedRouteBits
                );
    }

    public DiverterPortMode getPortMode(PowerDiverterPort port) {
        int shift = port.getIndex() * 2;
        return DiverterPortMode.fromPackedValue(
                this.portModeBits >> shift
        );
    }

    /**
     * Changes only the port mode while the screen is open. Requested routes are
     * retained so a temporarily blocked plan can be edited in either order.
     */
    public PowerDiverterConfig withPortMode(
            PowerDiverterPort port,
            DiverterPortMode mode
    ) {
        int shift = port.getIndex() * 2;
        int clearedBits = this.portModeBits & ~(0b11 << shift);
        int updatedBits = clearedBits | (mode.ordinal() << shift);
        return new PowerDiverterConfig(
                updatedBits,
                this.routeBits,
                this.feedbackBlockedRouteBits
        );
    }

    public int getRouteMask(PowerDiverterPort source) {
        return (this.routeBits >> routeShift(source)) & allPortMask();
    }

    /** Returns the player's saved/requested route. */
    public boolean routesTo(
            PowerDiverterPort source,
            PowerDiverterPort destination
    ) {
        return source != destination
                && (this.getRouteMask(source) & destination.getMask()) != 0;
    }

    /**
     * True when the requested route is inactive because its opposite direction
     * was already requested first.
     */
    public boolean isRouteFeedbackBlocked(
            PowerDiverterPort source,
            PowerDiverterPort destination
    ) {
        return this.routesTo(source, destination)
                && (this.feedbackBlockedRouteBits
                        & packedRouteBit(source, destination)) != 0;
    }

    /** True only when a requested route can presently carry power. */
    public boolean isRouteEffective(
            PowerDiverterPort source,
            PowerDiverterPort destination
    ) {
        return this.routesTo(source, destination)
                && !this.isRouteFeedbackBlocked(source, destination)
                && this.canAcceptInput(source)
                && this.canEmitOutput(destination);
    }

    public boolean isRouteBlockedByPortModes(
            PowerDiverterPort source,
            PowerDiverterPort destination
    ) {
        return this.routesTo(source, destination)
                && (!this.canAcceptInput(source)
                        || !this.canEmitOutput(destination));
    }

    public boolean isRouteBlocked(
            PowerDiverterPort source,
            PowerDiverterPort destination
    ) {
        return this.routesTo(source, destination)
                && !this.isRouteEffective(source, destination);
    }

    /**
     * Enables or disables the player's requested route.
     *
     * Port modes never reject the request. If the reverse route is already
     * requested and effective, this newly enabled direction is saved but marked
     * feedback-blocked. Turning the first direction OFF automatically releases
     * the remaining reverse request.
     */
    public PowerDiverterConfig withRouteEnabled(
            PowerDiverterPort source,
            PowerDiverterPort destination,
            boolean enabled
    ) {
        if (source == destination) {
            return this;
        }

        int routeBit = packedRouteBit(source, destination);
        int reverseBit = packedRouteBit(destination, source);
        int updatedRoutes = this.routeBits;
        int updatedFeedbackBlocks = this.feedbackBlockedRouteBits;

        if (enabled) {
            updatedRoutes |= routeBit;

            boolean reverseRequested = (updatedRoutes & reverseBit) != 0;
            boolean reverseBlocked = (updatedFeedbackBlocks & reverseBit) != 0;

            if (reverseRequested && !reverseBlocked) {
                // This is the second direction, so save it but keep it inactive.
                updatedFeedbackBlocks |= routeBit;
            } else {
                updatedFeedbackBlocks &= ~routeBit;
            }
        } else {
            updatedRoutes &= ~routeBit;
            updatedFeedbackBlocks &= ~routeBit;

            // If the route that had priority is removed, release its saved
            // reverse request so it can become effective automatically.
            if ((updatedRoutes & reverseBit) != 0) {
                updatedFeedbackBlocks &= ~reverseBit;
            }
        }

        return new PowerDiverterConfig(
                this.portModeBits,
                updatedRoutes,
                updatedFeedbackBlocks
        );
    }

    /**
     * Applies save-time rules. A Disabled port is a hard reset: every route
     * originating from it and every route targeting it is cleared to OFF.
     */
    public PowerDiverterConfig normalizedForSave() {
        int updatedRoutes = this.routeBits;
        int updatedFeedbackBlocks = this.feedbackBlockedRouteBits;

        for (PowerDiverterPort disabledPort : PowerDiverterPort.values()) {
            if (this.getPortMode(disabledPort) != DiverterPortMode.DISABLED) {
                continue;
            }

            // Clear every route originating from the disabled port.
            int sourceRouteMask = allPortMask() << routeShift(disabledPort);
            updatedRoutes &= ~sourceRouteMask;
            updatedFeedbackBlocks &= ~sourceRouteMask;

            // Clear every route from another source targeting this port.
            for (PowerDiverterPort source : PowerDiverterPort.values()) {
                int targetBit = packedRouteBit(source, disabledPort);
                updatedRoutes &= ~targetBit;
                updatedFeedbackBlocks &= ~targetBit;
            }
        }

        return new PowerDiverterConfig(
                this.portModeBits,
                updatedRoutes,
                updatedFeedbackBlocks
        );
    }

    public boolean canAcceptInput(PowerDiverterPort port) {
        return this.getPortMode(port).acceptsInput();
    }

    public boolean canEmitOutput(PowerDiverterPort port) {
        return this.getPortMode(port).allowsOutput();
    }

    public static int allPortMask() {
        return (1 << PORT_COUNT) - 1;
    }

    private static int legacyAllOtherRoutes() {
        int legacyRoutes = 0;

        for (PowerDiverterPort source : PowerDiverterPort.values()) {
            int destinations = allPortMask() & ~source.getMask();
            legacyRoutes |= destinations << routeShift(source);
        }

        return legacyRoutes & ROUTE_MASK;
    }

    private static int packedRouteBit(
            PowerDiverterPort source,
            PowerDiverterPort destination
    ) {
        return destination.getMask() << routeShift(source);
    }

    private static int routeShift(PowerDiverterPort source) {
        return source.getIndex() * PORT_COUNT;
    }
}
