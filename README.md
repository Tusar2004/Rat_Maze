# 🌑🐭 ALGORITHM RAT

<p align="center">
  <img src="https://media.giphy.com/media/l0HlBO7eyXzSZkJri/giphy.gif" width="180" alt="Animated Rat"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-11+-0f0f0f?style=for-the-badge&logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/Swing-Graphics2D-0f0f0f?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/FPS-60-0f0f0f?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/License-MIT-0f0f0f?style=for-the-badge"/>
</p>

<p align="center">
  <b>A Real-Time 2D AI Pathfinding Game Engine</b><br>
  Watch a rat navigate a braided maze using classic algorithms — rendered at 60 FPS.
</p>

---

## 🌌 Preview

<p align="center">
  <img src="assets/demo.gif" width="700" alt="Algorithm Rat Gameplay"/>
</p>

> Not a static grid demo.  
> Not a button-based tracer.  
> A fully animated game-loop simulation.

---

# 🎮 Features

- 🐀 Smooth pixel-space movement (delta-time physics)
- ✨ Animated visited-node glow with fade-out
- 📊 Real-time statistics panel
- 🏆 Algorithm comparison leaderboard
- 🧠 Four classic search algorithms
- 🔁 Braided maze (multiple valid paths)
- 🧩 Clean architecture with zero UI coupling in algorithms
- 🎥 Fixed 60 FPS game loop

---

# 🧠 Algorithms Implemented

| Algorithm  | Optimal | Weighted | Behaviour |
|------------|----------|-----------|------------|
| **A\***     | ✓        | ✓         | Heuristic-driven, minimal exploration |
| **BFS**     | ✓ (steps)| ✗         | Uniform wave expansion |
| **Dijkstra**| ✓ (cost) | ✓         | Avoids expensive terrain |
| **DFS**     | ✗        | ✗         | Deep wandering with backtracking |

Because the maze is braided, each algorithm visibly chooses a different path.

---

# 🗺️ Maze System

### Grid Size
`25 × 25`

### Generation
Recursive Backtracking DFS → Braiding Pass (~18% wall removal)

---

## 🎨 Tile Types

| Tile    | Style                | Cost |
|----------|----------------------|------|
| Floor    | Dark checker pattern | 1    |
| Mud      | Brown textured       | 5    |
| Water    | Animated shimmer     | 10   |
| Wall     | Beveled 3D block     | ∞    |
| Visited  | Blue radial glow     | —    |
| Path     | Green → Yellow trail | —    |

---

# 📊 Live Statistics Panel

After each run:
Algorithm: A*
Nodes Explored: 143
Path Steps: 36
Total Cost: 41
Execution Time: 3 ms
Optimal: YES


### Comparison Table

- 🟢 Best cost highlighted
- 🔴 Worst cost highlighted

---

# 🏗️ Project Architecture
RATMAZE/src/
├── main/
│ └── MainFrame.java
│
├── engine/
│ ├── GameLoop.java
│ ├── GamePanel.java
│ ├── Camera.java
│ └── InputHandler.java
│
├── maze/
│ ├── TileType.java
│ ├── Maze.java
│ └── MazeGenerator.java
│
├── entities/
│ ├── Entity.java
│ ├── Rat.java
│ └── Cheese.java
│
├── algorithms/
│ ├── PathFinder.java
│ ├── AStarPathFinder.java
│ ├── BFSPathFinder.java
│ ├── DijkstraPathFinder.java
│ └── DFSPathFinder.java
│
└── ui/
├── ControlPanel.java
└── StatsPanel.java 


---

# 🧩 Design Principles

- Algorithms fully decoupled from rendering
- Game loop controls all animation
- No `Thread.sleep` inside entities
- Delta-time ensures frame-rate independence
- Double-buffered rendering prevents flicker
- Clean separation between engine, maze, entities, and UI

---

# 🎥 Visual Highlights

- 🌌 90-star animated background
- 🌊 Dual sine-wave water animation
- 🧱 Bevel-lit walls
- 🌱 Deterministic mud textures
- 🐀 Fully vector-drawn animated rat
- 🧀 Spinning cheese celebration
- ✨ Radial visited glow

---

# ⚙️ Build & Run

## Requirements
Java 11+

## Check Version
``powershell
java -version 

Compile & Run
cd "c:\Users\tusar\OneDrive\Desktop\RATMAZE"
javac -d out -sourcepath src (Get-ChildItem -Recurse -Filter "*.java" src | Select-Object -ExpandProperty FullName)
java -cp out main.MainFrame

After first compile:

java -cp "c:\Users\tusar\OneDrive\Desktop\RATMAZE\out" main.MainFrame

Recompile only when source files change.

🔧 Configuration
Constant	File	Default
BRAID_CHANCE	MazeGenerator.java	0.18
TILE_SIZE	Maze.java	28
TARGET_FPS	GameLoop.java	60
FLASH_FADE_MS	GamePanel.java	1200
TRAIL_MAX	Rat.java	16
➕ Add a New Algorithm

Implement:

public interface PathFinder {
    List<Point> findPath(Maze maze, Point start, Point goal);
    String getName();
    int getNodesExplored();
    long getExecutionTimeMs();
    boolean isOptimal();
}

Register it inside ControlPanel.java.

🚀 Why This Project Stands Out

Real-time simulation architecture

Game-engine-style loop

Proper delta-time physics

Clean algorithm abstraction

Meaningful performance comparison

Visually polished beyond academic demos

Extendable for future AI strategies

🖤 License

MIT — free to use, modify, and distribute.

<p align="center"> <b>Built for learning. Designed like a game engine.</b><br> 🌑🐭✨ </p> ```
