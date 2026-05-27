package br.community.core.persistence;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.LocalFileStorage;
import br.community.core.JsonStorageProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Configuration
@NullMarked
class JsonStorageConfig {

    @Bean
    @Primary
    ObjectMapper objectMapper() {
        SimpleModule bigDecimalModule = new SimpleModule();
        bigDecimalModule.addSerializer(BigDecimal.class, new JsonSerializer<>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeNumber(value.setScale(2, RoundingMode.HALF_UP));
            }
        });

        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(bigDecimalModule)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "storage.json.backend", havingValue = "local", matchIfMissing = true)
    Storage jsonStorage(JsonStorageProperties props, ObjectMapper mapper) {
        return new LocalFileStorage(props, mapper);
    }
}
