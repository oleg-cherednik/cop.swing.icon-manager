package cop.swing.icoman.imageio.bmp;

import cop.swing.icoman.exceptions.IconManagerException;

import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author Oleg Cherednik
 * @since 01.09.2013
 */
public final class Bitmap {
    public static BufferedImage readImage(ImageInputStream in) throws IOException, IconManagerException {
        BitmapInfoHeader header = new BitmapInfoHeader(in);
        Color[] colorTable = readColorTable(header, in);
        int width = header.getBiWidth();
        int height = header.getBiHeight();
        int bitCount = header.getBiBitCount();

        byte[] mask = readBitMasks(header, in);
        byte[] data = readData(header, in);
        byte[] alpha = create1bitAlpha(header, data);

        if (bitCount == 1)
            return create1bitImage(width, height, data);
        if (bitCount == 4)
            return createImage4(width, height, colorTable, alpha, mask);
        if (bitCount == 8)
            return createImage8(width, height, colorTable, alpha, mask);
        if (bitCount == 24)
            return createImage24(width, height, alpha, mask);
        if (bitCount == 32)
            return create32bitImage(width, height, mask);

        throw new IconManagerException("Bitmap with " + bitCount + "bit is not supported");
    }

    private static Color[] readColorTable(BitmapInfoHeader header, ImageInputStream in) throws IOException {
        int bitCount = header.getBiBitCount();
        int size = bitCount <= 8 ? (int)Math.pow(2.0, bitCount) : 0;

        if (size == 0)
            return null;

        Color[] data = new Color[size];

        for (int i = 0; i < data.length; ++i) {
            int blue = (int)in.readByte() & 255;
            int green = (int)in.readByte() & 255;
            int red = (int)in.readByte() & 255;
            in.skipBytes(1);    // reserved
            data[i] = new Color(red, green, blue);
        }

        return data;
    }

    private static byte[] readBitMasks(BitmapInfoHeader header, ImageInputStream in) throws IOException {
        int width = header.getBiWidth();
        int height = header.getBiHeight();
        int bitCount = header.getBiBitCount();
        byte[] buf = new byte[(width * bitCount + 31) / 32 * 4 * height];

        in.read(buf);

        return buf;
    }

    private static byte[] readData(BitmapInfoHeader header, ImageInputStream in) throws IOException {
        int width = header.getBiWidth();
        int height = header.getBiHeight();
        byte[] buf = new byte[(width + 31) / 32 * 4 * height];

        in.read(buf);

        return buf;
    }

    private static byte[] create1bitAlpha(BitmapInfoHeader header, byte... data) {
        int width = header.getBiWidth();
        int height = header.getBiHeight();
        byte[] buf = new byte[width * height];

        int pos = 0;
        int n4 = data.length / height;
        int n5 = 0;
        int n6 = 0;

        for (int i = 0; i < data.length; ++i) {
            ++n5;
            for (int j = 7; j >= 0; j--) {
                if (n6 >= width)
                    continue;
                buf[pos++] = (byte)((1 << j & data[i]) != 0 ? 0 : -1);
                ++n6;
            }
            if (n5 != n4)
                continue;
            n5 = 0;
            n6 = 0;
        }

        return buf;
    }

    public static BufferedImage create1bitImage(int width, int height, byte[] data) {
        int[] buf = decode1bitArray(width, height, data);
        int[] alpha = create1bitAlpha(width, height, data);
        return createImageNew(width, height, COLORS_2, alpha, buf);
    }

    public static BufferedImage create8bitsImage(int width, int height, byte[] data, byte[] mask) {
        return create8bitsImage(width, height, COLORS_256, data, mask);
    }

    private static BufferedImage create8bitsImage(int width, int height, int[] colors, byte[] data, byte[] mask) {
        int[] buf = decode8bitArray(width, height, data);
        int[] alpha = create1bitAlpha(width, height, mask);
        return createImageNew(width, height, colors, alpha, buf);
    }

