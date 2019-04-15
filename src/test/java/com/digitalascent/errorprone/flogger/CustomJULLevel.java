package com.digitalascent.errorprone.flogger;

import java.util.logging.Level;

public class CustomJULLevel extends Level {
    public static final Level LEVEL_1 = new CustomJULLevel("CustomLevel1", 100);
    public static final Level LEVEL_2 = new CustomJULLevel("CustomLevel2", 200);
    public static final Level LEVEL_3 = new CustomJULLevel("CustomLevel3", 800);

    private CustomJULLevel(String name, int value) {
        super(name, value);
    }
}
