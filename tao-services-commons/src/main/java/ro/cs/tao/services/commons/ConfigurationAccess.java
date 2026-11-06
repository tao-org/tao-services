package ro.cs.tao.services.commons;

import java.util.Set;

/** Explicit client-facing settings; all other global settings are administrative. */
public final class ConfigurationAccess {
    private static final Set<String> PUBLIC_KEYS = Set.of(
            "tao.ui.base", "tao.services.base", "component.editor");
    private ConfigurationAccess() { }
    public static boolean isPublic(String key) { return PUBLIC_KEYS.contains(key); }
}
