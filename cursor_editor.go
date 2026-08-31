// cursor_editor.go
package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"image"
	"image/color"
	"image/png"
	"os"
	"strconv"
	"strings"
)

type CursorEditor struct {
	Size         int       `json:"size"`
	Pixels       [][]int   `json:"pixels"`
	Hotspot      [2]int    `json:"hotspot"`
	Palette      []int     `json:"palette"`
	CurrentColor int       `json:"-"`
	ColorMap     map[int][4]int `json:"-"`
}

func NewCursorEditor(size int) *CursorEditor {
	return &CursorEditor{
		Size:    size,
		Pixels:  make([][]int, size),
		Hotspot: [2]int{0, 0},
		Palette: []int{1, 2, 3, 4, 5, 6, 7, 8},
		ColorMap: map[int][4]int{
			0: {255, 255, 255, 0},
			1: {0, 0, 0, 255},
			2: {255, 255, 255, 255},
			3: {255, 0, 0, 255},
			4: {0, 255, 0, 255},
			5: {0, 0, 255, 255},
			6: {255, 255, 0, 255},
			7: {255, 0, 255, 255},
			8: {0, 255, 255, 255},
		},
		CurrentColor: 1,
	}
}

func (e *CursorEditor) initPixels() {
	for i := range e.Pixels {
		e.Pixels[i] = make([]int, e.Size)
	}
}

func (e *CursorEditor) setPixel(x, y, colorIdx int) {
	if x >= 0 && x < e.Size && y >= 0 && y < e.Size {
		e.Pixels[y][x] = colorIdx
	}
}

func (e *CursorEditor) getPixel(x, y int) int {
	if x >= 0 && x < e.Size && y >= 0 && y < e.Size {
		return e.Pixels[y][x]
	}
	return 0
}

func (e *CursorEditor) fill(x, y, colorIdx int) {
	target := e.getPixel(x, y)
	if target == colorIdx {
		return
	}
	stack := [][2]int{{x, y}}
	visited := make(map[string]bool)
	for len(stack) > 0 {
		pos := stack[len(stack)-1]
		stack = stack[:len(stack)-1]
		cx, cy := pos[0], pos[1]
		key := fmt.Sprintf("%d,%d", cx, cy)
		if visited[key] {
			continue
		}
		visited[key] = true
		if e.getPixel(cx, cy) == target {
			e.setPixel(cx, cy, colorIdx)
			for _, d := range [][2]int{{1,0},{-1,0},{0,1},{0,-1}} {
				nx, ny := cx+d[0], cy+d[1]
				if nx >= 0 && nx < e.Size && ny >= 0 && ny < e.Size {
					stack = append(stack, [2]int{nx, ny})
				}
			}
		}
	}
}

func (e *CursorEditor) toImage() *image.RGBA {
	img := image.NewRGBA(image.Rect(0, 0, e.Size, e.Size))
	for y := 0; y < e.Size; y++ {
		for x := 0; x < e.Size; x++ {
			colorIdx := e.Pixels[y][x]
			c := e.ColorMap[colorIdx]
			img.Set(x, y, color.RGBA{uint8(c[0]), uint8(c[1]), uint8(c[2]), uint8(c[3])})
		}
	}
	return img
}

func (e *CursorEditor) exportCur(filename string) error {
	img := e.toImage()
	f, err := os.Create(filename[:len(filename)-4] + ".png")
	if err != nil {
		return err
	}
	defer f.Close()
	return png.Encode(f, img)
}

func (e *CursorEditor) toJSON() ([]byte, error) {
	return json.MarshalIndent(e, "", "  ")
}

func (e *CursorEditor) fromJSON(data []byte) error {
	return json.Unmarshal(data, e)
}

func (e *CursorEditor) preview() {
	fmt.Printf("\033[36mПредпросмотр курсора (%dx%d), hotspot: %v\033[0m\n", e.Size, e.Size, e.Hotspot)
	chars := []string{"·", "█", "▓", "▒", "░", " "}
	for y := 0; y < e.Size; y++ {
		line := ""
		for x := 0; x < e.Size; x++ {
			colorIdx := e.Pixels[y][x]
			if colorIdx >= 0 && colorIdx < len(chars) {
				line += chars[colorIdx]
			} else {
				line += " "
			}
		}
		fmt.Println(line)
	}
}