    private static int[] decode1bitArray(int width, int height, byte... data) {
        int[] buf = new int[width * height];

        for (int i = 0, offs = 0, x = 0; i < data.length; i++, x = i % 2 == 0 ? 0 : x)
            for (int j = 7; j >= 0; j--, x++)
                if (x < width && offs < buf.length)
                    buf[offs++] = (byte)((1 << j & data[i]) != 0 ? 1 : 0);

        return rotate180(width, height, buf);
    }

    private static int[] decode8bitArray(int width, int height, byte... data) {
        int[] buf = new int[width * height];

        for (int i = 0; i < data.length; i++)
            buf[i] = data[i] & 0xFF;

        return rotate180(width, height, buf);
    }

    private static int[] rotate180(int width, int height, int... data) {
        for (int i = 0; i < height / 2; i++) {
            for (int j = 0; j < width; j++) {
                int a = i * width + j;
                int b = data.length - i * width - width + j;
                int tmp = data[a];
                data[a] = data[b];
                data[b] = tmp;
            }
        }

        return data;
    }

    public static int[] create1bitAlpha(int width, int height, byte... mask) {
        int[] buf = new int[width * height];

        for (int i = 0, offs = 0, x = 0; i < mask.length; i++, x = i % 2 == 0 ? 0 : x)
            for (int j = 7; j >= 0; j--, x++)
                if (x < width && offs < buf.length)
                    buf[offs++] = (byte)((1 << j & mask[i]) != 0 ? 0xFF : 0x0);

        return rotate180(width, height, buf);
    }

    public static int[] create8bitsAlpha(int width, int height, byte... mask) {
        int[] buf = new int[width * height];

        for (int i = 0, offs = 0, x = 0; i < mask.length; i++, x = i % 2 == 0 ? 0 : x)
            for (int j = 7; j >= 0; j--, x++)
                if (x < width && offs < buf.length)
                    buf[offs++] = (byte)((1 << j & mask[i]) != 0 ? 0xFF : 0x0);

        return rotate180(width, height, buf);
    }

    public static void print(int width, int height, int... buf) {
        for (int i = 0, offs = 0; i < height; i++) {
            for (int j = 0; j < width; j++, offs++)
                System.out.print(buf[offs] == 0x0 ? '#' : '.');
            System.out.println();
        }
    }

    private static BufferedImage createImage4(int width, int height, Color[] colors, byte[] alpha, byte... mask) {
        int[] buf = new int[width * height];

        if (mask.length * 2 == buf.length) {
            for (int offsMask = 0, offs = 0; offsMask < mask.length; offsMask++) {
                buf[offs++] = (byte)((mask[offsMask] & 255) >> 4);
                buf[offs++] = (byte)((mask[offsMask] & 255) >> 4 << 4 ^ mask[offsMask] & 255);
            }
        } else {
            int n6 = mask.length * 2 / height - width;

            for (int offsMask = 0, offs = 0, n5 = 0, n8 = 1; offsMask < mask.length; offsMask++) {
                if (n8 != 0)
                    buf[offs++] = (byte)((mask[offsMask] & 255) >> 4);
                if (n8 != 0 && ++n5 == width) {
                    n5 = 0;
                    n8 = 0;
                } else if (n8 == 0 && n5 == n6) {
                    n5 = 0;
                    n8 = 1;
                }

                if (n8 != 0)
                    buf[offs++] = (byte)((mask[offsMask] & 255) >> 4 << 4 ^ mask[offsMask] & 255);
                if (n8 != 0 && ++n5 == width) {
                    n5 = 0;
                    n8 = 0;
                    continue;
                }
                if (n8 != 0 || n5 != n6)
                    continue;
                n5 = 0;
                n8 = 1;
            }
        }

        return createImage(width, height, colors, alpha, buf);
    }

    public static BufferedImage createImage8(int width, int height, Color[] colors, byte[] alpha, byte... mask) {
        int[] buf = new int[width * height];

        if (mask.length == buf.length)
            for (int offs = 0; offs < buf.length; offs++)
                buf[offs] = mask[offs] & 255;
        else {
            int size = mask.length / height - width;

            for (int offsMask = 0, offs = 0, n5 = 0; offsMask < mask.length; offsMask++, offs++) {
                buf[offs] = mask[offsMask] & 255;

                if (++n5 != width)
                    continue;

                n5 = 0;
                offsMask += size;
            }
        }

        return createImage(width, height, colors, alpha, buf);
    }

