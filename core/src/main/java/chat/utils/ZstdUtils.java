package chat.utils;

import com.github.luben.zstd.Zstd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ZstdUtils {
    private static final int ZSTD_COMPRESSION_LEVEL = 3;

    public byte[] compressByte(byte[] unCompInput) {
        return Zstd.compress(unCompInput, ZSTD_COMPRESSION_LEVEL);
    }
    public int compress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset, int maxOutputLength)
    {
        byte[] uncompressed = Arrays.copyOfRange(input, inputOffset, inputLength);
        byte[] compressed = Zstd.compress(uncompressed, ZSTD_COMPRESSION_LEVEL);
        System.arraycopy(compressed, 0, output, outputOffset, compressed.length);
        return compressed.length;
    }
    public ByteBuf encode(ByteBuf source) {
        int uncompressedLength = source.readableBytes();
        int maxLength = (int) Zstd.compressBound(uncompressedLength);
        ByteBuf target = PooledByteBufAllocator.DEFAULT.directBuffer(maxLength, maxLength);
        int compressedLength;
        if (source.hasMemoryAddress()) {
            compressedLength = (int) Zstd.compressUnsafe(target.memoryAddress(), maxLength,
                    source.memoryAddress() + source.readerIndex(),
                    uncompressedLength, ZSTD_COMPRESSION_LEVEL);
        } else {
            ByteBuffer sourceNio = source.nioBuffer(source.readerIndex(), source.readableBytes());
            ByteBuffer targetNio = target.nioBuffer(0, maxLength);
            compressedLength = Zstd.compress(targetNio, sourceNio, ZSTD_COMPRESSION_LEVEL);
        }
        target.writerIndex(compressedLength);
        return target;
    }
    public ByteBuf decode(ByteBuf encoded, int uncompressedLength) throws IOException {
        ByteBuf uncompressed = PooledByteBufAllocator.DEFAULT.directBuffer(uncompressedLength, uncompressedLength);

        if (encoded.hasMemoryAddress()) {
            Zstd.decompressUnsafe(uncompressed.memoryAddress(), uncompressedLength,
                    encoded.memoryAddress() + encoded.readerIndex(),
                    encoded.readableBytes());
        } else {
            ByteBuffer uncompressedNio = uncompressed.nioBuffer(0, uncompressedLength);
            ByteBuffer encodedNio = encoded.nioBuffer(encoded.readerIndex(), encoded.readableBytes());

            Zstd.decompress(uncompressedNio, encodedNio);
        }

        uncompressed.writerIndex(uncompressedLength);
        return uncompressed;
    }

}