func main() {
	var (
		newFlag  bool
		size     int
		loadFile string
		saveFile string
		export   string
		hotspot  string
		color    string
		pixel    string
		fill     string
		preview  bool
	)
	flag.BoolVar(&newFlag, "new", false, "Создать новый курсор")
	flag.IntVar(&size, "size", 32, "Размер курсора")
	flag.StringVar(&loadFile, "load", "", "Загрузить проект из JSON")
	flag.StringVar(&saveFile, "save", "", "Сохранить проект в JSON")
	flag.StringVar(&export, "export", "", "Экспортировать в файл")
	flag.StringVar(&hotspot, "hotspot", "", "Точка активации (x,y)")
	flag.StringVar(&color, "color", "", "Цвет для рисования (HEX)")
	flag.StringVar(&pixel, "pixel", "", "Установить пиксель (x,y)")
	flag.StringVar(&fill, "fill", "", "Залить область (x,y)")
	flag.BoolVar(&preview, "preview", false, "Показать предпросмотр")
	flag.Parse()

	var editor *CursorEditor

	if loadFile != "" {
		data, err := os.ReadFile(loadFile)
		if err == nil {
			editor = &CursorEditor{}
			editor.ColorMap = map[int][4]int{
				0: {255, 255, 255, 0}, 1: {0, 0, 0, 255},
				2: {255, 255, 255, 255}, 3: {255, 0, 0, 255},
				4: {0, 255, 0, 255}, 5: {0, 0, 255, 255},
				6: {255, 255, 0, 255}, 7: {255, 0, 255, 255},
				8: {0, 255, 255, 255},
			}
			json.Unmarshal(data, editor)
		}
	} else if newFlag || size > 0 {
		editor = NewCursorEditor(size)
		editor.initPixels()
	} else {
		editor = NewCursorEditor(32)
		editor.initPixels()
	}

	if hotspot != "" {
		parts := strings.Split(hotspot, ",")
		if len(parts) == 2 {
			x, _ := strconv.Atoi(parts[0])
			y, _ := strconv.Atoi(parts[1])
			editor.Hotspot = [2]int{x, y}
		}
	}

	if color != "" {
		colorMap := map[string]int{
			"#000000": 1, "#FFFFFF": 2, "#FF0000": 3,
			"#00FF00": 4, "#0000FF": 5, "#FFFF00": 6,
			"#FF00FF": 7, "#00FFFF": 8,
		}
		if c, ok := colorMap[color]; ok {
			editor.CurrentColor = c
		}
	}

	if pixel != "" {
		parts := strings.Split(pixel, ",")
		if len(parts) == 2 {
			x, _ := strconv.Atoi(parts[0])
			y, _ := strconv.Atoi(parts[1])
			editor.setPixel(x, y, editor.CurrentColor)
		}
	}

	if fill != "" {
		parts := strings.Split(fill, ",")
		if len(parts) == 2 {
			x, _ := strconv.Atoi(parts[0])
			y, _ := strconv.Atoi(parts[1])
			editor.fill(x, y, editor.CurrentColor)
		}
	}

	if saveFile != "" {
		data, _ := editor.toJSON()
		os.WriteFile(saveFile, data, 0644)
		fmt.Printf("\033[32mПроект сохранён в %s\033[0m\n", saveFile)
	}

	if export != "" {
		if err := editor.exportCur(export); err != nil {
			fmt.Fprintf(os.Stderr, "Ошибка экспорта: %v\n", err)
		} else {
			fmt.Printf("\033[32mЭкспортировано в %s\033[0m\n", export)
		}
	}

	if preview {
		editor.preview()
	}

	if !newFlag && loadFile == "" && saveFile == "" && export == "" && !preview && pixel == "" && fill == "" {
		fmt.Println("Используйте -h для справки")
	}
}
