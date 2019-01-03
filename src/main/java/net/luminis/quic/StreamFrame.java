package net.luminis.quic;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static net.luminis.quic.StreamType.ClientInitiatedBidirectional;

public class StreamFrame extends QuicFrame {

    private StreamType streamType;
    private int streamId;
    private int offset;
    private int length;
    private byte[] streamData;
    private boolean isFinal;
    private byte[] frameData;

    public StreamFrame() {
    }

    public StreamFrame(int streamId, byte[] applicationData, boolean fin) {
        this(streamId, 0, applicationData, fin);
    }

    public StreamFrame(int streamId, int streamOffset, byte[] applicationData, boolean fin) {
        streamType = ClientInitiatedBidirectional;
        this.streamId = streamId;
        this.offset = streamOffset;
        this.length = applicationData.length;
        streamData = applicationData;
        isFinal = fin;

        ByteBuffer buffer = ByteBuffer.allocate(1 + 3 * 4 + applicationData.length);
        byte frameType = 0x10 | 0x04 | 0x02 | 0x00;  // OFF-bit, LEN-bit, (no) FIN-bit
        if (fin) {
            frameType |= 0x01;
        }
        buffer.put(frameType);
        buffer.put(encodeVariableLengthInteger(streamId));
        buffer.put(encodeVariableLengthInteger(offset));  // offset
        buffer.put(encodeVariableLengthInteger(applicationData.length));  // length
        buffer.put(applicationData);

        frameData = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(frameData);
    }

    public StreamFrame parse(ByteBuffer buffer, Logger log) {
        int frameType = buffer.get();
        boolean withOffset = ((frameType & 0x04) == 0x04);
        boolean withLength = ((frameType & 0x02) == 0x02);
        isFinal = ((frameType & 0x01) == 0x01);

        streamId = QuicPacket.parseVariableLengthInteger(buffer);
        streamType = Stream.of(StreamType.values()).filter(t -> t.value == (streamId & 0x03)).findFirst().get();

        if (withOffset) {
            offset = QuicPacket.parseVariableLengthInteger(buffer);
        }
        if (withLength) {
            length = QuicPacket.parseVariableLengthInteger(buffer);
        }

        if (length > 0) {
            length = buffer.limit() - buffer.position();
        }
        streamData = new byte[length];
        buffer.get(streamData);
        log.info("Stream data", streamData);

        return this;
    }

    @Override
    byte[] getBytes() {
        return frameData;
    }

    @Override
    public String toString() {
        return "StreamFrame[" + streamId + "(" + streamType.abbrev + ")" + "," + offset + "," + length + "]";
    }

    public int getStreamId() {
        return streamId;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public byte[] getStreamData() {
        return streamData;
    }

    public boolean isFinal() {
        return isFinal;
    }


}