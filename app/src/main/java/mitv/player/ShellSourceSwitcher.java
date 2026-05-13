package mitv.player;

import java.io.IOException;

final class ShellSourceSwitcher {
    private ShellSourceSwitcher() {
    }

    static SwitchResult switchTo(SourceItem item) {
        String command = "am start"
                + " -a " + shellQuote(item.action)
                + " --es sourceName " + shellQuote(item.romValue)
                + " --es source_name " + shellQuote(item.romValue)
                + " --es sourceTitle " + shellQuote(item.title)
                + " --es device_name " + shellQuote(item.title)
                + " --es sourceType " + shellQuote(item.kind.name())
                + " --ei sourceId " + item.numericId
                + " --ei source_id " + item.numericId;
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return SwitchResult.ok("shell:am start");
            }
            return SwitchResult.fail("shell exit " + exitCode);
        } catch (IOException exception) {
            return SwitchResult.fail("shell io:" + exception.getClass().getSimpleName());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return SwitchResult.fail("shell interrupted");
        }
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}

