package core;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public class MainThread extends JFrame{

    //  屏幕的分辨率
    public static int Width = 800;
    public static int Height = 800;

    public static JPanel panel;     //  用JPanel作为画板
    public static int[] screen;     //  使用一个int数组存处屏幕上像素的数值
    public static BufferedImage screenBuffer;   //  屏幕图像缓冲区。它提供了在内存中操作屏幕中图像的方法
    public static int frameIndex;   //  记载目前已渲染的帧数
    public static int frameInterval = 33;   //  希望达到的每频之间的间隔时间 (毫秒)
    public static int sleepTime;    //  cpu睡眠时间
    //  刷新率，及计算刷新率所用到一些辅助参数
    public static int framePerSecond;
    public static long lastDraw;
    public static double lastTime;

    //  程序入口
    public static void main(String[] args){
        new MainThread();
    }

    public MainThread(){
        setTitle("TinyRenderer");
        panel = (JPanel) this.getContentPane();
        panel.setPreferredSize(new Dimension(Width, Height));
        panel.setMinimumSize(new Dimension(Width, Height));
        panel.setLayout(null);

        setResizable(false);
        pack();
        setVisible(true);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //  用TYPE_INT_RGB来创建BufferedImage，然后把屏幕的像素数组指向BufferedImage中的DataBuffer。
        //  这样通过改变屏幕的像素数组(screen[])中的数据就可以在屏幕中渲染出图像
        screenBuffer =  new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
        DataBuffer dest = screenBuffer.getRaster().getDataBuffer();
        screen = ((DataBufferInt)dest).getData();

        //  加载渲染器
        openGL renderer = new openGL(0, 0, Width, Height);
        /*
        renderer.setUpsideDown(); //    设置画面颠倒
         */
        Model diablo = new Model("diablo3_pose");
        Model floor = new Model("floor");

        //  程序主循环
        int degree = 0;
        while(true) {
            renderer.init_light(new Vector(10, 0, 10));

            render(renderer, diablo, degree);
            degree = (degree + 1) % 360;
            frameIndex++;

            //  计算当前的刷新率，并尽量让刷新率保持恒定。
            if(frameIndex % 30 == 0){
                double thisTime = System.currentTimeMillis();
                framePerSecond = (int) (1000 / ((thisTime - lastTime) / 30));
                lastTime = thisTime;
            }
            sleepTime = 0;
            while(System.currentTimeMillis() - lastDraw < frameInterval){
                try {
                    Thread.sleep(1);
                    sleepTime++;
                }
                catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            lastDraw = System.currentTimeMillis();

            //  显示当前刷新率
            Graphics2D g2 = (Graphics2D) screenBuffer.getGraphics();
            g2.setColor(Color.WHITE);
            g2.drawString("FPS: " + framePerSecond + "   "  +  "Thread Sleep: " + sleepTime +  "ms    ", 5, 15);

            //  把图像发画到显存
            panel.getGraphics().drawImage(screenBuffer, 0, 0, this);
        }
    }

    public void render(openGL renderer, Model model, int degree) {
        // 每帧开始时清空屏幕缓冲区
        Arrays.fill(screen, 0xFF000000);

        double n = degree * Math.PI / 180.0;
        /*
        double eye_x = 5 * Math.cos(n);
        double eye_z = 5 * Math.sin(n);
         */

        Vec3 eye = new Vec3(5, 0, 5, 1);
        Vec3 center = new Vec3(0, 0, 0, 1);
        Vector up = new Vector(0, 1, 0);

        renderer.camera(eye, center, up, 10);
        renderer.model_direct(n);
        //  加载渲染模型后的画面
        renderer.render_model(model, screen);
    }
}