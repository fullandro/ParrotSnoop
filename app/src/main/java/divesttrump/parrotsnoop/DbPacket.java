package divesttrump.parrotsnoop;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;


@Entity(tableName = "packets")
class DbPacket {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "timestamp_long")
    private long timestampLong;

    @ColumnInfo(name = "timestamp_string")
    private String timestampString;

    @ColumnInfo(name = "ip_version")
    private String ipVersion;

    @ColumnInfo(name = "transport_type")
    private String transportType;

    @ColumnInfo(name = "source_address")
    private String sourceAddress;

    @ColumnInfo(name = "destination_address")
    private String destinationAddress;

    @ColumnInfo(name = "payload_size")
    private int payloadSize;

    @ColumnInfo(name = "payload_contents")
    private String payloadContents;

    DbPacket() {
        Date now = Calendar.getInstance().getTime();
        timestampLong = now.getTime();
        timestampString = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(now);
    }

    int getId() {
        return this.id;
    }
    void setId(int id) {
        this.id = id;
    }

    long getTimestampLong() {
        return this.timestampLong;
    }
    void setTimestampLong(long timestampLong) {
        this.timestampLong = timestampLong;
    }

    String getTimestampString() {
        return this.timestampString;
    }
    void setTimestampString(String timestampString) {
        this.timestampString = timestampString;
    }

    String getIpVersion() {
        return ipVersion;
    }
    void setIpVersion(String ipVersion) {
        this.ipVersion = ipVersion;
    }

    String getTransportType() {
        return transportType;
    }
    void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    String getSourceAddress() {
        return  this.sourceAddress;
    }
    void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    String getDestinationAddress() {
        return this.destinationAddress;
    }
    void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    int getPayloadSize() {
        return this.payloadSize;
    }
    void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }

    String getPayloadContents() {
        return this.payloadContents;
    }
    void setPayloadContents(String payloadContents) {
        this.payloadContents = payloadContents;
    }

    String getShortDescription() {
        String description = "[" + getIpVersion() + "/" + getTransportType() + "] ";
        description = description + getTimestampString() + "\r\n\t";
        description = description + getSourceAddress() + " -> ";
        description = description + getDestinationAddress();
        description = description + " [" + getPayloadSize() + "]";

        return description;
    }

    String getLongDescription() {
        String description = "Timestamp: " + getTimestampString() + "\n";
        description = description + "Version: " + getIpVersion() + "\n";
        description = description + "Transport: " + getTransportType() + "\n";
        description = description + "Source: " + getSourceAddress() + "\n";
        description = description + "Destination: " + getDestinationAddress() + "\n";
        description = description + "Size: " + getPayloadSize() + "\n";
        description = description + "Contents: " + getPayloadContents();

        return description;
    }

    String getCsvLine() {
        String csvLine = getTimestampString().replace(",", "") + ",";
        csvLine = csvLine + getIpVersion() + ",";
        csvLine = csvLine + getTransportType() + ",";
        csvLine = csvLine + getSourceAddress() + ",";
        csvLine = csvLine + getDestinationAddress() + ",";
        csvLine = csvLine + getPayloadSize() + ",";
        csvLine = csvLine + getPayloadContents().replace(",", ";").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t") + "\r\n";

        return csvLine;
    }
}