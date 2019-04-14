package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.TreeVisitor;
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

        MessageFormatConversionResult result = MessageFormat.convertJavaTextMessageFormat("{2} {1} {0}",
                ImmutableList.of(argument("abc"), argument("def"), argument("ghi")));

        assertThat( result.messageFormat() ).isEqualTo("%s %s %s");
        assertThat( result.arguments()).hasSize(3);
        assertThat( result.arguments()).extracting("value").containsExactly("ghi","def","abc");
    }

    @Test
    void testPlaceholderArgumentsRepeated() {

        MessageFormatConversionResult result = MessageFormat.convertJavaTextMessageFormat("{0} {0} {0}",
                ImmutableList.of(argument("abc")));

        assertThat( result.messageFormat() ).isEqualTo("%s %s %s");
        assertThat( result.arguments()).hasSize(3);
        assertThat( result.arguments()).extracting("value").containsExactly("abc","abc","abc");
    }

    @Test
    void testEscaping() {
        assertThat( convert("{0} 5%") ).isEqualTo("%s 5%%");
    }

    private String convert( String format ) {
        return convert( format, ImmutableList.of());
    }

    private String convert(String format, List<? extends ExpressionTree> arguments) {
        return MessageFormat.convertJavaTextMessageFormat(format, arguments).messageFormat();
    }

    private ExpressionTree argument( String value ) {
        return new LiteralTree() {
            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public Kind getKind() {
                return Kind.STRING_LITERAL;
            }

            @Override
            public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
                return null;
            }
        };
    }
}