package mitv.player;

final class SourceItem {
    final String key;
    final String title;
    final SourceKind kind;
    final int numericId;
    final String romValue;
    final String action;
    final String activityClassName;
    final String[] aliases;

    SourceItem(
            String key,
            String title,
            SourceKind kind,
            int numericId,
            String romValue,
            String action,
            String activityClassName,
            String... aliases
    ) {
        this.key = key;
        this.title = title;
        this.kind = kind;
        this.numericId = numericId;
        this.romValue = romValue;
        this.action = action;
        this.activityClassName = activityClassName;
        this.aliases = aliases == null ? new String[0] : aliases;
    }

    boolean matches(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals(key.toLowerCase())
                || normalized.equals(title.toLowerCase())
                || normalized.equals(romValue.toLowerCase())
                || normalized.equals(String.valueOf(numericId))
                || matchesAlias(normalized);
    }

    boolean isDeviceAlias() {
        return kind == SourceKind.EXTERNAL
                || kind == SourceKind.ROUTER
                || kind == SourceKind.MI_BOX
                || kind == SourceKind.MI_PORT
                || kind == SourceKind.BLU_RAY
                || kind == SourceKind.SOUNDBAR
                || kind == SourceKind.HOME_CINEMA
                || kind == SourceKind.UNKNOWN_DEVICE;
    }

    boolean requiresPreciseSwitch() {
        return kind == SourceKind.HDMI || kind == SourceKind.USB || kind == SourceKind.VGA;
    }

    private boolean matchesAlias(String normalized) {
        for (String alias : aliases) {
            if (alias != null && normalized.equals(alias.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}

