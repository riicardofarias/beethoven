package br.com.tdsoft.beethoven.common;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

@ReadingConverter
@WritingConverter
public class ByteBooleanConverter implements Converter<Byte, Boolean> {
    @Override
    public Boolean convert(@NotNull Byte source) {
        return source == 1;
    }
}
