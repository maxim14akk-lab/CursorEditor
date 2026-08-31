# Редактор курсоров (предпросмотр)

Многоязычная утилита для создания, редактирования и предварительного просмотра пользовательских курсоров.  
Позволяет рисовать курсоры по пикселям, выбирать цвета, задавать hotspot (точку активации) и экспортировать в форматы .cur и .ani.

## Особенности
- Интерактивный редактор пиксельных курсоров (сетка 32×32, 48×48, 64×64).
- Палитра цветов (16 базовых + пользовательские).
- Инструменты: карандаш, заливка, ластик, пипетка.
- Настройка hotspot (точки, на которую кликает курсор).
- Предпросмотр в реальном времени с анимацией (для .ani).
- Экспорт в форматы .cur (статический) и .ani (анимированный).
- Поддержка нескольких фреймов для анимированных курсоров.
- Сохранение и загрузка проектов в JSON.
- Поддержка аргументов командной строки для пакетного экспорта.
- Кроссплатформенность (Windows, Linux, macOS).

## Установка и запуск
Для каждого языка требуются соответствующие инструменты и зависимости.

### Запуск на разных языках

1. **Python**  
   Установка: `pip install pillow colorama`  
   Запуск: `python cursor_editor.py --new --size 32 --export cursor.cur`

2. **JavaScript (Node.js)**  
   Установка: `npm install commander chalk`  
   Запуск: `node cursor_editor.js --new --size 32 --export cursor.cur`

3. **Go**  
   Установка: `go get github.com/fogleman/gg`  
   Запуск: `go run cursor_editor.go --new --size 32 --export cursor.cur`

4. **Rust**  
   Добавьте `clap`, `image`, `serde`, `serde_json` в `Cargo.toml`.  
   Запуск: `cargo run -- --new --size 32 --export cursor.cur`

5. **Java**  
   Сборка: `javac -cp gson.jar CursorEditor.java`  
   Запуск: `java -cp .;gson.jar CursorEditor --new --size 32 --export cursor.cur`

6. **C# (.NET Core)**  
   Установка: `dotnet add package Newtonsoft.Json` и `System.Drawing.Common`  
   Запуск: `dotnet run -- --new --size 32 --export cursor.cur`

7. **C++ (Linux)**  
   Требуется libpng, libjpeg, nlohmann/json.  
   Сборка: `g++ -std=c++11 -o cursor_editor cursor_editor.cpp -lpng -ljpeg -ljsoncpp`  
   Запуск: `./cursor_editor --new --size 32 --export cursor.cur`

8. **Kotlin (JVM)**  
   Сборка: `kotlinc -cp gson.jar CursorEditor.kt` (использует Java AWT).  
   Запуск: `kotlin -cp .;gson.jar CursorEditorKt --new --size 32 --export cursor.cur`

## Использование

Общие аргументы командной строки (везде, где поддерживается):

- `--new` – создать новый курсор (требует `--size`).
- `--size <32|48|64>` – размер курсора (по умолчанию 32).
- `--load <файл>` – загрузить проект из JSON.
- `--save <файл>` – сохранить проект в JSON.
- `--export <файл>` – экспортировать в .cur или .ani (по расширению).
- `--hotspot <x,y>` – задать точку активации.
- `--color <HEX>` – выбрать цвет для рисования.
- `--pixel <x,y>` – установить пиксель в текущий цвет.
- `--fill <x,y>` – залить область цветом.
- `--preview` – показать предпросмотр курсора в консоли.
- `--help` – справка.

Пример (Python):
```bash
python cursor_editor.py --new --size 32 --hotspot 16,16 --color FF0000 --pixel 0,0 --pixel 1,0 --export my_cursor.cur
Структура репозитория
text
/
├── README.md
├── cursor_editor.py
├── cursor_editor.js
├── cursor_editor.go
├── cursor_editor.rs
├── CursorEditor.java
├── CursorEditor.cs
├── cursor_editor.cpp
└── CursorEditor.kt
Лицензия
MIT
