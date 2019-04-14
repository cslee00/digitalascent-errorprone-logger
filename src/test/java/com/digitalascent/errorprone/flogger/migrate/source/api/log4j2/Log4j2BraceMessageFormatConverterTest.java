package com.digitalascent.errorprone.flogger.migrate.source.api.log4j2;


import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class Log4j2BraceMessageFormatConverterTest {

    @Test
    void test() {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(Log4j2BraceMessageFormatConverter.convertMessageFormat("{}")).isEqualTo("%s");
        softly.assertThat(Log4j2BraceMessageFormatConverter.convertMessageFormat("{} {} {}")).isEqualTo("%s %s %s");
        softly.assertThat(Log4j2BraceMessageFormatConverter.convertMessageFormat("abc {} {} {} def")).isEqualTo("abc %s %s %s def");
        softly.assertThat(Log4j2BraceMessageFormatConverter.convertMessageFormat("\\{}")).isEqualTo("{}");
        softly.assertThat(Log4j2BraceMessageFormatConverter.convertMessageFormat("\\\\{}")).isEqualTo("\\%s");
        softly.assertThat(Log4j2BraceMessageFormatConverter.convertMessageFormat("5% of {}")).isEqualTo("5%% of %s");
        softly.assertAll();
    }
}