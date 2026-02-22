# 🐭 Algorithm Rat

> A real-time 2D AI pathfinding game built with Java Swing.
> Watch a rat navigate a braided maze to reach the cheese — using four different algorithms side by side.

---

## 📸 Overview

**Algorithm Rat** is an animated, game-loop-driven pathfinding visualizer. It is **not** a button-grid demo — the rat moves smoothly through pixel space at 60 FPS, driven by delta-time physics.

Select an algorithm, press **START**, and observe:
- Visited cells glowing and fading in real time
- The rat walking (legs, head bob, tail sway) along the found path
- Statistics counting up live on the right panel
- Algorithm comparison table highlighting the best and worst performers

---

## 🎮 How to Play

| Button | Action |
|---|---|
| **▶ START** | Run the selected algorithm and animate the rat |
| **↺ RESET** | Return rat to start, clear path and stats |
| **⚙ NEW MAZE** | Generate a fresh braided maze |
| **Algorithm dropdown** | Switch between DFS / BFS / Dijkstra / A* |
| **Rat Speed slider** | Adjust movement speed (40 – 280 px/s) |

---

## 🧠 Algorithms

| Algorithm | Optimal? | Tile Weights | Characteristic Behaviour |
|---|---|---|---|
| **A\*** | ✓ | Yes | Heuristic-guided, fewest nodes explored |
| **BFS** | ✓ (hops) | No | Uniform wave, fewest steps regardless of cost |
| **Dijkstra** | ✓ (cost) | Yes | Avoids mud & water, finds lowest-cost path |
| **DFS** | ✗ | No | Wanders, backtracks, usually finds a long path |

Because the maze is **braided** (multiple valid routes exist), each algorithm genuinely picks a different path, making the comparison meaningful.

---

## 🗺️ Maze & Tile Types

**Size:** 25 × 25 grid  
**Generation:** Recursive-backtracking DFS → braiding pass (≈18 % wall removal)

| Tile | Colour | Movement Cost |
|---|---|---|
| Floor | Dark blue-grey | 1 |
| Mud | Brown + texture | 5 |
| Water | Animated blue | 10 |
| Wall | Dark bevel block | Impassable |
| Visited | Blue radial glow | — |
| Path | Green → yellow trail | — |

---

## 📊 Statistics Panel

After each run the right panel shows:

- **Algorithm name**
- **Nodes explored** (animated count-up)
- **Path steps** (animated count-up)
- **Total weighted cost** (animated count-up)
- **Execution time** in ms
- **Optimal?** yes / no

The **Comparison Table** tracks all four algorithms across runs and highlights the **best cost in green** and **worst cost in red**.

---

## 🏗️ Project Architecture

```
RATMAZE/src/
├── main/
│   └── MainFrame.java          ← Entry point & JFrame assembly
│
├── engine/
│   ├── GameLoop.java           ← Fixed 60 FPS loop, delta time
│   ├── GamePanel.java          ← Double-buffered Graphics2D renderer
│   ├── Camera.java             ← Viewport centering
│   └── InputHandler.java       ← Keyboard / mouse events
│
├── maze/
│   ├── TileType.java           ← WALL / NORMAL / MUD / WATER + costs
│   ├── Maze.java               ← Grid data, visited tracking, neighbour lookup
│   └── MazeGenerator.java      ← DFS carver + braiding pass
│
├── entities/
│   ├── Entity.java             ← Base class (pixel + tile position)
│   ├── Rat.java                ← Walking legs, head bob, breathing, tail, trail
│   └── Cheese.java             ← Triangular wedge, glow pulse, spin celebration
│
├── algorithms/
│   ├── PathFinder.java         ← Interface: findPath() + stats getters
│   ├── AStarPathFinder.java    ← A* with Manhattan heuristic
│   ├── BFSPathFinder.java      ← Breadth-first search
│   ├── DijkstraPathFinder.java ← Priority-queue Dijkstra
│   └── DFSPathFinder.java      ← Iterative depth-first search
│
└── ui/
    ├── ControlPanel.java       ← Left panel: controls & legend
    └── StatsPanel.java         ← Right panel: stat cards & comparison table
```

### Key design principles

- **Algorithms are fully decoupled from rendering.** They run synchronously, return a `List<Point>`, and never call any UI code.
- **Game loop drives all animation.** No `Thread.sleep` inside entities or algorithms.
- **Delta time** ensures movement is frame-rate independent.
- **Double-buffered rendering** via `BufferedImage` prevents flicker.

---

## ⚙️ Build & Run

### Requirements
- **Java 11 or higher** — [Download Temurin JDK](https://adoptium.net/)

### Check Java version
```powershell
java -version
```

### Compile + Run (one command)
```powershell
cd "c:\Users\tusar\OneDrive\Desktop\RATMAZE"
javac -d out -sourcepath src (Get-ChildItem -Recurse -Filter "*.java" src | Select-Object -ExpandProperty FullName)
java -cp out main.MainFrame
```

### After the first compile, just run:
```powershell
java -cp "c:\Users\tusar\OneDrive\Desktop\RATMAZE\out" main.MainFrame
```

> Only recompile when source files change.

---

## 🔧 Configuration

| Constant | File | Default | Effect |
|---|---|---|---|
| `BRAID_CHANCE` | `MazeGenerator.java` | `0.18` | Wall removal rate (0.10 = sparse loops, 0.30 = many loops) |
| `TILE_SIZE` | `Maze.java` | `28` | Pixel size of each grid cell |
| `TARGET_FPS` | `GameLoop.java` | `60` | Render / update rate |
| `FLASH_FADE_MS` | `GamePanel.java` | `1200` | Duration of visited-cell glow |
| `TRAIL_MAX` | `Rat.java` | `16` | Length of the rat's motion trail |

---

## 📐 Algorithm Interface

All pathfinders implement `PathFinder`:

```java
public interface PathFinder {
    List<Point> findPath(Maze maze, Point start, Point goal);
    String  getName();
    int     getNodesExplored();
    long    getExecutionTimeMs();
    boolean isOptimal();
}
```

To add a new algorithm, create a class implementing `PathFinder` and add it to the `finders[]` array in `ControlPanel.java`.

---

## 🎨 Visual Features

- **Starfield background** — 90 twinkling stars, sine-wave brightness
- **Checker floor** — alternating dark shades per tile
- **Bevel walls** — bright top-left edge, dark bottom-right edge, inner shadow
- **Mud texture** — 3 deterministic oval clumps per cell (seeded by position)
- **Animated water** — dual sine-wave colour shift + shimmer stripe
- **Rat character** — vector-drawn with walking legs, bezier tail, whiskers, specular eyes
- **Cheese** — `Path2D` triangle wedge with gradient, rimmed holes, spin on arrival

---

## 📄 License

MIT — free to use, modify, and distribute.
