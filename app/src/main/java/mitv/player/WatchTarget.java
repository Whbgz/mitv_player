package mitv.player;

final class WatchTarget {
    final String key;
    final String title;
    final String action;
    final String sourceName;
    final int sourceId;
    final String activityClassName;

    WatchTarget(
            String key,
            String title,
            String action,
            String sourceName,
            int sourceId,
            String activityClassName
    ) {
        this.key = key;
        this.title = title;
        this.action = action;
        this.sourceName = sourceName;
        this.sourceId = sourceId;
        this.activityClassName = activityClassName;
    }
}

