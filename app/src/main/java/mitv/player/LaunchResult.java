package mitv.player;

final class LaunchResult {
    final boolean success;
    final String detail;

    private LaunchResult(boolean success, String detail) {
        this.success = success;
        this.detail = detail;
    }

    static LaunchResult ok(String detail) {
        return new LaunchResult(true, detail);
    }

    static LaunchResult fail(String detail) {
        return new LaunchResult(false, detail);
    }
}

