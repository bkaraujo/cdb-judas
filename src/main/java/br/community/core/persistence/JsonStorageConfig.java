package br.community.core.persistence;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.LocalFileStorage;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.core.JsonStorageProperties;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Registra módulos Jackson customizados como beans {@link JacksonModule}.
 * O Spring Boot 4 os detecta automaticamente e os incorpora ao ObjectMapper
 * auto-configurado ({@code jacksonJsonMapper}), garantindo um único mapper
 * para persistência e camada web.
 */
@Configuration
@NullMarked
class JsonStorageConfig {

    @Bean
    JacksonModule bigDecimalModule() {
        SimpleModule module = new SimpleModule("bigDecimal");
        module.addSerializer(BigDecimal.class, new ValueSerializer<>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext serializers) {
                gen.writeNumber(value.setScale(2, RoundingMode.HALF_UP));
            }
        });
        return module;
    }

    /**
     * Serializa/desserializa enums de transação como lowercase tokens,
     * mantendo o domínio livre de anotações de framework.
     */
    @Bean
    JacksonModule transactionEnumModule() {
        SimpleModule module = new SimpleModule("transactionEnum");
        module.addSerializer(MonetaryTransaction.Status.class, new ValueSerializer<>() {
            @Override
            public void serialize(MonetaryTransaction.Status value, JsonGenerator gen, SerializationContext serializers) {
                gen.writeString(value.name().toLowerCase(Locale.ROOT));
            }
        });
        module.addDeserializer(MonetaryTransaction.Status.class, new ValueDeserializer<>() {
            @Override
            public MonetaryTransaction.Status deserialize(JsonParser p, DeserializationContext ctxt) {
                return MonetaryTransaction.Status.valueOf(p.getValueAsString().toUpperCase(Locale.ROOT));
            }
        });
        module.addSerializer(MonetaryTransaction.Type.class, new ValueSerializer<>() {
            @Override
            public void serialize(MonetaryTransaction.Type value, JsonGenerator gen, SerializationContext serializers) {
                gen.writeString(value.name().toLowerCase(Locale.ROOT));
            }
        });
        module.addDeserializer(MonetaryTransaction.Type.class, new ValueDeserializer<>() {
            @Override
            public MonetaryTransaction.Type deserialize(JsonParser p, DeserializationContext ctxt) {
                return MonetaryTransaction.Type.valueOf(p.getValueAsString().toUpperCase(Locale.ROOT));
            }
        });
        return module;
    }

    @Bean
    @ConditionalOnProperty(name = "storage.json.backend", havingValue = "local", matchIfMissing = true)
    Storage jsonStorage(JsonStorageProperties props, ObjectMapper mapper) {
        return new LocalFileStorage(props, mapper);
    }
}
