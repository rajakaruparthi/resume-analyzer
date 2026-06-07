package com.sprint.analyzer.until;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PasswordHashDeserializer extends JsonDeserializer<String> {

    private static PasswordEncoder encoder;

    @Autowired
    public void setEncoder(PasswordEncoder passwordEncoder) {
        PasswordHashDeserializer.encoder = passwordEncoder;
    }

    public PasswordHashDeserializer() {
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
        String raw = parser.getValueAsString();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (encoder == null) {
            encoder = new BCryptPasswordEncoder();
        }
        return encoder.encode(raw);
    }
}
