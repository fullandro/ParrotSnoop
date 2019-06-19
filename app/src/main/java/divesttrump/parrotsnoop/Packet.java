package divesttrump.parrotsnoop;


import android.support.annotation.NonNull;
import android.util.Base64;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;


public class Packet {

    static final int IP4_HEADER_SIZE = 20;
    static final int IP6_HEADER_SIZE = 40;
    static final int TCP_HEADER_SIZE = 20;
    static final int UDP_HEADER_SIZE = 8;

    IP4Header ip4Header;
    IP6Header ip6Header;
    TCPHeader tcpHeader;
    UDPHeader udpHeader;
    ByteBuffer backingBuffer;

    private boolean isIP4;
    private boolean isIP6;
    private boolean isTCP;
    private boolean isUDP;

    Packet(ByteBuffer buffer) throws UnknownHostException {
        int ipVersion = (buffer.duplicate().get() >> 4);
        if (ipVersion == 4) {
            this.ip4Header = new IP4Header(buffer);
            if (this.ip4Header.protocol == IP4Header.TransportProtocol.TCP) {
                this.tcpHeader = new TCPHeader(buffer);
                this.isTCP = true;
            } else if (ip4Header.protocol == IP4Header.TransportProtocol.UDP) {
                this.udpHeader = new UDPHeader(buffer);
                this.isUDP = true;
            }
            this.backingBuffer = buffer;
            this.isIP4 = true;
        } else if (ipVersion == 6) {
            this.ip6Header = new IP6Header(buffer);
            if (this.ip6Header.protocol == IP6Header.TransportProtocol.TCP) {
                this.tcpHeader = new TCPHeader(buffer);
                this.isTCP = true;
            } else if (ip6Header.protocol == IP6Header.TransportProtocol.UDP) {
                this.udpHeader = new UDPHeader(buffer);
                this.isUDP = true;
            }
            this.backingBuffer = buffer;
            this.isIP6 = true;
        }
    }

    @Override
    public @NonNull String toString() {
        String packetString = "Packet{";
        if (isIP4) packetString = packetString + "ip4Header=" + ip4Header;
        else if (isIP6) packetString = packetString + "ip6Header=" + ip6Header;
        if (isTCP) packetString = packetString + ", tcpHeader=" + tcpHeader;
        else if (isUDP) packetString = packetString + ", udpHeader=" + udpHeader;
        packetString = packetString + ", payloadSize=" + (backingBuffer.limit() - backingBuffer.position()) + "}";

        return packetString;
    }

    boolean isIP4() {
        return isIP4;
    }
    boolean isIP6() {
        return isIP6;
    }
    boolean isTCP() {
        return isTCP;
    }
    boolean isUDP() {
        return isUDP;
    }

    void swapSourceAndDestination() {
        if (isIP4) {
            InetAddress newSourceAddress = ip4Header.destinationAddress;
            ip4Header.destinationAddress = ip4Header.sourceAddress;
            ip4Header.sourceAddress = newSourceAddress;
        } else if (isIP6) {
            InetAddress newSourceAddress = ip6Header.destinationAddress;
            ip6Header.destinationAddress = ip6Header.sourceAddress;
            ip6Header.sourceAddress = newSourceAddress;
        }

        if (isUDP) {
            int newSourcePort = udpHeader.destinationPort;
            udpHeader.destinationPort = udpHeader.sourcePort;
            udpHeader.sourcePort = newSourcePort;
        } else if (isTCP) {
            int newSourcePort = tcpHeader.destinationPort;
            tcpHeader.destinationPort = tcpHeader.sourcePort;
            tcpHeader.sourcePort = newSourcePort;
        }
    }

