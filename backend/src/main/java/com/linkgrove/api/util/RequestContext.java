package com.linkgrove.api.util;

public final class RequestContext {
    private static final ThreadLocal<Long> SELECTED_VARIANT = new ThreadLocal<>();

    private RequestContext() {}

    public static void setSelectedVariantId(Long id) {
        SELECTED_VARIANT.set(id);
    }

    public static Long getSelectedVariantId() {
        return SELECTED_VARIANT.get();
    }

    public static void clear() {
        SELECTED_VARIANT.remove();
    }
}


