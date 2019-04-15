package com.digitalascent.errorprone.flogger.migrate.source.format.argconverter;

import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatArgument;
import com.google.common.collect.ImmutableList;

abstract class AbstractLazyArgConverter implements MessageFormatArgumentConverter{
    private static final ImmutableList<String> LAZY_ARG_IMPORT = ImmutableList.of("com.google.common.flogger.LazyArgs.lazy");

    protected final MessageFormatArgument lazyArgument(String code ) {
        String source = "lazy(" + code + ")";
        return MessageFormatArgument.fromCode(source, ImmutableList.of(), LAZY_ARG_IMPORT);
    }
}
