package net.eric.tpc.persist;

public class KeyRecord {
    
    private String prefix;
    private int dateDigit;
    private int serial;
        
    public KeyRecord() {
    }
    
    public KeyRecord(String prefix, int dateDigit, int serial) {
        super();
        this.prefix = prefix;
        this.dateDigit = dateDigit;
        this.serial = serial;
    }
    public String getPrefix() {
        return prefix;
    }
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    public int getDateDigit() {
        return dateDigit;
    }
    public void setDateDigit(int dateDigit) {
        this.dateDigit = dateDigit;
    }
    public int getSerialNumber() {
        return serial;
    }
    public void setSerialNumber(int serial) {
        this.serial = serial;
    }
    
    
}
