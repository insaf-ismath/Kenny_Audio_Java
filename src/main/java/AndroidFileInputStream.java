import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by KennyChoo on 3/5/17.
 */
public class AndroidFileInputStream implements TarsosDSPAudioInputStream {

    private final FileInputStream underlyingStream;
    private final TarsosDSPAudioFormat format;

    public AndroidFileInputStream(FileInputStream underlyingStream, TarsosDSPAudioFormat format) throws IOException {
        this.underlyingStream = underlyingStream;
        this.format = format;
    }

    @Override
    public long skip(long bytesToSkip) throws IOException {
        return underlyingStream.skip(bytesToSkip);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return underlyingStream.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        underlyingStream.close();
    }

    @Override
    public TarsosDSPAudioFormat getFormat() {
        return format;
    }

    /**
     * This bit does not seem implemented
     *
     * @return always -1, so it's useles!
     */
    @Override
    public long getFrameLength() {
        return -1;
    }
}