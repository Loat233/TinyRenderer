package core;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Model {
    private final String path;

    private final List<Vec3> verts;        // 顶点数组
    private final List<Vec3> norms;        // 法向量数组
    private final List<Vec2> texCoords;    // 纹理坐标数组

    private final List<Integer> facet_vrt; // 每个三角形在顶点数组中的索引
    private final List<Integer> facet_nrm; // 每个三角形在法向量数组中的索引
    private final List<Integer> facet_tex; // 每个三角形在纹理坐标数组中的索引

    private final Texture[] textures;

    public Model(String filename) {
        this.path = "src/obj/" + filename + "/";

        this.verts = new ArrayList<>();
        this.norms = new ArrayList<>();
        this.texCoords = new ArrayList<>();

        this.facet_vrt = new ArrayList<>();
        this.facet_nrm = new ArrayList<>();
        this.facet_tex = new ArrayList<>();

        this.textures = new Texture[4];
        loadModel(filename);
        loadTexture(filename);
    }

    private void loadModel(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader( path + filename + ".obj"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("v ")) {
                    processVertexLine(line);    // 处理顶点行
                }
                else if (line.startsWith("vt ")) {
                    processTextureCoordLine(line);    // 处理纹理坐标行
                }
                else if (line.startsWith("vn ")) {
                    processNormalLine(line);    // 处理法向量行
                }
                else if (line.startsWith("f ")) {
                    processFaceLine(line);      // 处理面行
                }
            }
        }
        catch (IOException e) {
            System.err.println("无法读取模型文件: " + filename);
        }
    }

    private void loadTexture(String filename) {
        //  tangent space normal mapping
        this.textures[0] = new Texture(path + filename + "_nm.tga");
        //  diffuse texture
        this.textures[1]  = new Texture(path + filename + "_diffuse.tga");
        //  specular texture
        this.textures[2] = new Texture(path + filename + "_spec.tga");
        //  glow texture
        this.textures[3] = new Texture(path + filename + "_glow.tga");
    }

    /**
     * 处理顶点行: v x y z
     */
    private void processVertexLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 4) {
            try {
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                verts.add(new Vec3(x, y, z));
            }
            catch (NumberFormatException e) {
                System.err.println("顶点数据格式错误: " + line);
            }
        }
    }

    /**
     * 处理纹理坐标行: vt u v [w]（新增）
     * OBJ格式支持2D纹理坐标(u,v)或3D纹理坐标(u,v,w)
     * 这里我们只读取u,v，忽略可选的w分量
     */
    private void processTextureCoordLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 3) {
            try {
                double u = Double.parseDouble(parts[1]);
                double v = Double.parseDouble(parts[2]);
                // 如果存在w分量，忽略它
                texCoords.add(new Vec2(u, v));
            }
            catch (NumberFormatException e) {
                System.err.println("纹理坐标数据格式错误: " + line);
            }
        }
        else {
            System.err.println("纹理坐标数据不完整: " + line);
        }
    }

    /**
     * 处理法向量行: vn x y z
     */
    private void processNormalLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 4) {
            try {
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                norms.add(new Vec3(x, y, z));
            }
            catch (NumberFormatException e) {
                System.err.println("法向量数据格式错误: " + line);
            }
        }
    }

    private void processFaceLine(String line) {
        String[] parts = line.split("\\s+");

        // 面定义至少需要4部分: "f" + 3个顶点
        if (parts.length < 4) {
            System.err.println("面数据不完整: " + line);
            return;
        }

        // 存储当前面的顶点索引、纹理坐标索引和法向量索引
        List<Integer> faceVertexIndices = new ArrayList<>();
        List<Integer> faceTextureIndices = new ArrayList<>();
        List<Integer> faceNormalIndices = new ArrayList<>();

        // 从第二个部分开始（索引1），跳过"f"
        for (int i = 1; i < parts.length; i++) {
            String vertexDescription = parts[i];

            // 检查顶点描述格式
            if (vertexDescription.contains("/")) {
                // 包含斜杠，需要解析各个组件
                String[] vertexComponents = vertexDescription.split("/");

                int vertexIndex = -1;
                int textureIndex = -1;
                int normalIndex = -1;

                try {
                    // 第一个组件总是顶点索引
                    if (vertexComponents.length > 0 && !vertexComponents[0].isEmpty()) {
                        vertexIndex = Integer.parseInt(vertexComponents[0]) - 1;
                    }

                    // 第二个组件是纹理坐标索引（可能为空）
                    if (vertexComponents.length > 1 && !vertexComponents[1].isEmpty()) {
                        textureIndex = Integer.parseInt(vertexComponents[1]) - 1;
                    }

                    // 第三个组件是法向量索引（可能为空）
                    if (vertexComponents.length > 2 && !vertexComponents[2].isEmpty()) {
                        normalIndex = Integer.parseInt(vertexComponents[2]) - 1;
                    }

                }
                catch (NumberFormatException e) {
                    System.err.println("顶点描述格式错误: " + vertexDescription);
                    continue;
                }

                // 验证顶点索引
                if (vertexIndex >= 0) {
                    if (vertexIndex >= verts.size()) {
                        System.err.println("顶点索引越界: " + (vertexIndex + 1) + " (最大: " + verts.size() + ")");
                        continue;
                    }
                    faceVertexIndices.add(vertexIndex);
                }
                else {
                    System.err.println("顶点索引缺失: " + vertexDescription);
                    continue;
                }

                // 验证纹理坐标索引（如果存在）
                if (textureIndex >= 0) {
                    if (textureIndex >= texCoords.size()) {
                        System.err.println("纹理坐标索引越界: " + (textureIndex + 1) + " (最大: " + texCoords.size() + ")");
                        // 不中断处理，继续使用-1表示缺失
                        faceTextureIndices.add(-1);
                    }
                    else {
                        faceTextureIndices.add(textureIndex);
                    }
                }
                else {
                    faceTextureIndices.add(-1); // 使用-1表示纹理坐标缺失
                }

                // 验证法向量索引（如果存在）
                if (normalIndex >= 0) {
                    if (normalIndex >= norms.size()) {
                        System.err.println("法向量索引越界: " + (normalIndex + 1) + " (最大: " + norms.size() + ")");
                        // 不中断处理，继续使用-1表示缺失
                        faceNormalIndices.add(-1);
                    }
                    else {
                        faceNormalIndices.add(normalIndex);
                    }
                }
                else {
                    faceNormalIndices.add(-1); // 使用-1表示法向量缺失
                }

            }
            else {
                // 简单格式：只有顶点索引
                try {
                    int vertexIndex = Integer.parseInt(vertexDescription) - 1;

                    if (vertexIndex < 0 || vertexIndex >= verts.size()) {
                        System.err.println("顶点索引越界: " + (vertexIndex + 1) + " (最大: " + verts.size() + ")");
                        continue;
                    }

                    faceVertexIndices.add(vertexIndex);
                    faceTextureIndices.add(-1); // 纹理坐标缺失
                    faceNormalIndices.add(-1); // 法向量缺失

                }
                catch (NumberFormatException e) {
                    System.err.println("顶点索引格式错误: " + vertexDescription);
                }
            }
        }

        // 将面分解为三角形（三角化）
        triangulateFace(faceVertexIndices, faceTextureIndices, faceNormalIndices);
    }

    /**
     * 将多边形面分解为三角形
     * 现在同时处理顶点索引、纹理坐标索引和法向量索引
     */
    private void triangulateFace(List<Integer> vertexIndices, List<Integer> textureIndices, List<Integer> normalIndices) {
        int vertexCount = vertexIndices.size();

        if (vertexCount < 3) {
            System.err.println("面的顶点数不足3个: " + vertexCount);
            return;
        }

        // 确保所有索引列表长度一致
        if (vertexCount != textureIndices.size() || vertexCount != normalIndices.size()) {
            System.err.println("顶点、纹理坐标和法向量索引数量不一致");
            return;
        }

        // 如果是三角形，直接添加
        if (vertexCount == 3) {
            for (int i = 0; i < 3; i++) {
                facet_vrt.add(vertexIndices.get(i));
                facet_tex.add(textureIndices.get(i));
                facet_nrm.add(normalIndices.get(i));
            }
        }
        // 如果是四边形，分解为2个三角形
        else if (vertexCount == 4) {
            // 第一个三角形: 顶点0,1,2
            facet_vrt.add(vertexIndices.get(0));
            facet_vrt.add(vertexIndices.get(1));
            facet_vrt.add(vertexIndices.get(2));
            facet_tex.add(textureIndices.get(0));
            facet_tex.add(textureIndices.get(1));
            facet_tex.add(textureIndices.get(2));
            facet_nrm.add(normalIndices.get(0));
            facet_nrm.add(normalIndices.get(1));
            facet_nrm.add(normalIndices.get(2));

            // 第二个三角形: 顶点0,2,3
            facet_vrt.add(vertexIndices.get(0));
            facet_vrt.add(vertexIndices.get(2));
            facet_vrt.add(vertexIndices.get(3));
            facet_tex.add(textureIndices.get(0));
            facet_tex.add(textureIndices.get(2));
            facet_tex.add(textureIndices.get(3));
            facet_nrm.add(normalIndices.get(0));
            facet_nrm.add(normalIndices.get(2));
            facet_nrm.add(normalIndices.get(3));
        }
        // 对于更多顶点的多边形，使用扇形三角化
        else {
            for (int i = 1; i < vertexCount - 1; i++) {
                facet_vrt.add(vertexIndices.get(0));        // 中心顶点
                facet_vrt.add(vertexIndices.get(i));       // 当前顶点
                facet_vrt.add(vertexIndices.get(i + 1));   // 下一个顶点

                facet_tex.add(textureIndices.get(0));      // 中心纹理坐标
                facet_tex.add(textureIndices.get(i));      // 当前纹理坐标
                facet_tex.add(textureIndices.get(i + 1));  // 下个纹理坐标

                facet_nrm.add(normalIndices.get(0));        // 中心法向量
                facet_nrm.add(normalIndices.get(i));       // 当前法向量
                facet_nrm.add(normalIndices.get(i + 1));   // 下个法向量
            }
        }
    }

    //  获取纹理texture[4]
    public Texture[] textures() {
        return textures;
    }

    // 面相关方法
    public int nfaces() {
        return facet_vrt.size() / 3;
    }


    // 获取顶点数量
    public int nverts() {
        return verts.size();
    }

    // 获取索引为i的顶点
    public Vec3 vert(int i) {
        if (i < 0 || i >= verts.size()) {
            throw new IndexOutOfBoundsException("顶点索引越界: " + i);
        }
        return verts.get(i);
    }

    //获取索引为iface的面内的顶点
    public Vec3 vert(int iface, int nthvert) {
        if (iface < 0 || iface >= nfaces()) {
            throw new IndexOutOfBoundsException("面索引越界: " + iface);
        }
        if (nthvert < 0 || nthvert >= 3) {
            throw new IndexOutOfBoundsException("顶点在面中的位置越界: " + nthvert);
        }

        int vertexIndex = facet_vrt.get(iface * 3 + nthvert);
        return verts.get(vertexIndex);
    }

    // 获取纹理顶点数量
    public int ntexcoords() {
        return texCoords.size();
    }

    // 获取索引为i的纹理顶点
    public Vec2 texcoord(int i) {
        if (i < 0 || i >= texCoords.size()) {
            throw new IndexOutOfBoundsException("纹理坐标索引越界: " + i);
        }
        return texCoords.get(i);
    }

    // 获得索引为iface的面内的纹理顶点
    public Vec2 texcoord(int iface, int nthvert) {
        if (iface < 0 || iface >= nfaces()) {
            throw new IndexOutOfBoundsException("面索引越界: " + iface);
        }
        if (nthvert < 0 || nthvert >= 3) {
            throw new IndexOutOfBoundsException("纹理坐标在面中的位置越界: " + nthvert);
        }

        int texIndex = facet_tex.get(iface * 3 + nthvert);
        if (texIndex == -1) {
            return null; // 返回null表示该顶点没有纹理坐标
        }
        return texCoords.get(texIndex);
    }

    // 获取法向量数量
    public int nnorms() {
        return norms.size();
    }

    // 获取索引为i的法向量
    public Vec3 norm(int i) {
        if (i < 0 || i >= norms.size()) {
            throw new IndexOutOfBoundsException("法向量索引越界: " + i);
        }
        return norms.get(i);
    }

    // 获取索引为iface的面内的法向量
    public Vec3 norm(int iface, int nthvert) {
        if (iface < 0 || iface >= nfaces()) {
            throw new IndexOutOfBoundsException("面索引越界: " + iface);
        }
        if (nthvert < 0 || nthvert >= 3) {
            throw new IndexOutOfBoundsException("法向量在面中的位置越界: " + nthvert);
        }

        int normalIndex = facet_nrm.get(iface * 3 + nthvert);
        if (normalIndex == -1) {
            return null; // 返回null表示该顶点没有法向量
        }
        return norms.get(normalIndex);
    }





    /**
     * 打印模型信息（用于调试）
     */
    public void printModelInfo() {
        System.out.println("模型信息:");
        System.out.println("顶点数量: " + nverts());
        System.out.println("纹理坐标数量: " + ntexcoords()); // 新增
        System.out.println("法向量数量: " + nnorms());
        System.out.println("三角形面片数量: " + nfaces());

        // 打印前几个顶点作为示例
        System.out.println("前5个顶点:");
        for (int i = 0; i < Math.min(5, verts.size()); i++) {
            System.out.println("v" + (i + 1) + ": " + verts.get(i));
        }

        // 打印前几个纹理坐标作为示例（新增）
        System.out.println("前5个纹理坐标:");
        for (int i = 0; i < Math.min(5, texCoords.size()); i++) {
            System.out.println("vt" + (i + 1) + ": " + texCoords.get(i));
        }

        // 打印前几个法向量作为示例
        System.out.println("前5个法向量:");
        for (int i = 0; i < Math.min(5, norms.size()); i++) {
            System.out.println("vn" + (i + 1) + ": " + norms.get(i));
        }

        // 打印前几个面作为示例
        System.out.println("前3个面:");
        for (int i = 0; i < Math.min(3, nfaces()); i++) {
            System.out.print("f" + (i + 1) + ": ");
            for (int j = 0; j < 3; j++) {
                int vertexIndex = facet_vrt.get(i * 3 + j) + 1; // 转换回1-based显示
                int texIndex = facet_tex.get(i * 3 + j);
                int normalIndex = facet_nrm.get(i * 3 + j);

                System.out.print(vertexIndex);
                if (texIndex != -1) {
                    System.out.print("/" + (texIndex + 1));
                } else {
                    System.out.print("/");
                }
                if (normalIndex != -1) {
                    System.out.print("/" + (normalIndex + 1));
                } else {
                    System.out.print("/");
                }
                System.out.print(" ");
            }
            System.out.println();
        }
    }
}