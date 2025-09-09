package net.runelite.microbot.mule.bridge.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a mule trading request in the system
 */
public class MuleRequest {

    private String id;

    @NotBlank(message = "Requester username is required")
    private String requesterUsername;

    @NotBlank(message = "Mule account is required")
    private String muleAccount;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Items list is required")
    private List<TradeItem> items;

    private RequestStatus status = RequestStatus.QUEUED;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String currentStep;

    public MuleRequest() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.currentStep = "QUEUED";
    }

    public MuleRequest(String requesterUsername, String muleAccount, String location, List<TradeItem> items) {
        this();
        this.requesterUsername = requesterUsername;
        this.muleAccount = muleAccount;
        this.location = location;
        this.items = items;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequesterUsername() {
        return requesterUsername;
    }

    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }

    public String getMuleAccount() {
        return muleAccount;
    }

    public void setMuleAccount(String muleAccount) {
        this.muleAccount = muleAccount;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<TradeItem> getItems() {
        return items;
    }

    public void setItems(List<TradeItem> items) {
        this.items = items;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    @Override
    public String toString() {
        return "MuleRequest{" +
                "id='" + id + '\'' +
                ", requesterUsername='" + requesterUsername + '\'' +
                ", muleAccount='" + muleAccount + '\'' +
                ", location='" + location + '\'' +
                ", status=" + status +
                ", currentStep='" + currentStep + '\'' +
                '}';
    }
}
