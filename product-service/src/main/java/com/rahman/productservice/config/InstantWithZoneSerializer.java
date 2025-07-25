package com.rahman.productservice.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;

public class InstantWithZoneSerializer extends StdSerializer<Instant> {

    public InstantWithZoneSerializer() {
        super(Instant.class);
    }

    @Override
    public void serialize(Instant instant, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        ZonedDateTime zonedDateTime = instant.atZone(TimeZoneContext.getZoneId());
        jsonGenerator.writeString(zonedDateTime.toString());
    }
}
