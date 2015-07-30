package com.ucware.icontools;

import com.ucware.coff.A.IconFileImage;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class IconImage implements Icon, Serializable {
    private static final long serialVersionUID = 2276680104695453325L;

    private final BufferedImage image;
    private final int height;
    private final int width;
    private final int bitCount;

    public IconImage(IconFileImage iconImage) {
        image = getImage(iconImage);

        IconBitmap iconBitmap = new IconBitmap(iconImage);
        height = iconBitmap.getBitmap().getBitmapInfoHeader().getHeight();
        width = iconBitmap.getBitmap().getBitmapInfoHeader().getWidth();
        bitCount = iconBitmap.getBitmap().getBitmapInfoHeader().getField("biBitCount").readInt();


//        int n;
//        B b = new B(iconImage);
//        height = b.D().getHeader().getHeight();
//        width = b.D().getHeader().getWidth();
//        byte[] arrby = new byte[width * height];
//        byte[] arrby2 = b.C();
//        int n2 = 0;
//        int n3 = width;
//        int n4 = arrby2.length / height;
//        int n5 = 0;
//        int n6 = 0;
//        for (int i = 0; i < arrby2.length; ++i) {
//            ++n5;
//            for (n = 7; n >= 0; --n) {
//                if (n6 >= n3) continue;
//                arrby[n2++] = (byte)((1 << n & arrby2[i]) != 0 ? 0 : -1);
//                ++n6;
//            }
//            if (n5 != n4) continue;
//            n5 = 0;
//            n6 = 0;
//        }
//        n = 0;
//        byte[] arrby3 = b.B();
//        bitCount = b.D().getHeader().getField("biBitCount").readInt();
//        BufferedImage bufferedImage = new BufferedImage(width, height, 6);
//        if (bitCount <= 8) {
//            int n7;
//            int n8;
//            int[] arrn = new int[width * height];
//            if (bitCount == 8) {
//                if (arrby3.length == arrn.length) {
//                    for (n8 = 0; n8 < arrn.length; ++n8) {
//                        arrn[n8] = arrby3[n8] & 255;
//                    }
//                } else {
//                    n2 = 0;
//                    n4 = n3;
//                    n5 = 0;
//                    n6 = arrby3.length / height - width;
//                    for (n8 = 0; n8 < arrby3.length; ++n8) {
//                        arrn[n2++] = arrby3[n8] & 255;
//                        if (++n5 != n4) continue;
//                        n5 = 0;
//                        n8 += n6;
//                    }
//                }
//            } else if (bitCount == 4) {
//                n = 0;
//                if (arrby3.length * 2 == arrn.length) {
//                    for (n8 = 0; n8 < arrby3.length; ++n8) {
//                        arrn[n++] = (arrby3[n8] & 255) >> 4;
//                        arrn[n++] = (arrby3[n8] & 255) >> 4 << 4 ^ arrby3[n8] & 255;
//                    }
//                } else {
//                    n2 = 0;
//                    n4 = n3;
//                    n5 = 0;
//                    n6 = arrby3.length * 2 / height - width;
//                    n8 = 1;
//                    for (n7 = 0; n7 < arrby3.length; ++n7) {
//                        if (n8 != 0) {
//                            arrn[n++] = (arrby3[n7] & 255) >> 4;
//                        }
//                        if (n8 != 0 && ++n5 == n4) {
//                            n5 = 0;
//                            n8 = 0;
//                        } else if (n8 == 0 && n5 == n6) {
//                            n5 = 0;
//                            n8 = 1;
//                        }
//                        if (n8 != 0) {
//                            arrn[n++] = (arrby3[n7] & 255) >> 4 << 4 ^ arrby3[n7] & 255;
//                        }
//                        if (n8 != 0 && ++n5 == n4) {
//                            n5 = 0;
//                            n8 = 0;
//                            continue;
//                        }
//                        if (n8 != 0 || n5 != n6) continue;
//                        n5 = 0;
//                        n8 = 1;
//                    }
//                }
//            } else if (bitCount == 1) {
//                n2 = 0;
//                n3 = width;
//                n4 = arrby2.length / height;
//                n5 = 0;
//                n6 = 0;
//                for (n8 = 0; n8 < arrby3.length; ++n8) {
//                    ++n5;
//                    for (n7 = 7; n7 >= 0; --n7) {
//                        if (n6 >= n3) continue;
//                        arrn[n2++] = (1 << n7 & arrby3[n8]) != 0 ? 1 : 0;
//                        ++n6;
//                    }
//                    if (n5 != n4) continue;
//                    n5 = 0;
//                    n6 = 0;
//                }
//            }
//            n = 0;
//            A[] arra = b.D().getData();
//            for (n7 = height - 1; n7 >= 0; --n7) {
//                for (int j = 0; j < width; ++j) {
//                    Color color = new Color(arra[arrn[n]].getField("rgbRed").readInt(), arra[arrn[n]].getField("rgbGreen").readInt(),
//                            arra[arrn[n]].getField(
//                                    "rgbBlue").readInt(),
//                            arrby[n] & 255);
//                    bufferedImage.setRGB(j, n7, color.getRGB());
//                    ++n;
//                }
//            }
//        } else {
//            n = 0;
//            if (bitCount == 24) {
//                int n9 = 0;
//                n6 = arrby3.length == 3 * height * width ? 0 : arrby3.length / height - width * 3;
//                for (int j = height - 1; j >= 0; --j) {
//                    for (int k = 0; k < width; ++k) {
//                        int n10 = arrby3[n++] & 255;
//                        int n11 = arrby3[n++] & 255;
//                        int n12 = arrby3[n++] & 255;
//                        Color color = new Color(n12, n11, n10, arrby[n9++] & 255);
//                        bufferedImage.setRGB(k, j, color.getRGB());
//                    }
//                    n += n6;
//                }
//            } else if (bitCount == 32) {
//                for (int j = height - 1; j >= 0; --j) {
//                    for (int k = 0; k < width; ++k) {
//                        int n13 = arrby3[n++] & 255;
//                        int n14 = arrby3[n++] & 255;
//                        int n15 = arrby3[n++] & 255;
//                        int n16 = arrby3[n++] & 255;
//                        Color color = new Color(n15, n14, n13, n16);
//                        bufferedImage.setRGB(k, j, color.getRGB());
//                    }
//                }
//            }
//        }
//        image = bufferedImage;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    public int getBitCount() {
        return bitCount;
    }

    @Override
    public void paintIcon(Component component, Graphics g, int n, int n2) {
        g.drawImage(image, n, n2, component);
    }

    private static BufferedImage getImage(IconFileImage iconImage) {
        int n;
        IconBitmap iconBitmap = new IconBitmap(iconImage);
        int height = iconBitmap.getBitmap().getBitmapInfoHeader().getHeight();
        int width = iconBitmap.getBitmap().getBitmapInfoHeader().getWidth();
        int bitCount = iconBitmap.getBitmap().getBitmapInfoHeader().getField("biBitCount").readInt();
        byte[] buf = new byte[width * height];
        byte[] colorTable = iconBitmap.getColorTable();
        int n2 = 0;
        int n3 = width;
        int n4 = colorTable.length / height;
        int n5 = 0;
        int n6 = 0;
        for (int i = 0; i < colorTable.length; ++i) {
            ++n5;
            for (n = 7; n >= 0; --n) {
                if (n6 >= n3) continue;
                buf[n2++] = (byte)((1 << n & colorTable[i]) != 0 ? 0 : -1);
                ++n6;
            }
            if (n5 != n4) continue;
            n5 = 0;
            n6 = 0;
        }
        n = 0;
        byte[] bitMasks = iconBitmap.getBitMasks();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        if (bitCount <= 8) {
            int n7;
            int n8;
            int[] arrn = new int[width * height];
            if (bitCount == 8) {
                if (bitMasks.length == arrn.length) {
                    for (n8 = 0; n8 < arrn.length; ++n8) {
                        arrn[n8] = bitMasks[n8] & 255;
                    }
                } else {
                    n2 = 0;
                    n4 = n3;
                    n5 = 0;
                    n6 = bitMasks.length / height - width;
                    for (n8 = 0; n8 < bitMasks.length; ++n8) {
                        arrn[n2++] = bitMasks[n8] & 255;
                        if (++n5 != n4) continue;
                        n5 = 0;
                        n8 += n6;
                    }
                }
            } else if (bitCount == 4) {
                n = 0;
                if (bitMasks.length * 2 == arrn.length) {
                    for (n8 = 0; n8 < bitMasks.length; ++n8) {
                        arrn[n++] = (bitMasks[n8] & 255) >> 4;
                        arrn[n++] = (bitMasks[n8] & 255) >> 4 << 4 ^ bitMasks[n8] & 255;
                    }
                } else {
                    n2 = 0;
                    n4 = n3;
                    n5 = 0;
                    n6 = bitMasks.length * 2 / height - width;
                    n8 = 1;
                    for (n7 = 0; n7 < bitMasks.length; ++n7) {
                        if (n8 != 0) {
                            arrn[n++] = (bitMasks[n7] & 255) >> 4;
                        }
                        if (n8 != 0 && ++n5 == n4) {
                            n5 = 0;
                            n8 = 0;
                        } else if (n8 == 0 && n5 == n6) {
                            n5 = 0;
                            n8 = 1;
                        }
                        if (n8 != 0) {
                            arrn[n++] = (bitMasks[n7] & 255) >> 4 << 4 ^ bitMasks[n7] & 255;
                        }
                        if (n8 != 0 && ++n5 == n4) {
                            n5 = 0;
                            n8 = 0;
                            continue;
                        }
                        if (n8 != 0 || n5 != n6) continue;
                        n5 = 0;
                        n8 = 1;
                    }
                }
            } else if (bitCount == 1) {
                n2 = 0;
                n3 = width;
                n4 = colorTable.length / height;
                n5 = 0;
                n6 = 0;
                for (n8 = 0; n8 < bitMasks.length; ++n8) {
                    ++n5;
                    for (n7 = 7; n7 >= 0; --n7) {
                        if (n6 >= n3) continue;
                        arrn[n2++] = (1 << n7 & bitMasks[n8]) != 0 ? 1 : 0;
                        ++n6;
                    }
                    if (n5 != n4) continue;
                    n5 = 0;
                    n6 = 0;
                }
            }
            n = 0;
            BitMask[] arra = iconBitmap.getBitmap().getData();
            for (n7 = height - 1; n7 >= 0; --n7) {
                for (int j = 0; j < width; ++j) {
                    Color color = new Color(arra[arrn[n]].getField("rgbRed").readInt(), arra[arrn[n]].getField("rgbGreen").readInt(),
                            arra[arrn[n]].getField(
                                    "rgbBlue").readInt(),
                            buf[n] & 255);
                    bufferedImage.setRGB(j, n7, color.getRGB());
                    ++n;
                }
            }
        } else {
            n = 0;
            if (bitCount == 24) {
                int n9 = 0;
                n6 = bitMasks.length == 3 * height * width ? 0 : bitMasks.length / height - width * 3;
                for (int j = height - 1; j >= 0; --j) {
                    for (int k = 0; k < width; ++k) {
                        int n10 = bitMasks[n++] & 255;
                        int n11 = bitMasks[n++] & 255;
                        int n12 = bitMasks[n++] & 255;
                        Color color = new Color(n12, n11, n10, buf[n9++] & 255);
                        bufferedImage.setRGB(k, j, color.getRGB());
                    }
                    n += n6;
                }
            } else if (bitCount == 32) {
                for (int j = height - 1; j >= 0; --j) {
                    for (int k = 0; k < width; ++k) {
                        int n13 = bitMasks[n++] & 255;
                        int n14 = bitMasks[n++] & 255;
                        int n15 = bitMasks[n++] & 255;
                        int n16 = bitMasks[n++] & 255;
                        Color color = new Color(n15, n14, n13, n16);
                        bufferedImage.setRGB(k, j, color.getRGB());
                    }
                }
            }
        }

        return bufferedImage;
    }
}
