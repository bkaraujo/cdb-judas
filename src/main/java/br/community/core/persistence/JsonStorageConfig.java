package br.community.core.persistence;

import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.LocalFileStorage;
import br.commons.tools.Strings;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.core.JsonStorageProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Registra os módulos Jackson customizados no {@link ObjectMapper} único do Quarkus
 * (via {@link ObjectMapperCustomizer}) — garantindo o mesmo mapper para a camada web
 * e para a persistência ({@link #jsonStorage}). Mantém o domínio livre de anotações.
 */
@Singleton
@NullMarked
public class JsonStorageConfig implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper mapper) {
        mapper.registerModule(bigDecimalModule());
        mapper.registerModule(transactionEnumModule());
    }

    private SimpleModule bigDecimalModule() {
        val module = new SimpleModule("bigDecimal");
        module.addSerializer(BigDecimal.class, new JsonSerializer<>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeNumber(value.setScale(2, RoundingMode.HALF_UP));
            }
        });
        return module;
    }

    /**
     * Serializa/desserializa enums de transação como lowercase tokens,
     * mantendo o domínio livre de anotações de framework.
     */
    private SimpleModule transactionEnumModule() {
        val module = new SimpleModule("transactionEnum");
        module.addSerializer(Transaction.Status.class, new JsonSerializer<>() {
            @Override
            public void serialize(Transaction.Status value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(Strings.lower(value.name()));
            }
        });
        module.addDeserializer(Transaction.Status.class, new JsonDeserializer<>() {
            @Override
            public Transaction.Status deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return Transaction.Status.valueOf(Strings.upper(p.getValueAsString()));
            }
        });
        module.addSerializer(Transaction.Type.class, new JsonSerializer<>() {
            @Override
            public void serialize(Transaction.Type value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(Strings.lower(value.name()));
            }
        });
        module.addDeserializer(Transaction.Type.class, new JsonDeserializer<>() {
            @Override
            public Transaction.Type deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return Transaction.Type.valueOf(Strings.upper(p.getValueAsString()));
            }
        });
        return module;
    }

    @Produces
    @Singleton
    Storage jsonStorage(JsonStorageProperties props, ObjectMapper mapper) {
        return new LocalFileStorage(props, mapper);
    }
}
