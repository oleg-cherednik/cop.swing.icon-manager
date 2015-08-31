package cop.swing.icoman.imageio.bmp;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author Oleg Cherednik
 * @since 31.08.2015
 */
final class Bitmap1Bit extends Bitmap {
    public static final Bitmap1Bit INSTANCE = new Bitmap1Bit();

    private Bitmap1Bit() {
    }

    // ========== Bitmap ==========

    @Override
    public BufferedImage createImage(int width, int height, int[] colors, ImageInputStream in, boolean inv) throws IOException {
        byte[] data = read32bitDataBlocks(width, Math.abs(height), 1, in);
        byte[] mask = read32bitMaskBlocks(width, Math.abs(height), in);
        return createImage(width, height, colors, data, mask, false);
    }

    @Override
    public BufferedImage createImage(int width, int height, int[] colors, byte[] data, byte[] mask, boolean inv) {
        int[] buf = decode(width, height, data);
        int[] alpha = alpha(width, height, mask, inv);
        return createImage(width, height, colors, alpha, buf);
    }

    // ========== static ==========

    private static int[] decode(int width, int height, byte[] data) {
        int[] buf = new int[Math.abs(width * height)];

        for (int i = 0, offs = 0, x = 0; i < data.length; i++, x = i % 2 == 0 ? 0 : x)
            for (int j = 7; j >= 0; j--, x++)
                if (x < width && offs < buf.length)
                    buf[offs++] = (0x1 << j & data[i]) != 0 ? 1 : 0;

        return height > 0 ? flipVertical(width, height, buf) : buf;
    }

    static int[] alpha(int width, int height, byte[] mask, boolean inv) {
        int[] buf = new int[Math.abs(width * height)];

        for (int i = 0, offs = 0, x = 0; i < mask.length; i++, x = i % 2 == 0 ? 0 : x)
            for (int j = 7; j >= 0; j--, x++)
                if (x < width && offs < buf.length)
                    buf[offs++] = (1 << j & mask[i]) != 0 ? inv ? 0xFF : 0x0 : inv ? 0x0 : 0xFF;

        return height > 0 ? flipVertical(width, height, buf) : buf;
    }
}
