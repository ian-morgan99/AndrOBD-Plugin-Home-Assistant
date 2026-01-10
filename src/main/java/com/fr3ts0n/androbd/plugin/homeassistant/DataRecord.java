package com.fr3ts0n.androbd.plugin.homeassistant;

/**
 * Represents a single OBD data record with timestamp and sent status
 */
public class DataRecord {
    private long id;
    private String key;
    private String value;
    private long timestamp;
    private boolean sent;
    
    public DataRecord() {
    }
    
    public DataRecord(String key, String value, long timestamp) {
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
        this.sent = false;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isSent() {
        return sent;
    }
    
    public void setSent(boolean sent) {
        this.sent = sent;
    }
    
    @Override
    public String toString() {
        return "DataRecord{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", timestamp=" + timestamp +
                ", sent=" + sent +
                '}';
    }
}
