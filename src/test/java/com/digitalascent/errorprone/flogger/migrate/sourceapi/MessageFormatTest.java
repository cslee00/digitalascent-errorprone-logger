package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageFormatTest {

    @Test
    void testPlaceholderReplacement() {
        assertThat( convert("a {3,number,integer} b {1} c {4} d {0} e") ).isEqualTo("a %s b %s c %s d %s e");
    }

    @Test
    void testPlaceholderArguments() {

        LogMessage logMessage = MessageFormat.convertJavaTextMessageFormat(null,"{2} {1} {0}",
                ImmutableList.of(argument("abc"), argument("def"), argument("ghi")));

        assertThat( logMessage.messageFormat() ).isEqualTo("%s %s %s");
        assertThat( logMessage.arguments()).hasSize(3);
        assertThat( logMessage.arguments()).extracting("code").containsExactly("ghi","def","abc");
    }

    @Test
    void testPlaceholderArgumentsRepeated() {

        LogMessage logMessage = MessageFormat.convertJavaTextMessageFormat(null,"{0} {0} {0}",
                ImmutableList.of(argument("abc")));

        assertThat( logMessage.messageFormat() ).isEqualTo("%s %s %s");
        assertThat( logMessage.arguments()).hasSize(3);
        assertThat( logMessage.arguments()).extracting("code").containsExactly("abc","abc","abc");
    }

    @Test
    void testEscaping() {
        assertThat( convert("{0} 5%") ).isEqualTo("%s 5%%");
    }

    private String convert( String format ) {
        return convert( format, ImmutableList.of());
    }

    private String convert(String format, List<MessageFormatArgument> arguments) {
        return MessageFormat.convertJavaTextMessageFormat(null,format, arguments).messageFormat();
    }

    private MessageFormatArgument argument( String value ) {
        return MessageFormatArgument.fromCode(value, ImmutableList.of(), ImmutableList.of());
    }
}