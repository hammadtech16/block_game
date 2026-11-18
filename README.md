# Block Game - Android

A fast-paced block matching game for Android where you tap same-colored blocks to destroy them before they reach the bottom.

## Gameplay

- **Objective**: Tap 2 or more blocks of the same color to destroy them and earn points
- **Blocks**: Different shapes (Square, Circle, Triangle, Hexagon, Star, Diamond) with different point values
- **Danger**: Blocks continuously move down toward the striker at the bottom
- **Game Over**: If any block reaches the striker, the game ends
- **Levels**: Complete levels by earning enough points, each level increases speed and difficulty

## Features

- 6 unique block shapes with distinct point values
- 7 vibrant block colors
- Particle explosion animations on block destruction
- Combo bonus points for destroying multiple same-colored blocks
- Progressive difficulty (faster spawns, higher speeds)
- Level completion screen with points earned
- Full-screen immersive gameplay
- Smooth 60 FPS game loop

## Block Types & Points

| Shape | Points |
|-------|--------|
| Square | 10 |
| Circle | 15 |
| Triangle | 20 |
| Hexagon | 25 |
| Star | 30 |
| Diamond | 20 |

## Building & Running

### Using Android Studio (Recommended)
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to `D:\blockGame\block_game`
4. Wait for Gradle sync
5. Click Run (green play button) or press `Shift+F10`

### Using Command Line
```bash
cd D:\blockGame\block_game
./gradlew assembleDebug
# Install on connected device
./gradlew installDebug
```

## Project Structure

```
block_game/
├── app/
│   ├── src/main/
│   │   ├── java/com/blockgame/
│   │   │   ├── MainActivity.kt      # Entry point, game loop
│   │   │   └── game/
│   │   │       ├── Block.kt         # Block model with shapes
│   │   │       ├── BlockShape.kt    # Shape enum
│   │   │       ├── Particle.kt      # Destruction particles
│   │   │       ├── Striker.kt       # Player striker/pointer
│   │   │       └── GameView.kt      # Main game logic & rendering
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
```

## Controls

- **Touch/Drag**: Move the striker horizontally
- **Tap on blocks**: Select blocks of the same color (need 2+ to destroy)
- **Tap on Game Over/Level Complete**: Restart/Continue

## Technical Details

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34
- **Language**: Kotlin
- **Rendering**: Custom View with Canvas API
- **Game Loop**: Fixed timestep (~60 FPS)
- **Architecture**: Single Activity, custom View

## Future Improvements

- [ ] Add sound effects and background music
- [ ] Add power-ups (bombs, slow motion, color changer)
- [ ] Add leaderboard and achievements
- [ ] Add more block shapes and special blocks
- [ ] Add tutorial/onboarding
- [ ] Add settings (difficulty, sound, vibration)
- [ ] Add daily challenges
- [ ] Improve particle effects with shaders