package com.digitalascent.errorprone.flogger.testdata;


import com.google.common.flogger.FluentLogger;

public class TestClassUsingJUL_0 {
    private static final FluentLogger someLogger = FluentLogger.forEnclosingClass();

    private int x = 1;

    public void testLogLevels() {
        someLogger.atFinest().log( "test message" );
        someLogger.atFinest().log( "test message" );

        someLogger.atFiner().log( "test message" );
        someLogger.atFiner().log( "test message" );

        someLogger.atFine().log( "test message" );
        someLogger.atFine().log( "test message" );

        someLogger.atFine().log( "test message" );
        someLogger.atFine().log( "test message" );

        someLogger.atInfo().log( "test message" );
        someLogger.atInfo().log( "test message" );

        someLogger.atWarning().log( "test message" );
        someLogger.atWarning().log( "test message" );

        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );

        someLogger.at(CustomJULLevel.LEVEL_1).log( "test message" );
        someLogger.at(CustomJULLevel.LEVEL_2).log( "test message" );
        someLogger.at(CustomJULLevel.LEVEL_3).log( "test message" );
    }

    public void testEnabled() {
        someLogger.atFinest().isEnabled();
        someLogger.atFiner().isEnabled();
        someLogger.atFine().isEnabled();
        someLogger.atFine().isEnabled();
        someLogger.atInfo().isEnabled();
        someLogger.atWarning().isEnabled();
        someLogger.atSevere().isEnabled();

        someLogger.at(CustomJULLevel.LEVEL_1).isEnabled();
        someLogger.at(CustomJULLevel.LEVEL_2).isEnabled();
        someLogger.at(CustomJULLevel.LEVEL_3).isEnabled();
    }

    public void testMessageFormat() {
        someLogger.atInfo().log( "1. Single parameter: %s", "abc" );
        someLogger.atInfo().log( "2. Escaped formatting anchor: \\{0}" );
        someLogger.atInfo().log( "3. Escaped anchor and single parameter: \\%s %s", "abc", "abc" );
        someLogger.atInfo().log( "4. Escaped anchors and single parameter: \\%s %s \\%s", "abc", "abc", "abc" );
        someLogger.atInfo().log( "5. Double-escaped anchor, single parameter: \\\\%s", "abc" );
        someLogger.atInfo().log( "6. Double-escaped anchor, no parameter: \\\\{0}" );
        someLogger.atInfo().log( "7. Single parameter, double-escaped anchor: %s \\\\%s", "abc", "abc" );
        someLogger.atInfo().log( "8. Multi parameters: %s %s %s", "def", "abc", "abc" );
        someLogger.atInfo().log( "9. Multi parameters 2: {9} %s %s", "abc", "abc", "def", "ghi" );
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch (NullPointerException e) {
            someLogger.atInfo().withCause(e).log( "Exception!!!" );
        }
    }

    public void testOther() {
        someLogger.atInfo().log( "a" + 1 + "b" );
        someLogger.atInfo().log( "%s", "abc" );
    }


}