    private static BufferedImage createImage24(int width, int height, byte[] alpha, byte... mask) {
        int offsMask = 0;
        int offsAlpha = 0;
        int size = mask.length == 3 * height * width ? 0 : mask.length / height - width * 3;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        for (int y = height - 1; y >= 0; --y) {
            for (int x = 0; x < width; ++x) {
                int blue = mask[offsMask++] & 255;
                int green = mask[offsMask++] & 255;
                int red = mask[offsMask++] & 255;
                int rgb = new Color(red, green, blue, alpha[offsAlpha++] & 255).getRGB();
                image.setRGB(x, y, rgb);
            }
            offsMask += size;
        }

        return image;
    }

    public static BufferedImage create32bitImage(int width, int height, byte... mask) {
        int offsMask = 0;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                int blue = mask[offsMask++] & 255;
                int green = mask[offsMask++] & 255;
                int red = mask[offsMask++] & 255;
                int alpha = mask[offsMask++] & 255;
                int rgb = new Color(red, green, blue, alpha).getRGB();
                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }

    public static BufferedImage createImage(int width, int height, Color[] colors, byte[] alpha, int... buf) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        for (int y = height - 1, offs = 0; y >= 0; y--) {
            for (int x = 0; x < width; x++, offs++) {
                int red = colors[buf[offs]].getRed();
                int green = colors[buf[offs]].getGreen();
                int blue = colors[buf[offs]].getBlue();
                int rgb = new Color(red, green, blue, alpha[offs] & 255).getRGB();
                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }

    public static BufferedImage createImageNew(int width, int height, int[] colors, int[] alpha, int... buf) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        for (int y = height - 1, offs = 0; y >= 0; y--)
            for (int x = 0; x < width; x++, offs++)
                image.setRGB(x, y, rgb(colors[buf[offs]], alpha[offs]));

        return image;
    }