    void updateTCPBuffer(ByteBuffer buffer, byte flags, long sequenceNum, long ackNum, int payloadSize) {
        buffer.position(0);
        fillHeader(buffer);
        backingBuffer = buffer;

        int ipHeaderSize = IP4_HEADER_SIZE;
        if (isIP6)
            ipHeaderSize = IP6_HEADER_SIZE;

        tcpHeader.flags = flags;
        backingBuffer.put(ipHeaderSize + 13, flags);

        tcpHeader.sequenceNumber = sequenceNum;
        backingBuffer.putInt(ipHeaderSize + 4, (int) sequenceNum);

        tcpHeader.acknowledgementNumber = ackNum;
        backingBuffer.putInt(ipHeaderSize + 8, (int) ackNum);

        byte dataOffset = (byte) (TCP_HEADER_SIZE << 2);
        tcpHeader.dataOffsetAndReserved = dataOffset;
        backingBuffer.put(ipHeaderSize + 12, dataOffset);

        updateTCPChecksum(payloadSize);

        if (isIP4) {
            int ip4TotalLength = ipHeaderSize + TCP_HEADER_SIZE + payloadSize;
            backingBuffer.putShort(2, (short) ip4TotalLength);
            ip4Header.totalLength = ip4TotalLength;

            updateIP4Checksum();
        }
    }

    void updateUDPBuffer(ByteBuffer buffer, int payloadSize) {
        buffer.position(0);
        fillHeader(buffer);
        backingBuffer = buffer;

        int udpTotalLength = UDP_HEADER_SIZE + payloadSize;
        backingBuffer.putShort(IP4_HEADER_SIZE + 4, (short) udpTotalLength);
        udpHeader.length = udpTotalLength;

        // Disable UDP checksum validation
        backingBuffer.putShort(IP4_HEADER_SIZE + 6, (short) 0);
        udpHeader.checksum = 0;

        int ip4TotalLength = IP4_HEADER_SIZE + udpTotalLength;
        backingBuffer.putShort(2, (short) ip4TotalLength);
        ip4Header.totalLength = ip4TotalLength;

        updateIP4Checksum();
    }

    private void updateIP4Checksum() {
        ByteBuffer buffer = backingBuffer.duplicate();
        buffer.position(0);

        // Clear previous checksum
        buffer.putShort(10, (short) 0);

        int ipLength = ip4Header.headerLength;
        int sum = 0;
        while (ipLength > 0) {
            sum += BitUtils.getUnsignedShort(buffer.getShort());
            ipLength -= 2;
        }
        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;
        ip4Header.headerChecksum = sum;
        backingBuffer.putShort(10, (short) sum);
    }

    private void updateTCPChecksum(int payloadSize) {
        int sum;
        int tcpLength = TCP_HEADER_SIZE + payloadSize;

        ByteBuffer buffer = ByteBuffer.wrap(ip4Header.sourceAddress.getAddress());
        sum = BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());

