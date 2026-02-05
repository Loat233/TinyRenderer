package core;

import static core.openGL.*;

public class IShader {
    double[][] normMatrix;
    Texture[] textures;                     //  纹理顺序:norm, diffuse, spec, glow

    double[] tangent = new double[3];       //  切空间的T向量
    double[] bitangent = new double[3];     //  切空间的B向量
    double[] tangent_norm = new double[3];  //  切空间: 点p的法向量

    public IShader(double[][] a, double[][] b, double[][] c, Texture[] textures, double[][] normMatrix) {
        this.normMatrix = normMatrix;
        this.textures = textures;
        init_TB(a, b, c);
    }

    public void init_TB(double[][] a, double[][] b, double[][] c) {
        //  计算切空间TBN的T, B向量
        //  计算eye空间边向量: e0, e1
        //  eye空间: e0 = b[0] - a[0]
        double e0_x = b[0][0] - a[0][0];
        double e0_y = b[0][1] - a[0][1];
        double e0_z = b[0][2] - a[0][2];
        //  eye空间: e1 = c[0] - a[0]
        double e1_x = c[0][0] - a[0][0];
        double e1_y = c[0][1] - a[0][1];
        double e1_z = c[0][2] - a[0][2];
        //  计算uv空间边向量: r0, r1
        //  uv: r0 = b[1] - a[1]
        double r0_x = b[1][0] - a[1][0];
        double r0_y = b[1][1] - a[1][1];
        //  uv: r1 = c[1] - a[1]
        double r1_x = c[1][0] - a[1][0];
        double r1_y = c[1][1] - a[1][1];

        /*  矩阵U = [r0_x, r1_x], E = [e0_x, e1_x], inv_U = [e0_x, e1_x]
                   [r0_y, r1_y]      [e0_y, e1_y]          [e0_y, e1_y]
                                     [e0_z, e1_z]
         */
        //  E * inv_U = [T B]
        double inv_detU = 1 / (r0_x * r1_y - r1_x * r0_y);
        //  T = (e0 * r1_y - e1 * r0_y) * invDetU
        this.tangent[0] = (e0_x * r1_y - e1_x * r0_y) * inv_detU;
        this.tangent[1] = (e0_y * r1_y - e1_y * r0_y) * inv_detU;
        this.tangent[2] = (e0_z * r1_y - e1_z * r0_y) * inv_detU;
        normalize(this.tangent);
        //  B = (e1 * r0_x - e0 * r1_x) * invDet
        this.bitangent[0] = (e1_x * r0_x - e0_x * r1_x) * inv_detU;
        this.bitangent[1] = (e1_y * r0_x - e0_y * r1_x) * inv_detU;
        this.bitangent[2] = (e1_z * r0_x - e0_z * r1_x) * inv_detU;
        normalize(this.bitangent);
    }

    // norm: 像素点的插值法线; tex: 像素点的插值纹理坐标
    // 向量: light, norm; 二维坐标: tex
    //  三角形顶点: double[][]{eye顶点, tex顶点, eye空间法向量}
    public int fragment(double lightIntensity, double[] light, double[] norm, double[] tex) {
        // 获取漫反射diffuse纹理
        int diff_color = textures[1].getRGB(tex[0], 1 - tex[1]);
        int spec_color = textures[2].getRGB(tex[0], 1 - tex[1]);
        int glow_color = textures[3].getRGB(tex[0], 1 - tex[1]);

        int R,G,B;
        if (lightIntensity == 0) {
            double glow_r = ((glow_color >> 16) & 0xFF) * 0.6;
            double glow_g = ((glow_color >> 8) & 0xFF) * 0.6;
            double glow_b = (glow_color & 0xFF) * 0.6;

            R = (int) Math.floor(Math.min(255.0, glow_r));
            G = (int) Math.floor(Math.min(255.0, glow_g));
            B = (int) Math.floor(Math.min(255.0, glow_b));
            return (R << 16) | (G << 8) | B;
        }
        // 计算eye空间下像素点法线
        // 直接使用插值法线计算,则直接使用norm
        // 若使用global space normal mapping:
        //  get_glob_norm(tex, norm);        // 会传递修改norm,但无影响
        // 若使用tangent space normal mapping:
        get_tangent_norm(norm, tex, norm);

        // 计算光线的衰减值(attenuation)
        double kc = 1.0;
        double kl = 0.009;
        double kq = 0.032;

        double light_dist = sqrt(light);
        double attenuation = 1.0 / (kc + kl * light_dist + kq * light_dist * light_dist) ;
        normalize(light);   //  后续light都为归一向量
        double factor = dot(norm, light);

        // 漫反射强度 = (直射光 * 阴影系数 * 衰减) + 环境光
        double direct_diffuse = Math.max(0.0, factor) * lightIntensity * attenuation;
        double diff_light = direct_diffuse + 0.9;

        // 高光强度 = (高光计算 * 阴影系数 * 衰减)
        scale(norm, factor * 2);        //  会传递修改norm,但无影响
        double[] r = new double[3];
        minus(norm, light, r);   //  赋值r: 反射向量r
        normalize(r);   //  赋值r: 归一化向量r
        double spec_light = Math.pow(Math.max(0.0, r[2]), 12.0) * attenuation * lightIntensity;

        double base_r = ((diff_color >> 16) & 0xFF) * diff_light;
        double specular_r = ((spec_color >> 16) & 0xFF) * spec_light;
        double glow_r = ((glow_color >> 16) & 0xFF) * 0.6;

        double base_g = ((diff_color >> 8) & 0xFF) * diff_light;
        double specular_g =((spec_color >> 8) & 0xFF) * spec_light;
        double glow_g = ((glow_color >> 8) & 0xFF) * 0.6;

        double base_b = (diff_color & 0xFF) * diff_light;
        double specular_b = (spec_color & 0xFF) * spec_light;
        double glow_b = (glow_color & 0xFF) * 0.6;

        R = (int) Math.floor(Math.min(255.0, base_r + 0.8 * specular_r + glow_r));
        G = (int) Math.floor(Math.min(255.0, base_g + 0.8 * specular_g + glow_g));
        B = (int) Math.floor(Math.min(255.0, base_b + 0.8 * specular_b + glow_b));
        // ARGB
        return (R << 16) | (G << 8) | B;
    }

    private void get_glob_norm(double[] tex, double[] dest) {
        textures[0].getVector(tex[0], 1 - tex[1], dest);    //  赋值dest: dest = 全局空间法向量(已归一化)
        Matrix.vec_product(normMatrix, dest, dest);
        normalize(dest);
    }

    //  计算eye空间下像素点法线
    //  向量: norm; 二维坐标: tex
    //  三角形顶点: double[][]{eye顶点, tex顶点, eye空间法向量}
    private void get_tangent_norm(double[] norm, double[] tex, double[] dest) {
        textures[0].getVector(tex[0], 1 - tex[1], tangent_norm);    //  赋值tangent_norm: tangent_norm = 切空间法向量(已归一化)
        for (int i = 0; i < 3; i++) {
            dest[i] = tangent[i] * tangent_norm[0] + bitangent[i] * tangent_norm[1] + norm[i] * tangent_norm[2];
        }
        normalize(dest);
    }
}
