// CursorEditor.java
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CursorEditor {
    @Parameter(names = "--new")
    private boolean newFlag;
    @Parameter(names = "--size")
    private int size = 32;
    @Parameter(names = "--load")
    private String loadFile;
    @Parameter(names = "--save")
    private String saveFile;
    @Parameter(names = "--export")
    private String exportFile;
    @Parameter(names = "--hotspot")
    private String hotspot;
    @Parameter(names = "--color")
    private String color;
    @Parameter(names = "--pixel")
    private String pixel;
    @Parameter(names = "--fill")
    private String fill;
    @Parameter(names = "--preview")
    private boolean preview;

    static class EditorData {
        int size;
        int[][] pixels;
        int[] hotspot;
        int[] palette;
    }

    private int[][] pixels;
    private int currentColor = 1;
    private int[] hotspotPos = {0, 0};
    private int[] palette = {1, 2, 3, 4, 5, 6, 7, 8};
    private Map<Integer, int[]> colorMap = new HashMap<>();

    public CursorEditor() {
        colorMap.put(0, new int[]{255, 255, 255, 0});
        colorMap.put(1, new int[]{0, 0, 0, 255});
        colorMap.put(2, new int[]{255, 255, 255, 255});
        colorMap.put(3, new int[]{255, 0, 0, 255});
        colorMap.put(4, new int[]{0, 255, 0, 255});
        colorMap.put(5, new int[]{0, 0, 255, 255});
        colorMap.put(6, new int[]{255, 255, 0, 255});
        colorMap.put(7, new int[]{255, 0, 255, 255});
        colorMap.put(8, new int[]{0, 255, 255, 255});
    }

    private void initPixels() {
        pixels = new int[size][size];
        for (int i = 0; i < size; i++) {
            Arrays.fill(pixels[i], 0);
        }
    }

    private void setPixel(int x, int y, int colorIdx) {
        if (x >= 0 && x < size && y >= 0 && y < size) {
            pixels[y][x] = colorIdx;
        }
    }

    private int getPixel(int x, int y) {
        if (x >= 0 && x < size && y >= 0 && y < size) {
            return pixels[y][x];
        }
        return 0;
    }

    private void fill(int x, int y, int colorIdx) {
        int target = getPixel(x, y);
        if (target == colorIdx) return;
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{x, y});
        Set<String> visited = new HashSet<>();
        while (!stack.isEmpty()) {
            int[] pos = stack.pop();
            int cx = pos[0], cy = pos[1];
            String key = cx + "," + cy;
            if (visited.contains(key)) continue;
            visited.add(key);
            if (getPixel(cx, cy) == target) {
                setPixel(cx, cy, colorIdx);
                int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
                for (int[] d : dirs) {
                    int nx = cx + d[0], ny = cy + d[1];
                    if (nx >= 0 && nx < size && ny >= 0 && ny < size) {
                        stack.push(new int[]{nx, ny});
                    }
                }
            }
        }
    }

    private BufferedImage toImage() {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int[] c = colorMap.get(pixels[y][x]);
                if (c == null) c = new int[]{0, 0, 0, 0};
                img.setRGB(x, y, new Color(c[0], c[1], c[2], c[3]).getRGB());
            }
        }
        return img;
    }

    private void exportCur(String filename) throws IOException {
        BufferedImage img = toImage();
        String pngName = filename.replace(".cur", ".png");
        ImageIO.write(img, "png", new File(pngName));
        System.out.println("\u001B[32mЭкспортировано в " + filename + " (как PNG)\u001B[0m");
    }

    private EditorData toData() {
        EditorData data = new EditorData();
        data.size = size;
        data.pixels = pixels;
        data.hotspot = hotspotPos;
        data.palette = palette;
        return data;
    }

    private void fromData(EditorData data) {
        this.size = data.size;
        this.pixels = data.pixels;
        this.hotspotPos = data.hotspot;
        this.palette = data.palette;
    }

    private void preview() {
        System.out.println("\u001B[36mПредпросмотр курсора (" + size + "x" + size + "), hotspot: " + hotspotPos[0] + "," + hotspotPos[1] + "\u001B[0m");
        String[] chars = {"·", "█", "▓", "▒", "░", " "};
        for (int y = 0; y < size; y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < size; x++) {
                int idx = pixels[y][x];
                line.append(idx < chars.length ? chars[idx] : " ");
            }
            System.out.println(line);
        }
    }

    public void run() throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type dataType = new TypeToken<EditorData>(){}.getType();

        if (loadFile != null) {
            String json = new String(Files.readAllBytes(Paths.get(loadFile)));
            EditorData data = gson.fromJson(json, dataType);
            fromData(data);
        } else if (newFlag || size > 0) {
            initPixels();
        } else {
            size = 32;
            initPixels();
        }

        if (hotspot != null) {
            String[] parts = hotspot.split(",");
            if (parts.length == 2) {
                hotspotPos[0] = Integer.parseInt(parts[0]);
                hotspotPos[1] = Integer.parseInt(parts[1]);
            }
        }

        if (color != null) {
            Map<String, Integer> colorMap = new HashMap<>();
            colorMap.put("#000000", 1); colorMap.put("#FFFFFF", 2);
            colorMap.put("#FF0000", 3); colorMap.put("#00FF00", 4);
            colorMap.put("#0000FF", 5); colorMap.put("#FFFF00", 6);
            colorMap.put("#FF00FF", 7); colorMap.put("#00FFFF", 8);
            currentColor = colorMap.getOrDefault(color.toUpperCase(), 1);
        }

        if (pixel != null) {
            String[] parts = pixel.split(",");
            if (parts.length == 2) {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                setPixel(x, y, currentColor);
            }
        }

        if (fill != null) {
            String[] parts = fill.split(",");
            if (parts.length == 2) {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                fill(x, y, currentColor);
            }
        }

        if (saveFile != null) {
            String json = gson.toJson(toData());
            Files.write(Paths.get(saveFile), json.getBytes());
            System.out.println("\u001B[32mПроект сохранён в " + saveFile + "\u001B[0m");
        }

        if (exportFile != null) {
            exportCur(exportFile);
        }

        if (preview) {
            preview();
        }

        if (!newFlag && loadFile == null && saveFile == null && exportFile == null && !preview && pixel == null && fill == null) {
            System.out.println("Используйте --help для справки.");
        }
    }

    public static void main(String[] args) throws Exception {
        CursorEditor editor = new CursorEditor();
        JCommander.newBuilder().addObject(editor).build().parse(args);
        editor.run();
    }
}