        buffer = ByteBuffer.wrap(ip4Header.destinationAddress.getAddress());
        sum += BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());

        sum += IP4Header.TransportProtocol.TCP.getNumber() + tcpLength;

        buffer = backingBuffer.duplicate();

        buffer.putShort(IP4_HEADER_SIZE + 16, (short) 0);

        buffer.position(IP4_HEADER_SIZE);
        while (tcpLength > 1) {
            sum += BitUtils.getUnsignedShort(buffer.getShort());
            tcpLength -= 2;
        }
        if (tcpLength > 0)
            sum += BitUtils.getUnsignedByte(buffer.get()) << 8;

        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;
        tcpHeader.checksum = sum;
        backingBuffer.putShort(IP4_HEADER_SIZE + 16, (short) sum);
    }

    private void fillHeader(ByteBuffer buffer) {
        if (isIP4)
            ip4Header.fillHeader(buffer);
        else if (isIP6)
            ip6Header.fillHeader(buffer);

        if (isUDP)
            udpHeader.fillHeader(buffer);
        else if (isTCP)
            tcpHeader.fillHeader(buffer);
    }


    String getIpVersion() {
        if (isIP4)
            return "IPv4";
        else
            return "IPv6";
    }

    String getTransportType() {
        if (isTCP)
            return "TCP";
        else
            return "UDP";
    }

    String getSourceAddress() {
        if (isIP6)
            return ip6Header.sourceAddress.getHostAddress();
        else
            return ip4Header.sourceAddress.getHostAddress();
    }

    String getDestinationAddress() {
        if (isIP6)
            return ip6Header.destinationAddress.getHostAddress();
        else
            return ip4Header.destinationAddress.getHostAddress();
    }

    int getPayloadSize() {
        return backingBuffer.limit() - backingBuffer.position();
    }

    String getPayload() {
        String payload = "";

        if (backingBuffer.limit() - backingBuffer.position() > 0)
            payload = Base64.encodeToString(backingBuffer.duplicate().array(), 0);

        return payload;
    }


    public static class IP4Header {

        byte version;
        byte IHL;
        int headerLength;
        short typeOfService;
        int totalLength;

        int identificationAndFlagsAndFragmentOffset;

        short TTL;
        private short protocolNum;
        TransportProtocol protocol;
        int headerChecksum;

        InetAddress sourceAddress;
        InetAddress destinationAddress;

        private enum TransportProtocol {
            TCP(6),
            UDP(17),
            Reserved(255);

            private int protocolNumber;

            TransportProtocol(int protocolNumber) {
                this.protocolNumber = protocolNumber;
            }

            private static TransportProtocol numberToEnum(int protocolNumber) {
                if (protocolNumber == 6)
                    return TCP;
                else if (protocolNumber == 17)
                    return UDP;
                else
                    return Reserved;
            }

            public int getNumber() {
                return this.protocolNumber;
            }
        }

        private IP4Header(ByteBuffer buffer) throws UnknownHostException {
            byte versionAndIHL = buffer.get();
            this.version = (byte) (versionAndIHL >> 4);
            this.IHL = (byte) (versionAndIHL & 0x0F);
            this.headerLength = this.IHL << 2;

            this.typeOfService = BitUtils.getUnsignedByte(buffer.get());
            this.totalLength = BitUtils.getUnsignedShort(buffer.getShort());

            this.identificationAndFlagsAndFragmentOffset = buffer.getInt();

            this.TTL = BitUtils.getUnsignedByte(buffer.get());
            this.protocolNum = BitUtils.getUnsignedByte(buffer.get());
            this.protocol = TransportProtocol.numberToEnum(protocolNum);
            this.headerChecksum = BitUtils.getUnsignedShort(buffer.getShort());

            byte[] addressBytes = new byte[4];
            buffer.get(addressBytes, 0, 4);
            this.sourceAddress = InetAddress.getByAddress(addressBytes);

            buffer.get(addressBytes, 0, 4);
            this.destinationAddress = InetAddress.getByAddress(addressBytes);
        }

        void fillHeader(ByteBuffer buffer) {
            buffer.put((byte) (this.version << 4 | this.IHL));
            buffer.put((byte) this.typeOfService);
            buffer.putShort((short) this.totalLength);

            buffer.putInt(this.identificationAndFlagsAndFragmentOffset);

            buffer.put((byte) this.TTL);
            buffer.put((byte) this.protocol.getNumber());
            buffer.putShort((short) this.headerChecksum);

            buffer.put(this.sourceAddress.getAddress());
            buffer.put(this.destinationAddress.getAddress());
        }

        @Override
        public @NonNull String toString() {
            String ip4String = "IP4Header{version=" + version;
            ip4String = ip4String + ", IHL=" + IHL;
            ip4String = ip4String + ", typeOfService=" + typeOfService;
            ip4String = ip4String + ", totalLength=" + totalLength;
            ip4String = ip4String + ", identificationAndFlagsAndFragmentOffset=" + identificationAndFlagsAndFragmentOffset;
            ip4String = ip4String + ", TTL=" + TTL;
            ip4String = ip4String + ", protocol=" + protocolNum + ":" + protocol;
            ip4String = ip4String + ", headerChecksum=" + headerChecksum;
            ip4String = ip4String + ", sourceAddress=" + sourceAddress.getHostAddress();
            ip4String = ip4String + ", destinationAddress=" + destinationAddress.getHostAddress() + "}";

            return ip4String;
        }
    }

    public static class IP6Header {

        private byte[] VTF;
        byte version;
        byte trafficClass;
        int flowLabel;

        short payloadLength;
        byte nextHeader;
        TransportProtocol protocol;
        byte hopLimit;

        private byte[] sourceBytes;
        InetAddress sourceAddress;
        private byte[] destinationBytes;
        InetAddress destinationAddress;

        private enum TransportProtocol {
            TCP(6),
            UDP(17),
            Reserved(255);

            private int protocolNumber;

            TransportProtocol(int protocolNumber) {
                this.protocolNumber = protocolNumber;
            }

            private static TransportProtocol numberToEnum(int protocolNumber) {
                if (protocolNumber == 6)
                    return TCP;
                else if (protocolNumber == 17)
                    return UDP;
                else
                    return Reserved;
            }

            public int getNumber() {
                return this.protocolNumber;
            }
        }

        private IP6Header(ByteBuffer buffer) throws UnknownHostException {
            this.VTF = new byte[4];
            buffer.get(VTF, 0, 4);
            this.version = (byte) (VTF[0] >> 4);
            this.trafficClass = (byte) ((VTF[0] << 4) | (VTF[1] >> 4));
            this.flowLabel = (0x000F & (VTF[1] << 16) | (VTF[2] << 8) | VTF[3]);

            this.payloadLength = buffer.getShort();
            this.nextHeader = buffer.get();
            this.protocol = TransportProtocol.numberToEnum(this.nextHeader);
            this.hopLimit = buffer.get();

            this.sourceBytes = new byte[16];
            buffer.get(sourceBytes, 0, 16);
            this.sourceAddress = Inet6Address.getByAddress(sourceBytes);

            this.destinationBytes = new byte[16];
            buffer.get(destinationBytes, 0, 16);
            this.destinationAddress = Inet6Address.getByAddress(destinationBytes);
        }

        void fillHeader(ByteBuffer buffer) {
            for (byte b : this.VTF) {
                buffer.put(b);
            }
            buffer.putShort(this.payloadLength);
            buffer.put(nextHeader);
            buffer.put(hopLimit);
            for (byte b : this.sourceBytes) {
                buffer.put(b);
            }
            for (byte b : this.destinationBytes) {
                buffer.put(b);
            }
        }

        @Override
        public @NonNull String toString() {
            String ip6String = "IP6Header{version=" + version;
            ip6String = ip6String + ", trafficClass=" + trafficClass;
            ip6String = ip6String + ", flowLabel=" + flowLabel;
            ip6String = ip6String + ", payloadLength=" + payloadLength;
            ip6String = ip6String + ", nextHeader=" + nextHeader + ":" + protocol;
            ip6String = ip6String + ", hopLimit=" + hopLimit;
            ip6String = ip6String + ", sourceAddress=" + sourceAddress.getHostAddress();
            ip6String = ip6String + ", destinationAddress=" + destinationAddress.getHostAddress() + "}";

            return ip6String;
        }
    }

    public static class TCPHeader {

        static final int FIN = 0x01;
        static final int SYN = 0x02;
        static final int RST = 0x04;
        static final int PSH = 0x08;
        static final int ACK = 0x10;
        static final int URG = 0x20;

        int sourcePort;
        int destinationPort;

        long sequenceNumber;
        long acknowledgementNumber;

        byte dataOffsetAndReserved;
        int headerLength;
        byte flags;
        int window;

        int checksum;
        int urgentPointer;

        byte[] optionsAndPadding;

        private TCPHeader(ByteBuffer buffer) {
            this.sourcePort = BitUtils.getUnsignedShort(buffer.getShort());
            this.destinationPort = BitUtils.getUnsignedShort(buffer.getShort());

            this.sequenceNumber = BitUtils.getUnsignedInt(buffer.getInt());
            this.acknowledgementNumber = BitUtils.getUnsignedInt(buffer.getInt());

            this.dataOffsetAndReserved = buffer.get();
            this.headerLength = (this.dataOffsetAndReserved & 0xF0) >> 2;
            this.flags = buffer.get();
            this.window = BitUtils.getUnsignedShort(buffer.getShort());

            this.checksum = BitUtils.getUnsignedShort(buffer.getShort());
            this.urgentPointer = BitUtils.getUnsignedShort(buffer.getShort());

            int optionsLength = this.headerLength - TCP_HEADER_SIZE;
            if (optionsLength > 0) {
                optionsAndPadding = new byte[optionsLength];
                buffer.get(optionsAndPadding, 0, optionsLength);
            }
        }

        boolean isFIN() {
            return (flags & FIN) == FIN;
        }

        boolean isSYN() {
            return (flags & SYN) == SYN;
        }

        boolean isRST() {
            return (flags & RST) == RST;
        }

        boolean isPSH() {
            return (flags & PSH) == PSH;
        }

        boolean isACK() {
            return (flags & ACK) == ACK;
        }

        boolean isURG() {
            return (flags & URG) == URG;
        }

        private void fillHeader(ByteBuffer buffer) {
            buffer.putShort((short) sourcePort);
            buffer.putShort((short) destinationPort);

            buffer.putInt((int) sequenceNumber);
            buffer.putInt((int) acknowledgementNumber);

            buffer.put(dataOffsetAndReserved);
            buffer.put(flags);
            buffer.putShort((short) window);

            buffer.putShort((short) checksum);
            buffer.putShort((short) urgentPointer);
        }

        @Override
        public @NonNull String toString() {
            String tcpString = "TCPHeader{sourcePort=" + sourcePort;
            tcpString = tcpString + ", destinationPort=" + destinationPort;
            tcpString = tcpString + ", sequenceNumber=" + sequenceNumber;
            tcpString = tcpString + ", acknowledgementNumber=" + acknowledgementNumber;
            tcpString = tcpString + ", headerLength=" + headerLength;
            tcpString = tcpString + ", window=" + window;
            tcpString = tcpString + ", checksum=" + checksum;
            tcpString = tcpString + ", flags=";
            if (isFIN()) tcpString = tcpString + " FIN";
            if (isSYN()) tcpString = tcpString + " SYN";
            if (isRST()) tcpString = tcpString + " RST";
            if (isPSH()) tcpString = tcpString + " PSH";
            if (isACK()) tcpString = tcpString + " ACK";
            if (isURG()) tcpString = tcpString + " URG";
            tcpString = tcpString + "}";

            return tcpString;
        }
    }

    public static class UDPHeader {

        int sourcePort;
        int destinationPort;

        int length;
        int checksum;

        private UDPHeader(ByteBuffer buffer) {
            this.sourcePort = BitUtils.getUnsignedShort(buffer.getShort());
            this.destinationPort = BitUtils.getUnsignedShort(buffer.getShort());

            this.length = BitUtils.getUnsignedShort(buffer.getShort());
            this.checksum = BitUtils.getUnsignedShort(buffer.getShort());
        }

        private void fillHeader(ByteBuffer buffer) {
            buffer.putShort((short) this.sourcePort);
            buffer.putShort((short) this.destinationPort);

            buffer.putShort((short) this.length);
            buffer.putShort((short) this.checksum);
        }

        @Override
        public @NonNull String toString() {
            String udpString = "UDPHeader{sourcePort=" + sourcePort;
            udpString = udpString + ", destinationPort=" + destinationPort;
            udpString = udpString + ", length=" + length;
            udpString = udpString + ", checksum=" + checksum + "}";
            udpString = udpString + ", destinationPort=" + destinationPort;
            udpString = udpString + ", destinationPort=" + destinationPort;

            return udpString;
        }
    }

    private static class BitUtils {

        private static short getUnsignedByte(byte value) {
            return (short)(value & 0xFF);
        }

        private static int getUnsignedShort(short value) {
            return value & 0xFFFF;
        }

        private static long getUnsignedInt(int value) {
            return value & 0xFFFFFFFFL;
        }
    }
}