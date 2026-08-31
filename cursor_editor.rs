// cursor_editor.rs
use clap::{App, Arg};
use image::{ImageBuffer, Rgba, RgbaImage};
use serde::{Deserialize, Serialize};
use serde_json;
use std::collections::HashMap;
use std::fs;
use std::io::Write;
use colored::*;

#[derive(Serialize, Deserialize, Clone)]
struct CursorEditor {
    size: usize,
    pixels: Vec<Vec<usize>>,
    hotspot: (usize, usize),
    palette: Vec<usize>,
    #[serde(skip)]
    current_color: usize,
    #[serde(skip)]
    color_map: HashMap<usize, [u8; 4]>,
}

impl CursorEditor {
    fn new(size: usize) -> Self {
        let mut color_map = HashMap::new();
        color_map.insert(0, [255, 255, 255, 0]);
        color_map.insert(1, [0, 0, 0, 255]);
        color_map.insert(2, [255, 255, 255, 255]);
        color_map.insert(3, [255, 0, 0, 255]);
        color_map.insert(4, [0, 255, 0, 255]);
        color_map.insert(5, [0, 0, 255, 255]);
        color_map.insert(6, [255, 255, 0, 255]);
        color_map.insert(7, [255, 0, 255, 255]);
        color_map.insert(8, [0, 255, 255, 255]);
        let mut pixels = Vec::with_capacity(size);
        for _ in 0..size {
            pixels.push(vec![0; size]);
        }
        CursorEditor {
            size,
            pixels,
            hotspot: (0, 0),
            palette: vec![1, 2, 3, 4, 5, 6, 7, 8],
            current_color: 1,
            color_map,
        }
    }

    fn set_pixel(&mut self, x: usize, y: usize, color_idx: usize) {
        if x < self.size && y < self.size {
            self.pixels[y][x] = color_idx;
        }
    }

    fn get_pixel(&self, x: usize, y: usize) -> usize {
        if x < self.size && y < self.size {
            self.pixels[y][x]
        } else {
            0
        }
    }

    fn fill(&mut self, x: usize, y: usize, color_idx: usize) {
        let target = self.get_pixel(x, y);
        if target == color_idx { return; }
        let mut stack = vec![(x, y)];
        let mut visited = std::collections::HashSet::new();
        while let Some((cx, cy)) = stack.pop() {
            if visited.contains(&(cx, cy)) { continue; }
            visited.insert((cx, cy));
            if self.get_pixel(cx, cy) == target {
                self.set_pixel(cx, cy, color_idx);
                for (dx, dy) in &[(1, 0), (-1, 0), (0, 1), (0, -1)] {
                    let nx = cx as i32 + dx;
                    let ny = cy as i32 + dy;
                    if nx >= 0 && nx < self.size as i32 && ny >= 0 && ny < self.size as i32 {
                        stack.push((nx as usize, ny as usize));
                    }
                }
            }
        }
    }

    fn to_image(&self) -> RgbaImage {
        ImageBuffer::from_fn(self.size as u32, self.size as u32, |x, y| {
            let color = self.color_map.get(&self.pixels[y as usize][x as usize]).unwrap_or(&[0, 0, 0, 0]);
            Rgba([color[0], color[1], color[2], color[3]])
        })
    }

    fn export_cur(&self, filename: &str) -> Result<(), Box<dyn std::error::Error>> {
        let img = self.to_image();
        img.save(filename.replace(".cur", ".png"))?;
        println!("{}", format!("Экспортировано в {}", filename).green());
        Ok(())
    }

