package ru.olegcherednik.icoman.ico.imageio;

import ru.olegcherednik.icoman.imageio.IconReader;
import ru.olegcherednik.icoman.imageio.IconReaderSpi;
import ru.olegcherednik.icoman.imageio.bmp.IconBitmapReaderSpi;

import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * This is spi for ico files as one file
 *
 * @author Oleg Cherednik
 * @since 15.08.2015
 */
public final class IcoReaderSpi extends IconReaderSpi {
    private static final IcoReaderSpi INSTANCE = new IcoReaderSpi();

    private IcoReaderSpi() {
    }

    // ========== IconFileReaderSpi ==========

    @Override
    public IconReader createReaderInstance(Object extension) {
        return new IcoReader(this);
    }

    @Override
    public boolean canDecodeInput(ImageInputStream in) throws IOException {
        return canDecodeInput(in, () -> isHeaderValid(in.readInt()));
    }

    // ========== static ==========

    public static synchronized void register() {
        IconBitmapReaderSpi.register();
        IIORegistry.getDefaultInstance().registerServiceProvider(INSTANCE);
    }

    public static boolean isHeaderValid(int marker) {
        return marker == 0x100;
    }
}
