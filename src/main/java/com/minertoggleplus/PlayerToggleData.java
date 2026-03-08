package com.minertoggleplus;

import java.util.EnumMap;
import java.util.Map;

/**
 * Stores the toggle states for a single player.
 */
public class PlayerToggleData {

    private final Map<Toggle, Boolean> toggles = new EnumMap<>(Toggle.class);

    public boolean isEnabled(Toggle toggle) {
        return toggles.getOrDefault(toggle, false);
    }

    /**
     * Flips the toggle and returns the new state.
     */
    public boolean toggle(Toggle toggle) {
        boolean newState = !isEnabled(toggle);
        toggles.put(toggle, newState);
        return newState;
    }

    public void setEnabled(Toggle toggle, boolean enabled) {
        toggles.put(toggle, enabled);
    }

    public Map<Toggle, Boolean> getAll() {
        return toggles;
    }
}
