package com.example.headlesscrm.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomFieldsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void customer_CustomFieldsSerialization() throws JsonProcessingException {
        Map<String, Object> custom = new HashMap<>();
        custom.put("industry", "FinTech");
        custom.put("annualRevenue", 15000000);
        custom.put("tier", "PLATINUM");

        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .customFields(custom)
                .build();

        String json = objectMapper.writeValueAsString(customer);
        Customer deserialized = objectMapper.readValue(json, Customer.class);

        assertThat(deserialized.getCustomFields()).isNotNull();
        assertThat(deserialized.getCustomFields().get("industry")).isEqualTo("FinTech");
        assertThat(deserialized.getCustomFields().get("tier")).isEqualTo("PLATINUM");
    }

    @Test
    void deal_CustomFieldsSerialization() throws JsonProcessingException {
        Map<String, Object> custom = new HashMap<>();
        custom.put("competitor", "CompetitorCorp");
        custom.put("renewalProbability", 0.95);

        Deal deal = Deal.builder()
                .id(UUID.randomUUID())
                .title("Acme Expansion")
                .value(new BigDecimal("120000.00"))
                .customFields(custom)
                .build();

        String json = objectMapper.writeValueAsString(deal);
        Deal deserialized = objectMapper.readValue(json, Deal.class);

        assertThat(deserialized.getCustomFields()).isNotNull();
        assertThat(deserialized.getCustomFields().get("competitor")).isEqualTo("CompetitorCorp");
    }

    @Test
    void activity_CustomFieldsSerialization() throws JsonProcessingException {
        Map<String, Object> custom = new HashMap<>();
        custom.put("zoomMeetingId", "987-654-321");
        custom.put("recordingUrl", "https://zoom.us/rec/12345");

        Activity activity = Activity.builder()
                .id(UUID.randomUUID())
                .type(ActivityType.MEETING)
                .title("Demo call")
                .customFields(custom)
                .build();

        String json = objectMapper.writeValueAsString(activity);
        Activity deserialized = objectMapper.readValue(json, Activity.class);

        assertThat(deserialized.getCustomFields()).isNotNull();
        assertThat(deserialized.getCustomFields().get("zoomMeetingId")).isEqualTo("987-654-321");
    }
}