    private static int rgb(Color color, int alpha) {
        return rgb(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static int rgb(int rgb, int alpha) {
        return rgb(rgb >> 16, rgb >> 8, rgb, alpha);
    }

    private static int rgb(int red, int green, int blue) {
        return rgb(red, green, blue, 0xFF);
    }

    private static int rgb(int red, int green, int blue, int alpha) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public static final int[] COLORS_2 = {
            rgb(0xFF, 0xFF, 0xFF),
            rgb(0x0, 0x0, 0x0)
    };

    public static final Color[] COLORS_16 = {
            new Color(0, 0, 0),
            new Color(128, 0, 0),
            new Color(0, 128, 0),
            new Color(128, 128, 0),
            new Color(0, 0, 128),
            new Color(128, 0, 128),
            new Color(0, 128, 128),
            new Color(128, 128, 128),
            new Color(255, 0, 0),
            new Color(0, 255, 0),
            new Color(255, 255, 0),
            new Color(0, 0, 255),
            new Color(255, 0, 255),
            new Color(0, 255, 255),
            new Color(192, 192, 192),
            new Color(255, 255, 255)
    };

    private static final int COLORS_256[] = {
            rgb(0xFF, 0xFF, 0xFF),
            rgb(0xFF, 0xFF, 0xCC),
            rgb(0xFF, 0xFF, 0x99),
            rgb(0xFF, 0xFF, 0x66),
            rgb(0xFF, 0xFF, 0x33),
            rgb(0xFF, 0xFF, 0x00),
            rgb(0xFF, 0xCC, 0xFF),
            rgb(0xFF, 0xCC, 0xCC),
            rgb(0xFF, 0xCC, 0x99),
            rgb(0xFF, 0xCC, 0x66),
            rgb(0xFF, 0xCC, 0x33),
            rgb(0xFF, 0xCC, 0x00),
            rgb(0xFF, 0x99, 0xFF),
            rgb(0xFF, 0x99, 0xCC),
            rgb(0xFF, 0x99, 0x99),
            rgb(0xFF, 0x99, 0x66),
            rgb(0xFF, 0x99, 0x33),
            rgb(0xFF, 0x99, 0x00),
            rgb(0xFF, 0x66, 0xFF),
            rgb(0xFF, 0x66, 0xCC),
            rgb(0xFF, 0x66, 0x99),
            rgb(0xFF, 0x66, 0x66),
            rgb(0xFF, 0x66, 0x33),
            rgb(0xFF, 0x66, 0x00),
            rgb(0xFF, 0x33, 0xFF),
            rgb(0xFF, 0x33, 0xCC),
            rgb(0xFF, 0x33, 0x99),
            rgb(0xFF, 0x33, 0x66),
            rgb(0xFF, 0x33, 0x33),
            rgb(0xFF, 0x33, 0x00),
            rgb(0xFF, 0x00, 0xFF),
            rgb(0xFF, 0x00, 0xCC),
            rgb(0xFF, 0x00, 0x99),
            rgb(0xFF, 0x00, 0x66),
            rgb(0xFF, 0x00, 0x33),
            rgb(0xFF, 0x00, 0x00),
            rgb(0xCC, 0xFF, 0xFF),
            rgb(0xCC, 0xFF, 0xCC),
            rgb(0xCC, 0xFF, 0x99),
            rgb(0xCC, 0xFF, 0x66),
            rgb(0xCC, 0xFF, 0x33),
            rgb(0xCC, 0xFF, 0x00),
            rgb(0xCC, 0xCC, 0xFF),
            rgb(0xCC, 0xCC, 0xCC),
            rgb(0xCC, 0xCC, 0x99),
            rgb(0xCC, 0xCC, 0x66),
            rgb(0xCC, 0xCC, 0x33),
            rgb(0xCC, 0xCC, 0x00),
            rgb(0xCC, 0x99, 0xFF),
            rgb(0xCC, 0x99, 0xCC),
            rgb(0xCC, 0x99, 0x99),
            rgb(0xCC, 0x99, 0x66),
            rgb(0xCC, 0x99, 0x33),
            rgb(0xCC, 0x99, 0x00),
            rgb(0xCC, 0x66, 0xFF),
            rgb(0xCC, 0x66, 0xCC),
            rgb(0xCC, 0x66, 0x99),
            rgb(0xCC, 0x66, 0x66),
            rgb(0xCC, 0x66, 0x33),
            rgb(0xCC, 0x66, 0x00),
            rgb(0xCC, 0x33, 0xFF),
            rgb(0xCC, 0x33, 0xCC),
            rgb(0xCC, 0x33, 0x99),
            rgb(0xCC, 0x33, 0x66),
            rgb(0xCC, 0x33, 0x33),
            rgb(0xCC, 0x33, 0x00),
            rgb(0xCC, 0x00, 0xFF),
            rgb(0xCC, 0x00, 0xCC),
            rgb(0xCC, 0x00, 0x99),
            rgb(0xCC, 0x00, 0x66),
            rgb(0xCC, 0x00, 0x33),
            rgb(0xCC, 0x00, 0x00),
            rgb(0x99, 0xFF, 0xFF),
            rgb(0x99, 0xFF, 0xCC),
            rgb(0x99, 0xFF, 0x99),
            rgb(0x99, 0xFF, 0x66),
            rgb(0x99, 0xFF, 0x33),
            rgb(0x99, 0xFF, 0x00),
            rgb(0x99, 0xCC, 0xFF),
            rgb(0x99, 0xCC, 0xCC),
            rgb(0x99, 0xCC, 0x99),
            rgb(0x99, 0xCC, 0x66),
            rgb(0x99, 0xCC, 0x33),
            rgb(0x99, 0xCC, 0x00),
            rgb(0x99, 0x99, 0xFF),
            rgb(0x99, 0x99, 0xCC),
            rgb(0x99, 0x99, 0x99),
            rgb(0x99, 0x99, 0x66),
            rgb(0x99, 0x99, 0x33),
            rgb(0x99, 0x99, 0x00),
            rgb(0x99, 0x66, 0xFF),
            rgb(0x99, 0x66, 0xCC),
            rgb(0x99, 0x66, 0x99),
            rgb(0x99, 0x66, 0x66),
            rgb(0x99, 0x66, 0x33),
            rgb(0x99, 0x66, 0x00),
            rgb(0x99, 0x33, 0xFF),
            rgb(0x99, 0x33, 0xCC),
            rgb(0x99, 0x33, 0x99),
            rgb(0x99, 0x33, 0x66),
            rgb(0x99, 0x33, 0x33),
            rgb(0x99, 0x33, 0x00),
            rgb(0x99, 0x00, 0xFF),
            rgb(0x99, 0x00, 0xCC),
            rgb(0x99, 0x00, 0x99),
            rgb(0x99, 0x00, 0x66),
            rgb(0x99, 0x00, 0x33),
            rgb(0x99, 0x00, 0x00),
            rgb(0x66, 0xFF, 0xFF),
            rgb(0x66, 0xFF, 0xCC),
            rgb(0x66, 0xFF, 0x99),
            rgb(0x66, 0xFF, 0x66),
            rgb(0x66, 0xFF, 0x33),
            rgb(0x66, 0xFF, 0x00),
            rgb(0x66, 0xCC, 0xFF),
            rgb(0x66, 0xCC, 0xCC),
            rgb(0x66, 0xCC, 0x99),
            rgb(0x66, 0xCC, 0x66),
            rgb(0x66, 0xCC, 0x33),
            rgb(0x66, 0xCC, 0x00),
            rgb(0x66, 0x99, 0xFF),
            rgb(0x66, 0x99, 0xCC),
            rgb(0x66, 0x99, 0x99),
            rgb(0x66, 0x99, 0x66),
            rgb(0x66, 0x99, 0x33),
            rgb(0x66, 0x99, 0x00),
            rgb(0x66, 0x66, 0xFF),
            rgb(0x66, 0x66, 0xCC),
            rgb(0x66, 0x66, 0x99),
            rgb(0x66, 0x66, 0x66),
            rgb(0x66, 0x66, 0x33),
            rgb(0x66, 0x66, 0x00),
            rgb(0x66, 0x33, 0xFF),
            rgb(0x66, 0x33, 0xCC),
            rgb(0x66, 0x33, 0x99),
            rgb(0x66, 0x33, 0x66),
            rgb(0x66, 0x33, 0x33),
            rgb(0x66, 0x33, 0x00),
            rgb(0x66, 0x00, 0xFF),
            rgb(0x66, 0x00, 0xCC),
            rgb(0x66, 0x00, 0x99),
            rgb(0x66, 0x00, 0x66),
            rgb(0x66, 0x00, 0x33),
            rgb(0x66, 0x00, 0x00),
            rgb(0x33, 0xFF, 0xFF),
            rgb(0x33, 0xFF, 0xCC),
            rgb(0x33, 0xFF, 0x99),
            rgb(0x33, 0xFF, 0x66),
            rgb(0x33, 0xFF, 0x33),
            rgb(0x33, 0xFF, 0x00),
            rgb(0x33, 0xCC, 0xFF),
            rgb(0x33, 0xCC, 0xCC),
            rgb(0x33, 0xCC, 0x99),
            rgb(0x33, 0xCC, 0x66),
            rgb(0x33, 0xCC, 0x33),
            rgb(0x33, 0xCC, 0x00),
            rgb(0x33, 0x99, 0xFF),
            rgb(0x33, 0x99, 0xCC),
            rgb(0x33, 0x99, 0x99),
            rgb(0x33, 0x99, 0x66),
            rgb(0x33, 0x99, 0x33),
            rgb(0x33, 0x99, 0x00),
            rgb(0x33, 0x66, 0xFF),
            rgb(0x33, 0x66, 0xCC),
            rgb(0x33, 0x66, 0x99),
            rgb(0x33, 0x66, 0x66),
            rgb(0x33, 0x66, 0x33),
            rgb(0x33, 0x66, 0x00),
            rgb(0x33, 0x33, 0xFF),
            rgb(0x33, 0x33, 0xCC),
            rgb(0x33, 0x33, 0x99),
            rgb(0x33, 0x33, 0x66),
            rgb(0x33, 0x33, 0x33),
            rgb(0x33, 0x33, 0x00),
            rgb(0x33, 0x00, 0xFF),
            rgb(0x33, 0x00, 0xCC),
            rgb(0x33, 0x00, 0x99),
            rgb(0x33, 0x00, 0x66),
            rgb(0x33, 0x00, 0x33),
            rgb(0x33, 0x00, 0x00),
            rgb(0x00, 0xFF, 0xFF),
            rgb(0x00, 0xFF, 0xCC),
            rgb(0x00, 0xFF, 0x99),
            rgb(0x00, 0xFF, 0x66),
            rgb(0x00, 0xFF, 0x33),
            rgb(0x00, 0xFF, 0x00),
            rgb(0x00, 0xCC, 0xFF),
            rgb(0x00, 0xCC, 0xCC),
            rgb(0x00, 0xCC, 0x99),
            rgb(0x00, 0xCC, 0x66),
            rgb(0x00, 0xCC, 0x33),
            rgb(0x00, 0xCC, 0x00),
            rgb(0x00, 0x99, 0xFF),
            rgb(0x00, 0x99, 0xCC),
            rgb(0x00, 0x99, 0x99),
            rgb(0x00, 0x99, 0x66),
            rgb(0x00, 0x99, 0x33),
            rgb(0x00, 0x99, 0x00),
            rgb(0x00, 0x66, 0xFF),
            rgb(0x00, 0x66, 0xCC),
            rgb(0x00, 0x66, 0x99),
            rgb(0x00, 0x66, 0x66),
            rgb(0x00, 0x66, 0x33),
            rgb(0x00, 0x66, 0x00),
            rgb(0x00, 0x33, 0xFF),
            rgb(0x00, 0x33, 0xCC),
            rgb(0x00, 0x33, 0x99),
            rgb(0x00, 0x33, 0x66),
            rgb(0x00, 0x33, 0x33),
            rgb(0x00, 0x33, 0x00),
            rgb(0x00, 0x00, 0xFF),
            rgb(0x00, 0x00, 0xCC),
            rgb(0x00, 0x00, 0x99),
            rgb(0x00, 0x00, 0x66),
            rgb(0x00, 0x00, 0x33),
            rgb(0xEE, 0x00, 0x00),
            rgb(0xDD, 0x00, 0x00),
            rgb(0xBB, 0x00, 0x00),
            rgb(0xAA, 0x00, 0x00),
            rgb(0x88, 0x00, 0x00),
            rgb(0x77, 0x00, 0x00),
            rgb(0x55, 0x00, 0x00),
            rgb(0x44, 0x00, 0x00),
            rgb(0x22, 0x00, 0x00),
            rgb(0x11, 0x00, 0x00),
            rgb(0x00, 0xEE, 0x00),
            rgb(0x00, 0xDD, 0x00),
            rgb(0x00, 0xBB, 0x00),
            rgb(0x00, 0xAA, 0x00),
            rgb(0x00, 0x88, 0x00),
            rgb(0x00, 0x77, 0x00),
            rgb(0x00, 0x55, 0x00),
            rgb(0x00, 0x44, 0x00),
            rgb(0x00, 0x22, 0x00),
            rgb(0x00, 0x11, 0x00),
            rgb(0x00, 0x00, 0xEE),
            rgb(0x00, 0x00, 0xDD),
            rgb(0x00, 0x00, 0xBB),
            rgb(0x00, 0x00, 0xAA),
            rgb(0x00, 0x00, 0x88),
            rgb(0x00, 0x00, 0x77),
            rgb(0x00, 0x00, 0x55),
            rgb(0x00, 0x00, 0x44),
            rgb(0x00, 0x00, 0x22),
            rgb(0x00, 0x00, 0x11),
            rgb(0xEE, 0xEE, 0xEE),
            rgb(0xDD, 0xDD, 0xDD),
            rgb(0xBB, 0xBB, 0xBB),
            rgb(0xAA, 0xAA, 0xAA),
            rgb(0x88, 0x88, 0x88),
            rgb(0x77, 0x77, 0x77),
            rgb(0x55, 0x55, 0x55),
            rgb(0x44, 0x44, 0x44),
            rgb(0x22, 0x22, 0x22),
            rgb(0x11, 0x11, 0x11),
            rgb(0x00, 0x00, 0x00)
    };

    private Bitmap() {
    }
}
