package net.runelite.client.plugins.microbot.mule;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a mule request (client-side model)
 * Compatible with bridge server response format
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MuleRequest {
    private String id;
    private String requesterUsername;
    private String muleAccount;
    private String location;
    private List<TradeItem> items;

    // Use custom deserializer to handle both enum and string status values
    @JsonDeserialize(using = StatusDeserializer.class)
    @JsonSerialize(using = StatusSerializer.class)
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private String currentStep;

    // Constructors
    public MuleRequest() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }

    public String getMuleAccount() { return muleAccount; }
    public void setMuleAccount(String muleAccount) { this.muleAccount = muleAccount; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public List<TradeItem> getItems() { return items; }
    public void setItems(List<TradeItem> items) { this.items = items; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }

    @Override
    public String toString() {
        return "MuleRequest{" +
                "id='" + id + '\'' +
                ", requesterUsername='" + requesterUsername + '\'' +
                ", location='" + location + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

/**
 * Custom deserializer to handle RequestStatus enum from bridge server
 */
class StatusDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return p.getValueAsString(); // Convert enum to string
    }
}

/**
 * Custom serializer for status field
 */
class StatusSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value);
    }
}

/**
 * Represents a trade item (client-side model)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class TradeItem {
    private int itemId;
    private String itemName;
    private int quantity;

    // Constructors
    public TradeItem() {}

    public TradeItem(int itemId, String itemName, int quantity) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
    }

    // Getters and Setters
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
