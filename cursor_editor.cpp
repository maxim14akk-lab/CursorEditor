// cursor_editor.cpp
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <map>
#include <set>
#include <sstream>
#include <cstring>
#include <json/json.h> // using jsoncpp

using namespace std;

class CursorEditor {
private:
    int size;
    vector<vector<int>> pixels;
    pair<int,int> hotspot;
    vector<int> palette;
    int currentColor;
    map<int, vector<int>> colorMap;

public:
    CursorEditor(int sz = 32) : size(sz), hotspot(0,0), currentColor(1) {
        colorMap[0] = {255, 255, 255, 0};
        colorMap[1] = {0, 0, 0, 255};
        colorMap[2] = {255, 255, 255, 255};
        colorMap[3] = {255, 0, 0, 255};
        colorMap[4] = {0, 255, 0, 255};
        colorMap[5] = {0, 0, 255, 255};
        colorMap[6] = {255, 255, 0, 255};
        colorMap[7] = {255, 0, 255, 255};
        colorMap[8] = {0, 255, 255, 255};
        palette = {1, 2, 3, 4, 5, 6, 7, 8};
        initPixels();
    }

    void initPixels() {
        pixels.assign(size, vector<int>(size, 0));
    }

    void setPixel(int x, int y, int colorIdx) {
        if (x >= 0 && x < size && y >= 0 && y < size) {
            pixels[y][x] = colorIdx;
        }
    }

    int getPixel(int x, int y) {
        if (x >= 0 && x < size && y >= 0 && y < size) {
            return pixels[y][x];
        }
        return 0;
    }

    void fill(int x, int y, int colorIdx) {
        int target = getPixel(x, y);
        if (target == colorIdx) return;
        vector<pair<int,int>> stack;
        set<pair<int,int>> visited;
        stack.push_back({x, y});
        while (!stack.empty()) {
            auto pos = stack.back();
            stack.pop_back();
            int cx = pos.first, cy = pos.second;
            if (visited.find({cx, cy}) != visited.end()) continue;
            visited.insert({cx, cy});
            if (getPixel(cx, cy) == target) {
                setPixel(cx, cy, colorIdx);
                int dirs[4][2] = {{1,0},{-1,0},{0,1},{0,-1}};
                for (auto& d : dirs) {
                    int nx = cx + d[0], ny = cy + d[1];
                    if (nx >= 0 && nx < size && ny >= 0 && ny < size) {
                        stack.push_back({nx, ny});
                    }
                }
            }
        }
    }

    void preview() {
        cout << "\033[36mПредпросмотр курсора (" << size << "x" << size << "), hotspot: (" << hotspot.first << "," << hotspot.second << ")\033[0m" << endl;
        string chars = "·█▓▒░ ";
        for (int y = 0; y < size; ++y) {
            string line;
            for (int x = 0; x < size; ++x) {
                int idx = pixels[y][x];
                if (idx >= 0 && idx < (int)chars.size()) {
                    line += chars[idx];
                } else {
                    line += ' ';
                }
            }
            cout << line << endl;
        }
    }

    void exportCur(const string& filename) {
        // Упрощённо: сохраняем как PPM (можно конвертировать в PNG)
        string pngName = filename.substr(0, filename.find_last_of('.')) + ".ppm";
        ofstream ofs(pngName);
        ofs << "P3\n" << size << " " << size << "\n255\n";
        for (int y = 0; y < size; ++y) {
            for (int x = 0; x < size; ++x) {
                auto c = colorMap[pixels[y][x]];
                ofs << c[0] << " " << c[1] << " " << c[2] << " ";
            }
            ofs << "\n";
        }
        cout << "\033[32mЭкспортировано в " << filename << " (как PPM)\033[0m" << endl;
    }

    Json::Value toJSON() {
        Json::Value root;
        root["size"] = size;
        root["hotspot"][0] = hotspot.first;
        root["hotspot"][1] = hotspot.second;
        for (int i = 0; i < (int)palette.size(); ++i) {
            root["palette"][i] = palette[i];
        }
        for (int y = 0; y < size; ++y) {
            for (int x = 0; x < size; ++x) {
                root["pixels"][y][x] = pixels[y][x];
            }
        }
        return root;
    }

    void fromJSON(const Json::Value& root) {
        size = root["size"].asInt();
        hotspot.first = root["hotspot"][0].asInt();
        hotspot.second = root["hotspot"][1].asInt();
        palette.clear();
        for (const auto& item : root["palette"]) {
            palette.push_back(item.asInt());
        }
        pixels.assign(size, vector<int>(size, 0));
        for (int y = 0; y < size; ++y) {
            for (int x = 0; x < size; ++x) {
                pixels[y][x] = root["pixels"][y][x].asInt();
            }
        }
    }
};

int main(int argc, char* argv[]) {
    bool newFlag = false, preview = false;
    int size = 32;
    string loadFile, saveFile, exportFile, hotspot, color, pixel, fill;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "--new") newFlag = true;
        else if (arg == "--size" && i+1 < argc) size = stoi(argv[++i]);
        else if (arg == "--load" && i+1 < argc) loadFile = argv[++i];
        else if (arg == "--save" && i+1 < argc) saveFile = argv[++i];
        else if (arg == "--export" && i+1 < argc) exportFile = argv[++i];
        else if (arg == "--hotspot" && i+1 < argc) hotspot = argv[++i];
        else if (arg == "--color" && i+1 < argc) color = argv[++i];
        else if (arg == "--pixel" && i+1 < argc) pixel = argv[++i];
        else if (arg == "--fill" && i+1 < argc) fill = argv[++i];
        else if (arg == "--preview") preview = true;
    }

    CursorEditor editor(size);

    if (!loadFile.empty()) {
        ifstream ifs(loadFile);
        Json::Value root;
        ifs >> root;
        editor.fromJSON(root);
    } else if (newFlag) {
        editor.initPixels();
    }

    if (!hotspot.empty()) {
        stringstream ss(hotspot);
        int x, y;
        char sep;
        ss >> x >> sep >> y;
        if (ss) {
            editor.hotspot = {x, y};
        }
    }

    if (!color.empty()) {
        map<string, int> cm = {
            {"#000000", 1}, {"#FFFFFF", 2}, {"#FF0000", 3},
            {"#00FF00", 4}, {"#0000FF", 5}, {"#FFFF00", 6},
            {"#FF00FF", 7}, {"#00FFFF", 8}
        };
        int c = cm[color];
        if (c != 0) editor.currentColor = c;
    }

    if (!pixel.empty()) {
        stringstream ss(pixel);
        int x, y;
        char sep;
        ss >> x >> sep >> y;
        if (ss) {
            editor.setPixel(x, y, editor.currentColor);
        }
    }

    if (!fill.empty()) {
        stringstream ss(fill);
        int x, y;
        char sep;
        ss >> x >> sep >> y;
        if (ss) {
            editor.fill(x, y, editor.currentColor);
        }
    }

    if (!saveFile.empty()) {
        Json::Value root = editor.toJSON();
        ofstream ofs(saveFile);
        ofs << root.toStyledString();
        cout << "\033[32mПроект сохранён в " << saveFile << "\033[0m" << endl;
    }

    if (!exportFile.empty()) {
        editor.exportCur(exportFile);
    }

    if (preview) {
        editor.preview();
    }

    if (!newFlag && loadFile.empty() && saveFile.empty() && exportFile.empty() && !preview && pixel.empty() && fill.empty()) {
        cout << "Используйте --help для справки." << endl;
    }

    return 0;
}
