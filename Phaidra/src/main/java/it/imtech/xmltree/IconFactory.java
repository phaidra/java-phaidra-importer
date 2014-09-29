package it.imtech.xmltree;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.media.jai.PlanarImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.apache.log4j.Logger;

/**
 *
 * @author luigi
 */
public class IconFactory {
    private final static Logger logger = Logger.getLogger(IconFactory.class.getName());
    private static final double SCALE_FACTOR = 540;

    public enum IconSize {

        SIZE_16X16,
        SIZE_32X32,
        SIZE_64X64,
        SIZE_128X128,
        SIZE_SCALE
    }
    private static ImageIcon icon;

    public static Icon getIcon(String iconName, IconSize iconSize) {
        ImageIcon scaledIcon = null;
        java.net.URL url = IconFactory.class.getResource("appIcons/" + iconName + ".png");
        if (url != null) {
            icon = new ImageIcon(url);
            scaledIcon = new ImageIcon(getScaledImage(icon.getImage(), iconSize));
        }
        return (Icon) scaledIcon;
    }

    public static Icon getPreviewImage(String imageName, IconSize iconSize) {

        Image image = null;
        if (imageName.endsWith("tif") || imageName.endsWith("tiff")) {
            try {
                FileInputStream in = new FileInputStream(imageName);
                FileChannel channel = in.getChannel();
                ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
                channel.read(buffer);
                image = getImage(buffer.array());
            } catch (Exception e) {
                logger.error("Exception e="+e.getMessage());
            }
        } else {
            image = new ImageIcon(imageName).getImage();
        }
        return (Icon) new ImageIcon(getScaledImage(image, iconSize));
    }

    private static Image getImage(byte[] data) throws Exception {
        Image image = null;
        SeekableStream stream = new ByteArraySeekableStream(data);
        String[] names = ImageCodec.getDecoderNames(stream);
        ImageDecoder dec = ImageCodec.createImageDecoder(names[0], stream, null);
        RenderedImage im = dec.decodeAsRenderedImage();
        image = PlanarImage.wrapRenderedImage(im).getAsBufferedImage();
        return image;
    }

    /**
     * Resizes an image using a Graphics2D object backed by a BufferedImage.
     *
     * @param srcImg - source image to scale
     * @param w - desired width
     * @param h - desired height
     * @return - the new resized image
     * @see
     * http://java.sun.com/docs/books/tutorial/uiswing/examples/components/IconDemoProject/src/components/IconDemoApp.java
     */
    public static Image getScaledImage(Image srcImg, IconSize iconSize) {
        int h, w;
        switch (iconSize) {
            case SIZE_SCALE:
                int[] hw = ScalingImage(srcImg);
                h = hw[0];
                w = hw[1];
                break;
            case SIZE_128X128:
                h = w = 128;
                break;
            case SIZE_16X16:
                h = w = 16;
                break;
            case SIZE_64X64:
                h = w = 64;
                break;
            case SIZE_32X32:
                h = w = 32;
                break;
            default:
                h = w = 16;
        }
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();
        return resizedImg;
    }

    private static int[] ScalingImage(Image srcImg) {

        int[] hw = new int[2];

        int w = srcImg.getWidth(null);
        int h = srcImg.getHeight(null);
        int side = Math.max(w, h);
        double scale = SCALE_FACTOR / (double) side;
        hw[0] = (int) (scale * (double) h);
        hw[1] = (int) (scale * (double) w);

        return hw;
    }
}
