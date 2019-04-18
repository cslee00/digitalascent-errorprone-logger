package com.digitalascent.errorprone.flogger.migrate.source.api.tinylog;


import org.pmw.tinylog.Logger;

public class TinyLogOutput {

    public static void main(String[] args) {
        Logger.info(new Throwable());
        Logger.info("message", new Throwable());
        Logger.info("1. Single parameter: {}", "abc");
        Logger.info("2. Escaped formatting anchor: \\{}");
        Logger.info("3. Escaped anchor and single parameter: \\{} {}", "abc");
        Logger.info("4. Escaped anchors and single parameter: \\{} {} \\{}", "abc");
        Logger.info("5. Double-escaped anchor, single parameter: \\\\{}", "abc");
        Logger.info("6. Double-escaped anchor, no parameter: \\\\{}");
        Logger.info("7. Single parameter, double-escaped anchor: {} \\\\{}", "abc");
        Logger.info("8. Percent sign: 5% of {}", "abc");
        Logger.info("9. Object[] {} {} {}", new Object[]{"abc", "def", "ghi"});
    }
}
