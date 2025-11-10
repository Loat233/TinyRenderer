package core;

import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Model {
    private List<Vec3> verts;        // 顶点数组
    private List<Integer> facet_vrt; // 每个三角形在顶点数组中的索引

    public Model(String filename) {
        this.verts = new ArrayList<>();
        this.facet_vrt = new ArrayList<>();
        loadModel(filename);
    }

    /**
     * 加载OBJ格式的3D模型文件
     * 顶点格式: v x y z
     * 面格式: f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3
     */
    private void loadModel(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("v ")) {
                    // 处理顶点行
                    processVertexLine(line);
                } else if (line.startsWith("f ")) {
                    // 处理面行
                    processFaceLine(line);
                }
                // 忽略其他行（vn, vt, 注释等）
            }
        } catch (IOException e) {
            System.err.println("无法读取模型文件: " + filename);
        }
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
            } catch (NumberFormatException e) {
                System.err.println("顶点数据格式错误: " + line);
            }
        }
    }

    /**
     * 处理面行: f 1193/1240/1193 1180/1227/1180 1179/1226/1179
     * 只提取每个顶点描述中的第一个数字（顶点索引）
     */
    private void processFaceLine(String line) {
        String[] parts = line.split("\\s+");

        // 面定义至少需要4部分: "f" + 3个顶点
        if (parts.length < 4) {
            System.err.println("面数据不完整: " + line);
            return;
        }

        // 存储当前面的顶点索引
        List<Integer> faceVertexIndices = new ArrayList<>();

        // 从第二个部分开始（索引1），跳过"f"
        for (int i = 1; i < parts.length; i++) {
            String vertexDescription = parts[i];

            // 分割顶点描述，获取第一个数字（顶点索引）
            String[] vertexComponents = vertexDescription.split("/");
            if (vertexComponents.length == 0) {
                System.err.println("顶点描述格式错误: " + vertexDescription);
                continue;
            }

            try {
                // 提取顶点索引并转换为0-based
                int vertexIndex = Integer.parseInt(vertexComponents[0]) - 1;

                // 检查索引是否有效
                if (vertexIndex < 0 || vertexIndex >= verts.size()) {
                    System.err.println("顶点索引越界: " + (vertexIndex + 1) + " (最大: " + verts.size() + ")");
                    continue;
                }

                faceVertexIndices.add(vertexIndex);
            } catch (NumberFormatException e) {
                System.err.println("顶点索引格式错误: " + vertexComponents[0]);
            }
        }

        // 将面分解为三角形（三角化）
        triangulateFace(faceVertexIndices);
    }

    /**
     * 将多边形面分解为三角形
     * 对于三角形面，直接添加
     * 对于多边形面，使用扇形三角化
     */
    private void triangulateFace(List<Integer> vertexIndices) {
        int vertexCount = vertexIndices.size();

        if (vertexCount < 3) {
            System.err.println("面的顶点数不足3个: " + vertexCount);
            return;
        }

        // 如果是三角形，直接添加
        if (vertexCount == 3) {
            facet_vrt.add(vertexIndices.get(0));
            facet_vrt.add(vertexIndices.get(1));
            facet_vrt.add(vertexIndices.get(2));
        }
        // 如果是四边形，分解为2个三角形
        else if (vertexCount == 4) {
            // 第一个三角形: 顶点0,1,2
            facet_vrt.add(vertexIndices.get(0));
            facet_vrt.add(vertexIndices.get(1));
            facet_vrt.add(vertexIndices.get(2));

            // 第二个三角形: 顶点0,2,3
            facet_vrt.add(vertexIndices.get(0));
            facet_vrt.add(vertexIndices.get(2));
            facet_vrt.add(vertexIndices.get(3));
        }
        // 对于更多顶点的多边形，使用扇形三角化
        else {
            for (int i = 1; i < vertexCount - 1; i++) {
                facet_vrt.add(vertexIndices.getFirst());        // 中心顶点
                facet_vrt.add(vertexIndices.get(i));       // 当前顶点
                facet_vrt.add(vertexIndices.get(i + 1));   // 下一个顶点
            }
        }
    }

    // 其他方法保持不变
    public int nverts() {
        return verts.size();
    }

    public int nfaces() {
        return facet_vrt.size() / 3;
    }

    public Vec3 vert(int i) {
        if (i < 0 || i >= verts.size()) {
            throw new IndexOutOfBoundsException("顶点索引越界: " + i);
        }
        return verts.get(i);
    }

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

    /**
     * 打印模型信息（用于调试）
     */
    public void printModelInfo() {
        System.out.println("模型信息:");
        System.out.println("顶点数量: " + nverts());
        System.out.println("三角形面片数量: " + nfaces());

        // 打印前几个顶点作为示例
        System.out.println("前5个顶点:");
        for (int i = 0; i < Math.min(5, verts.size()); i++) {
            System.out.println("v" + (i + 1) + ": " + verts.get(i));
        }

        // 打印前几个面作为示例
        System.out.println("前3个面:");
        for (int i = 0; i < Math.min(3, nfaces()); i++) {
            System.out.print("f" + (i + 1) + ": ");
            for (int j = 0; j < 3; j++) {
                int vertexIndex = facet_vrt.get(i * 3 + j) + 1; // 转换回1-based显示
                System.out.print(vertexIndex + " ");
            }
            System.out.println();
        }
    }
}
