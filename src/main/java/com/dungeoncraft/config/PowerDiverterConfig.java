package com.dungeoncraft.config;

/**
 * Compact, packet-friendly configuration for the four-port Power Diverter.
 *
 * - portModeBits stores two bits per port;
 * - routeBits stores four destination bits for each source port.
 *
 * Self-routes are always removed. A signal that enters a source port can never
 * be immediately echoed back through that same physical port.
 */
public record PowerDiverterConfig(
        int portModeBits,
        int routeBits
) {
    private static final int PORT_COUNT = 4;
    private static final int PORT_MODE_MASK = 0xFF;
    private static final int ROUTE_MASK = 0xFFFF;

    public PowerDiverterConfig {
        portModeBits &= PORT_MODE_MASK;
        routeBits &= ROUTE_MASK;

        for (PowerDiverterPort source : PowerDiverterPort.values()) {
            routeBits &= ~(source.getMask() << routeShift(source));
        }
    }

    /**
     * Safe placement default: all ports are disabled, while each source route
     * is pre-filled to the other three ports. Enabling input/output modes is
     * therefore enough to make a route active without creating a live loop on
     * placement.
     */
    public static PowerDiverterConfig defaults() {
        int defaultRoutes = 0;

        for (PowerDiverterPort source : PowerDiverterPort.values()) {
            int destinations = allPortMask() & ~source.getMask();
            defaultRoutes |= destinations << routeShift(source);
        }

        return new PowerDiverterConfig(0, defaultRoutes);
    }

    public static PowerDiverterConfig sanitized(
            PowerDiverterConfig config
    ) {
        return config == null
                ? defaults()
                : new PowerDiverterConfig(
                        config.portModeBits,
                        config.routeBits
                );
    }

    public DiverterPortMode getPortMode(PowerDiverterPort port) {
        int shift = port.getIndex() * 2;
        return DiverterPortMode.fromPackedValue(
                this.portModeBits >> shift
        );
    }

    public PowerDiverterConfig withPortMode(
            PowerDiverterPort port,
            DiverterPortMode mode
    ) {
        int shift = port.getIndex() * 2;
        int clearedBits = this.portModeBits & ~(0b11 << shift);
        int updatedBits = clearedBits | (mode.ordinal() << shift);
        return new PowerDiverterConfig(updatedBits, this.routeBits);
    }

    public int getRouteMask(PowerDiverterPort source) {
        return (this.routeBits >> routeShift(source)) & allPortMask();
    }

    public boolean routesTo(
            PowerDiverterPort source,
            PowerDiverterPort destination
    ) {
        return source != destination
                && (this.getRouteMask(source) & destination.getMask()) != 0;
    }

    public PowerDiverterConfig withRouteEnabled(
            PowerDiverterPort source,
            PowerDiverterPort destination,
            boolean enabled
    ) {
        if (source == destination) {
            return this;
        }

        int shift = routeShift(source);
        int sourceMask = this.getRouteMask(source);

        if (enabled) {
            sourceMask |= destination.getMask();
        } else {
            sourceMask &= ~destination.getMask();
        }

        int clearedRoutes = this.routeBits
                & ~(allPortMask() << shift);
        int updatedRoutes = clearedRoutes | (sourceMask << shift);
        return new PowerDiverterConfig(this.portModeBits, updatedRoutes);
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

    private static int routeShift(PowerDiverterPort source) {
        return source.getIndex() * PORT_COUNT;
    }
}
