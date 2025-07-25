package com.rahman.productservice.config;

import java.time.ZoneId;

public class TimeZoneContext {
    private static final ThreadLocal<ZoneId> ZONE_ID_HOLDER =
            ThreadLocal.withInitial(() -> ZoneId.of("UTC"));

    public static void setZoneId(ZoneId zoneId) {
        ZONE_ID_HOLDER.set(zoneId);
    }

    public static ZoneId getZoneId() {
        return ZONE_ID_HOLDER.get();
    }

    public static void clear() {
        ZONE_ID_HOLDER.remove();
    }
}
