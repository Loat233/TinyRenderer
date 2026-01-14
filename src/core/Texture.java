package core;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

public class Texture {
    private int width;
    private int height;
    private int[][] data;

    public Texture(String filename) {
        BufferedImage image = loadTGA(filename);
        if (image == null) {
            createDefaultTexture();
            return;
        }

        width = image.getWidth();
        height = image.getHeight();
        data = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[y][x] = image.getRGB(x, y);
            }
        }

    }

    private BufferedImage loadTGA(String filename) {
        try {
            // 尝试使用TwelveMonkeys库加载TGA
            Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix("tga");
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(new FileImageInputStream(new File(filename)));
                return reader.read(0);
            }
            // 如果上面的方法失败，返回null
            return null;
        } catch (Exception e) {
            System.err.println("无法加载TGA纹理: " + filename);
            return null;
        }
    }

    private void createDefaultTexture() {
        width = height = 256;
        data = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isWhite = ((x / 32) + (y / 32)) % 2 == 0;
                data[y][x] = isWhite ? 0xFFFFFF : 0x888888;
            }
        }
    }

    public Vector getVector(double u, double v) {
        u = u - Math.floor(u);
        v = v - Math.floor(v);
        int x = (int) (u * (width - 1));
        int y = (int) (v * (height - 1));

        int color = data[y][x];
        double b = (color >> 16) & 0xFF;
        double g = (color >> 8) & 0xFF;
        double r = color & 0xFF;

        double vx = (r / 255) * 2 - 1;
        double vy = (g / 255) * 2 - 1;
        double vz = (b / 255) * 2 - 1;

        return new Vector(vx, vy, vz).normalize();
    }

    public int[] getRGB(double u, double v) {
        u = u - Math.floor(u);
        v = v - Math.floor(v);
        int x = (int) (u * (width - 1));
        int y = (int) (v * (height - 1));

        int color = data[y][x];
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return new int[]{r, g, b};
    }
}