    fn preview(&self) {
        println!("{}", format!("Предпросмотр курсора ({}x{}), hotspot: {:?}", self.size, self.size, self.hotspot).cyan());
        let chars = ['·', '█', '▓', '▒', '░', ' '];
        for y in 0..self.size {
            let mut line = String::new();
            for x in 0..self.size {
                let idx = self.pixels[y][x];
                let ch = chars.get(idx).unwrap_or(&' ');
                line.push(*ch);
            }
            println!("{}", line);
        }
    }
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let matches = App::new("Cursor Editor")
        .arg(Arg::with_name("new").long("new").help("Создать новый курсор"))
        .arg(Arg::with_name("size").long("size").takes_value(true).default_value("32"))
        .arg(Arg::with_name("load").long("load").takes_value(true).help("Загрузить проект из JSON"))
        .arg(Arg::with_name("save").long("save").takes_value(true).help("Сохранить проект в JSON"))
        .arg(Arg::with_name("export").long("export").takes_value(true).help("Экспортировать в файл"))
        .arg(Arg::with_name("hotspot").long("hotspot").takes_value(true).help("Точка активации (x,y)"))
        .arg(Arg::with_name("color").long("color").takes_value(true).help("Цвет для рисования (HEX)"))
        .arg(Arg::with_name("pixel").long("pixel").takes_value(true).help("Установить пиксель (x,y)"))
        .arg(Arg::with_name("fill").long("fill").takes_value(true).help("Залить область (x,y)"))
        .arg(Arg::with_name("preview").long("preview").help("Показать предпросмотр"))
        .get_matches();

    let mut editor = if let Some(file) = matches.value_of("load") {
        let data = fs::read_to_string(file)?;
        let mut e: CursorEditor = serde_json::from_str(&data)?;
        e.color_map = HashMap::new();
        e.color_map.insert(0, [255, 255, 255, 0]);
        e.color_map.insert(1, [0, 0, 0, 255]);
        e.color_map.insert(2, [255, 255, 255, 255]);
        e.color_map.insert(3, [255, 0, 0, 255]);
        e.color_map.insert(4, [0, 255, 0, 255]);
        e.color_map.insert(5, [0, 0, 255, 255]);
        e.color_map.insert(6, [255, 255, 0, 255]);
        e.color_map.insert(7, [255, 0, 255, 255]);
        e.color_map.insert(8, [0, 255, 255, 255]);
        e.current_color = 1;
        e
    } else if matches.is_present("new") {
        let size: usize = matches.value_of("size").unwrap().parse()?;
        CursorEditor::new(size)
    } else {
        CursorEditor::new(32)
    };

    if let Some(hs) = matches.value_of("hotspot") {
        let parts: Vec<&str> = hs.split(',').collect();
        if parts.len() == 2 {
            let x = parts[0].parse().unwrap_or(0);
            let y = parts[1].parse().unwrap_or(0);
            editor.hotspot = (x, y);
        }
    }

    if let Some(color) = matches.value_of("color") {
        let color_map = std::collections::HashMap::from([
            ("#000000", 1), ("#FFFFFF", 2), ("#FF0000", 3),
            ("#00FF00", 4), ("#0000FF", 5), ("#FFFF00", 6),
            ("#FF00FF", 7), ("#00FFFF", 8)
        ]);
        if let Some(&c) = color_map.get(color) {
            editor.current_color = c;
        }
    }

    if let Some(pixel) = matches.value_of("pixel") {
        let parts: Vec<&str> = pixel.split(',').collect();
        if parts.len() == 2 {
            let x = parts[0].parse().unwrap_or(0);
            let y = parts[1].parse().unwrap_or(0);
            editor.set_pixel(x, y, editor.current_color);
        }
    }

    if let Some(fill) = matches.value_of("fill") {
        let parts: Vec<&str> = fill.split(',').collect();
        if parts.len() == 2 {
            let x = parts[0].parse().unwrap_or(0);
            let y = parts[1].parse().unwrap_or(0);
            editor.fill(x, y, editor.current_color);
        }
    }

    if let Some(file) = matches.value_of("save") {
        let json = serde_json::to_string_pretty(&editor)?;
        fs::write(file, json)?;
        println!("{}", format!("Проект сохранён в {}", file).green());
    }

    if let Some(file) = matches.value_of("export") {
        editor.export_cur(file)?;
    }

    if matches.is_present("preview") {
        editor.preview();
    }

    if !matches.is_present("new") && matches.value_of("load").is_none() &&
       matches.value_of("save").is_none() && matches.value_of("export").is_none() &&
       !matches.is_present("preview") && matches.value_of("pixel").is_none() &&
       matches.value_of("fill").is_none() {
        println!("Используйте --help для справки.");
    }

    Ok(())
}
