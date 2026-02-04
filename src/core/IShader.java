package core;

import static core.openGL.*;

public class IShader {
    double[][] normMatrix;
    Texture[] textures; // 纹理顺序:norm, diffuse, spec, glow

    public IShader(Texture[] textures, double[][] normMatrix) {
        this.normMatrix = normMatrix;
        this.textures = textures;
    }

    // norm: 像素点的插值法线; tex: 像素点的纹理坐标
    // 向量: light, norm; 二维坐标: tex
    //  三角形顶点: double[][]{eye顶点, tex顶点, eye空间法向量}
    public int fragment(double[][] a, double[][] b, double[][] c, double lightIntensity, double[] light, double[] norm, double[] tex) {
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
        //  norm = get_gb_norm(tex);        // 会传递修改norm,但无影响
        // 若使用tangent space normal mapping:
        norm = tangent_space_norm(a, b, c, norm, tex);

        // 计算光线的衰减值(attenuation)
        double kc = 1.0;
        double kl = 0.09;
        double kq = 0.032;

        double lg_dist = sqrt(light);
        double attenuation = 1 / (kc + kl * lg_dist + kq * lg_dist * lg_dist);
        double factor = dot(norm, light);

        // 漫反射强度 = (直射光 * 阴影系数 * 衰减) + 环境光
        double direct_diffuse = Math.max(0.0, factor) * lightIntensity * attenuation;
        double diff_light = direct_diffuse + 0.2;

        // 高光强度 = (高光计算 * 阴影系数 * 衰减)
        scale(norm, factor * 2);        // 会传递修改norm,但无影响
        double[] r = minus(norm, light);   // 计算反射向量r
        normalize(r);
        double spec_light = Math.pow(Math.max(0.0, r[2]), 10.0) * attenuation * lightIntensity;

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

    private double[] get_gb_norm(double[] tex) {
        //  不会传递修改
        double[] v = textures[0].getVector(tex[0], 1 - tex[1]);
        v = Matrix.vec_product(normMatrix, v);
        normalize(v);
        return v;
    }

    // 计算eye空间下像素点法线
    //  向量: norm; 二维坐标: tex
    //  三角形顶点: double[][]{eye顶点, tex顶点, eye空间法向量}
    private double[] tangent_space_norm(double[][] a, double[][] b, double[][] c,  double[] norm, double[] tex) {
        double[] e0 = minus(b[0], a[0]);
        double[] e1 = minus(c[0], a[0]);

        double[] r0 = minus(b[1], a[1]);
        double[] r1 = minus(c[1], a[1]);

        double[][] E = new double[][] {
                {e0[0], e1[0]},
                {e0[1], e1[1]},
                {e0[2], e1[2]}
        };
        double[][] U = new double[][] {
                {r0[0], r1[0]},
                {r0[1], r1[1]}
        };
        double[][] inv_U = Matrix.inverse(U);
        double[][] T = Matrix.product(E, inv_U);

        // t和b需要单位化
        double[] t0 = new double[]{T[0][0], T[1][0], T[2][0]};
        normalize(t0);
        double[] b0 = new double[]{T[0][1], T[1][1], T[2][1]};
        normalize(b0);

        double[][] D = new double[][] {
                {t0[0], b0[0], norm[0]},
                {t0[1], b0[1], norm[1]},
                {t0[2], b0[2], norm[2]}
        };
        double[] uv_norm = textures[0].getVector(tex[0], 1 - tex[1]);
        uv_norm = Matrix.vec_product(D, uv_norm);
        normalize(uv_norm);
        return uv_norm;
    }
}
