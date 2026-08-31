// CursorEditor.cs
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace CursorEditor
{
    class Program
    {
        static void Main(string[] args)
        {
            var opts = ParseArgs(args);
            var editor = new CursorEditor();
            if (opts.Load != null)
            {
                editor.Load(opts.Load);
            }
            else if (opts.New)
            {
                editor.Size = opts.Size;
                editor.InitPixels();
            }
            else
            {
                editor.Size = 32;
                editor.InitPixels();
            }

            if (opts.Hotspot != null)
            {
                var parts = opts.Hotspot.Split(',');
                if (parts.Length == 2)
                {
                    editor.Hotspot = (int.Parse(parts[0]), int.Parse(parts[1]));
                }
            }

            if (opts.Color != null)
            {
                var colorMap = new Dictionary<string, int>
                {
                    ["#000000"] = 1, ["#FFFFFF"] = 2, ["#FF0000"] = 3,
                    ["#00FF00"] = 4, ["#0000FF"] = 5, ["#FFFF00"] = 6,
                    ["#FF00FF"] = 7, ["#00FFFF"] = 8
                };
                editor.CurrentColor = colorMap.GetValueOrDefault(opts.Color.ToUpper(), 1);
            }

            if (opts.Pixel != null)
            {
                var parts = opts.Pixel.Split(',');
                if (parts.Length == 2)
                {
                    int x = int.Parse(parts[0]);
                    int y = int.Parse(parts[1]);
                    editor.SetPixel(x, y, editor.CurrentColor);
                }
            }

            if (opts.Fill != null)
            {
                var parts = opts.Fill.Split(',');
                if (parts.Length == 2)
                {
                    int x = int.Parse(parts[0]);
                    int y = int.Parse(parts[1]);
                    editor.Fill(x, y, editor.CurrentColor);
                }
            }

            if (opts.Save != null)
            {
                editor.Save(opts.Save);
            }

            if (opts.Export != null)
            {
                editor.ExportCur(opts.Export);
            }

            if (opts.Preview)
            {
                editor.Preview();
            }

            if (!opts.New && opts.Load == null && opts.Save == null && opts.Export == null &&
                !opts.Preview && opts.Pixel == null && opts.Fill == null)
            {
                Console.WriteLine("Используйте --help для справки.");
            }
        }

        static Options ParseArgs(string[] args)
        {
            var opts = new Options();
            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--new": opts.New = true; break;
                    case "--size": opts.Size = int.Parse(args[++i]); break;
                    case "--load": opts.Load = args[++i]; break;
                    case "--save": opts.Save = args[++i]; break;
                    case "--export": opts.Export = args[++i]; break;
                    case "--hotspot": opts.Hotspot = args[++i]; break;
                    case "--color": opts.Color = args[++i]; break;
                    case "--pixel": opts.Pixel = args[++i]; break;
                    case "--fill": opts.Fill = args[++i]; break;
                    case "--preview": opts.Preview = true; break;
                }
            }
            return opts;
        }

        class Options
        {
            public bool New { get; set; }
            public int Size { get; set; } = 32;
            public string Load { get; set; }
            public string Save { get; set; }
            public string Export { get; set; }
            public string Hotspot { get; set; }
            public string Color { get; set; }
            public string Pixel { get; set; }
            public string Fill { get; set; }
            public bool Preview { get; set; }
        }

        class EditorData
        {
            public int Size { get; set; }
            public int[][] Pixels { get; set; }
            public int[] Hotspot { get; set; }
            public int[] Palette { get; set; }
        }

        class CursorEditor
        {
            public int Size { get; set; }
            public (int X, int Y) Hotspot { get; set; }
            public int CurrentColor { get; set; } = 1;
            public int[] Palette { get; set; } = { 1, 2, 3, 4, 5, 6, 7, 8 };
            private int[][] pixels;
            private Dictionary<int, int[]> colorMap = new();

            public CursorEditor()
            {
                colorMap[0] = new int[] { 255, 255, 255, 0 };
                colorMap[1] = new int[] { 0, 0, 0, 255 };
                colorMap[2] = new int[] { 255, 255, 255, 255 };
                colorMap[3] = new int[] { 255, 0, 0, 255 };
                colorMap[4] = new int[] { 0, 255, 0, 255 };
                colorMap[5] = new int[] { 0, 0, 255, 255 };
                colorMap[6] = new int[] { 255, 255, 0, 255 };
                colorMap[7] = new int[] { 255, 0, 255, 255 };
                colorMap[8] = new int[] { 0, 255, 255, 255 };
            }

            public void InitPixels()
            {
                pixels = new int[Size][];
                for (int i = 0; i < Size; i++)
                {
                    pixels[i] = new int[Size];
                }
            }

            public void SetPixel(int x, int y, int colorIdx)
            {
                if (x >= 0 && x < Size && y >= 0 && y < Size)
                    pixels[y][x] = colorIdx;
            }

            public int GetPixel(int x, int y)
            {
                if (x >= 0 && x < Size && y >= 0 && y < Size)
                    return pixels[y][x];
                return 0;
            }

            public void Fill(int x, int y, int colorIdx)
            {
                int target = GetPixel(x, y);
                if (target == colorIdx) return;
                var stack = new Stack<(int, int)>();
                stack.Push((x, y));
                var visited = new HashSet<(int, int)>();
                while (stack.Count > 0)
                {
                    var (cx, cy) = stack.Pop();
                    if (visited.Contains((cx, cy))) continue;
                    visited.Add((cx, cy));
                    if (GetPixel(cx, cy) == target)
                    {
                        SetPixel(cx, cy, colorIdx);
                        foreach (var (dx, dy) in new[] { (1, 0), (-1, 0), (0, 1), (0, -1) })
                        {
                            int nx = cx + dx, ny = cy + dy;
                            if (nx >= 0 && nx < Size && ny >= 0 && ny < Size)
                                stack.Push((nx, ny));
                        }
                    }
                }
            }

            public Bitmap ToImage()
            {
                var img = new Bitmap(Size, Size);
                for (int y = 0; y < Size; y++)
                {
                    for (int x = 0; x < Size; x++)
                    {
                        var c = colorMap.GetValueOrDefault(pixels[y][x], new int[] { 0, 0, 0, 0 });
                        img.SetPixel(x, y, Color.FromArgb(c[3], c[0], c[1], c[2]));
                    }
                }
                return img;
            }

            public void ExportCur(string filename)
            {
                var img = ToImage();
                string pngName = filename.Replace(".cur", ".png");
                img.Save(pngName, ImageFormat.Png);
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Экспортировано в {filename} (как PNG)");
                Console.ResetColor();
            }

            public void Save(string filename)
            {
                var data = new EditorData
                {
                    Size = Size,
                    Pixels = pixels,
                    Hotspot = new[] { Hotspot.X, Hotspot.Y },
                    Palette = Palette
                };
                string json = JsonSerializer.Serialize(data, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(filename, json);
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Проект сохранён в {filename}");
                Console.ResetColor();
            }

            public void Load(string filename)
            {
                string json = File.ReadAllText(filename);
                var data = JsonSerializer.Deserialize<EditorData>(json);
                if (data != null)
                {
                    Size = data.Size;
                    pixels = data.Pixels;
                    Hotspot = (data.Hotspot[0], data.Hotspot[1]);
                    Palette = data.Palette ?? new[] { 1, 2, 3, 4, 5, 6, 7, 8 };
                }
            }

            public void Preview()
            {
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine($"Предпросмотр курсора ({Size}x{Size}), hotspot: ({Hotspot.X},{Hotspot.Y})");
                Console.ResetColor();
                string[] chars = { "·", "█", "▓", "▒", "░", " " };
                for (int y = 0; y < Size; y++)
                {
                    string line = "";
                    for (int x = 0; x < Size; x++)
                    {
                        int idx = pixels[y][x];
                        line += idx < chars.Length ? chars[idx] : " ";
                    }
                    Console.WriteLine(line);
                }
            }
        }
    }
}
