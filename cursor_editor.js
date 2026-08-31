#!/usr/bin/env node
// cursor_editor.js
const { program } = require('commander');
const fs = require('fs');
const chalk = require('chalk');

class CursorEditor {
    constructor(size = 32) {
        this.size = size;
        this.pixels = Array.from({ length: size }, () => Array(size).fill(0));
        this.hotspot = [0, 0];
        this.colors = {
            0: [255, 255, 255, 0],
            1: [0, 0, 0, 255],
            2: [255, 255, 255, 255],
            3: [255, 0, 0, 255],
            4: [0, 255, 0, 255],
            5: [0, 0, 255, 255],
            6: [255, 255, 0, 255],
            7: [255, 0, 255, 255],
            8: [0, 255, 255, 255]
        };
        this.currentColor = 1;
        this.palette = [1, 2, 3, 4, 5, 6, 7, 8];
    }

    setPixel(x, y, colorIdx) {
        if (x >= 0 && x < this.size && y >= 0 && y < this.size) {
            this.pixels[y][x] = colorIdx;
        }
    }

    getPixel(x, y) {
        if (x >= 0 && x < this.size && y >= 0 && y < this.size) {
            return this.pixels[y][x];
        }
        return 0;
    }

    fill(x, y, colorIdx) {
        const target = this.getPixel(x, y);
        if (target === colorIdx) return;
        const stack = [[x, y]];
        const visited = new Set();
        while (stack.length) {
            const [cx, cy] = stack.pop();
            const key = `${cx},${cy}`;
            if (visited.has(key)) continue;
            visited.add(key);
            if (this.getPixel(cx, cy) === target) {
                this.setPixel(cx, cy, colorIdx);
                for (const [dx, dy] of [[1,0],[-1,0],[0,1],[0,-1]]) {
                    const nx = cx + dx, ny = cy + dy;
                    if (nx >= 0 && nx < this.size && ny >= 0 && ny < this.size) {
                        stack.push([nx, ny]);
                    }
                }
            }
        }
    }

    toImage() {
        // Упрощённо: создаём PNG в буфере
        // Для реальной работы используем sharp или jimp
        const { createCanvas } = require('canvas');
        const canvas = createCanvas(this.size, this.size);
        const ctx = canvas.getContext('2d');
        const imageData = ctx.createImageData(this.size, this.size);
        for (let y = 0; y < this.size; y++) {
            for (let x = 0; x < this.size; x++) {
                const color = this.colors[this.pixels[y][x]] || [0,0,0,0];
                const idx = (y * this.size + x) * 4;
                imageData.data[idx] = color[0];
                imageData.data[idx+1] = color[1];
                imageData.data[idx+2] = color[2];
                imageData.data[idx+3] = color[3];
            }
        }
        ctx.putImageData(imageData, 0, 0);
        return canvas.toBuffer('image/png');
    }

    exportCur(filename) {
        const png = this.toImage();
        // .cur — упрощённо сохраняем как PNG
        fs.writeFileSync(filename.replace('.cur', '.png'), png);
        console.log(chalk.green(`Экспортировано в ${filename} (как PNG)`));
    }

    toJSON() {
        return {
            size: this.size,
            hotspot: this.hotspot,
            pixels: this.pixels,
            palette: this.palette
        };
    }

    static fromJSON(data) {
        const editor = new CursorEditor(data.size);
        editor.pixels = data.pixels;
        editor.hotspot = data.hotspot;
        editor.palette = data.palette || [1, 2, 3, 4, 5, 6, 7, 8];
        return editor;
    }

    preview() {
        console.log(chalk.cyan(`Предпросмотр курсора (${this.size}x${this.size}), hotspot: ${this.hotspot}`));
        const chars = ['·', '█', '▓', '▒', '░', ' '];
        for (let y = 0; y < this.size; y++) {
            let line = '';
            for (let x = 0; x < this.size; x++) {
                const color = this.pixels[y][x];
                line += chars[Math.min(color, chars.length-1)] || ' ';
            }
            console.log(line);
        }
    }
}

program
    .option('--new', 'Создать новый курсор')
    .option('--size <number>', 'Размер курсора', parseInt, 32)
    .option('--load <file>', 'Загрузить проект из JSON')
    .option('--save <file>', 'Сохранить проект в JSON')
    .option('--export <file>', 'Экспортировать в файл')
    .option('--hotspot <x,y>', 'Точка активации')
    .option('--color <hex>', 'Цвет для рисования')
    .option('--pixel <x,y>', 'Установить пиксель', collect)
    .option('--fill <x,y>', 'Залить область')
    .option('--preview', 'Показать предпросмотр')
    .parse(process.argv);

function collect(value, previous) {
    return previous.concat([value]);
}

const opts = program.opts();
let editor = null;

if (opts.load) {
    const data = JSON.parse(fs.readFileSync(opts.load, 'utf8'));
    editor = CursorEditor.fromJSON(data);
} else if (opts.new) {
    editor = new CursorEditor(opts.size);
} else {
    editor = new CursorEditor(32);
}

if (opts.hotspot) {
    const [x, y] = opts.hotspot.split(',').map(Number);
    editor.hotspot = [x, y];
}

if (opts.color) {
    const colorMap = {
        '#000000': 1, '#FFFFFF': 2, '#FF0000': 3,
        '#00FF00': 4, '#0000FF': 5, '#FFFF00': 6,
        '#FF00FF': 7, '#00FFFF': 8
    };
    editor.currentColor = colorMap[opts.color.toUpperCase()] || 1;
}

if (opts.pixel) {
    for (const p of opts.pixel) {
        const [x, y] = p.split(',').map(Number);
        editor.setPixel(x, y, editor.currentColor);
    }
}

if (opts.fill) {
    const [x, y] = opts.fill.split(',').map(Number);
    editor.fill(x, y, editor.currentColor);
}

if (opts.save) {
    fs.writeFileSync(opts.save, JSON.stringify(editor.toJSON(), null, 2));
    console.log(chalk.green(`Проект сохранён в ${opts.save}`));
}

if (opts.export) {
    editor.exportCur(opts.export);
}

if (opts.preview) {
    editor.preview();
}

if (!process.argv.slice(2).length) {
    program.help();
}
