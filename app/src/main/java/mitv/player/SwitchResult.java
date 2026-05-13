package mitv.player;

final class SwitchResult {
    final boolean success;
    final String detail;

    private SwitchResult(boolean success, String detail) {
        this.success = success;
        this.detail = detail;
    }

    static SwitchResult ok(String detail) {
        return new SwitchResult(true, detail);
    }

    static SwitchResult fail(String detail) {
        return new SwitchResult(false, detail);
    }
}

