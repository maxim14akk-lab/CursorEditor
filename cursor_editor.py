
```python
#!/usr/bin/env python3
# cursor_editor.py
import argparse
import json
import sys
from PIL import Image, ImageDraw
from colorama import init, Fore, Style

init(autoreset=True)

class CursorEditor:
    def __init__(self, size=32):
        self.size = size
        self.pixels = [[0 for _ in range(size)] for _ in range(size)]
        self.hotspot = (0, 0)
        self.colors = {
            0: (255, 255, 255, 0),  # прозрачный
            1: (0, 0, 0, 255),       # чёрный
            2: (255, 255, 255, 255), # белый
            3: (255, 0, 0, 255),     # красный
            4: (0, 255, 0, 255),     # зелёный
            5: (0, 0, 255, 255),     # синий
            6: (255, 255, 0, 255),   # жёлтый
            7: (255, 0, 255, 255),   # маджента
            8: (0, 255, 255, 255),   # циан
        }
        self.current_color = 1
        self.palette = [1, 2, 3, 4, 5, 6, 7, 8]

    def set_pixel(self, x, y, color_idx):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.pixels[y][x] = color_idx

    def get_pixel(self, x, y):
        if 0 <= x < self.size and 0 <= y < self.size:
            return self.pixels[y][x]
        return 0

    def fill(self, x, y, color_idx):
        target = self.get_pixel(x, y)
        if target == color_idx:
            return
        stack = [(x, y)]
        visited = set()
        while stack:
            cx, cy = stack.pop()
            if (cx, cy) in visited:
                continue
            visited.add((cx, cy))
            if self.get_pixel(cx, cy) == target:
                self.set_pixel(cx, cy, color_idx)
                for dx, dy in [(1,0), (-1,0), (0,1), (0,-1)]:
                    nx, ny = cx+dx, cy+dy
                    if 0 <= nx < self.size and 0 <= ny < self.size:
                        stack.append((nx, ny))

    def to_image(self):
        img = Image.new('RGBA', (self.size, self.size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        for y in range(self.size):
            for x in range(self.size):
                color = self.colors.get(self.pixels[y][x], (0, 0, 0, 0))
                draw.point((x, y), fill=color)
        return img

    def export_cur(self, filename):
        img = self.to_image()
        # .cur формат — упрощённо сохраняем как PNG с переименованием
        img.save(filename.replace('.cur', '.png'))
        print(f"Экспортировано в {filename} (как PNG)")

    def to_dict(self):
        return {
            "size": self.size,
            "hotspot": self.hotspot,
            "pixels": self.pixels,
            "palette": self.palette
        }

    @classmethod
    def from_dict(cls, data):
        editor = cls(data["size"])
        editor.pixels = data["pixels"]
        editor.hotspot = tuple(data["hotspot"])
        editor.palette = data.get("palette", [1, 2, 3, 4, 5, 6, 7, 8])
        return editor

    def preview(self):
        print(Fore.CYAN + f"Предпросмотр курсора ({self.size}x{self.size}), hotspot: {self.hotspot}")
        for y in range(self.size):
            line = ""
            for x in range(self.size):
                color = self.pixels[y][x]
                if color == 0:
                    line += "·"
                elif color == 1:
                    line += "█"
                elif color == 2:
                    line += "▓"
                else:
                    line += "▒"
            print(line)

def main():
    parser = argparse.ArgumentParser(description="Редактор курсоров")
    parser.add_argument("--new", action="store_true", help="Создать новый курсор")
    parser.add_argument("--size", type=int, default=32, help="Размер курсора")
    parser.add_argument("--load", help="Загрузить проект из JSON")
    parser.add_argument("--save", help="Сохранить проект в JSON")
    parser.add_argument("--export", help="Экспортировать в файл")
    parser.add_argument("--hotspot", help="Точка активации (x,y)")
    parser.add_argument("--color", help="Цвет для рисования (HEX)")
    parser.add_argument("--pixel", action="append", help="Установить пиксель (x,y)")
    parser.add_argument("--fill", help="Залить область (x,y)")
    parser.add_argument("--preview", action="store_true", help="Показать предпросмотр")
    args = parser.parse_args()

    editor = None
    if args.load:
        with open(args.load, 'r') as f:
            data = json.load(f)
            editor = CursorEditor.from_dict(data)
    elif args.new:
        editor = CursorEditor(args.size)
    else:
        editor = CursorEditor(32)

    if args.hotspot:
        x, y = map(int, args.hotspot.split(','))
        editor.hotspot = (x, y)

    if args.color:
        # преобразуем HEX в индекс палитры (упрощённо)
        color_map = {
            '#000000': 1, '#FFFFFF': 2, '#FF0000': 3,
            '#00FF00': 4, '#0000FF': 5, '#FFFF00': 6,
            '#FF00FF': 7, '#00FFFF': 8
        }
        editor.current_color = color_map.get(args.color.upper(), 1)

    if args.pixel:
        for p in args.pixel:
            x, y = map(int, p.split(','))
            editor.set_pixel(x, y, editor.current_color)

    if args.fill:
        x, y = map(int, args.fill.split(','))
        editor.fill(x, y, editor.current_color)

    if args.save:
        with open(args.save, 'w') as f:
            json.dump(editor.to_dict(), f, indent=2)
        print(Fore.GREEN + f"Проект сохранён в {args.save}")

    if args.export:
        editor.export_cur(args.export)

    if args.preview:
        editor.preview()

    if not any([args.new, args.load, args.save, args.export, args.preview, args.pixel, args.fill]):
        parser.print_help()

if __name__ == "__main__":
    main()
