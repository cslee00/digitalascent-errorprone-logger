package com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class Slf4jMessageFormatConverterTest {

    @Test
    void test() {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(Slf4jMessageFormatConverter.convertMessageFormat("{}")).isEqualTo("%s");
        softly.assertThat(Slf4jMessageFormatConverter.convertMessageFormat("{} {} {}")).isEqualTo("%s %s %s");
        softly.assertThat(Slf4jMessageFormatConverter.convertMessageFormat("abc {} {} {} def")).isEqualTo("abc %s %s %s def");
        softly.assertThat(Slf4jMessageFormatConverter.convertMessageFormat("\\{}")).isEqualTo("{}");
        softly.assertThat(Slf4jMessageFormatConverter.convertMessageFormat("\\\\{}")).isEqualTo("\\%s");
        softly.assertThat(Slf4jMessageFormatConverter.convertMessageFormat("5% of {}")).isEqualTo("5%% of %s");
        softly.assertAll();
    }

}