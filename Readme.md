# Git Wreckers : Zombie Mission Simulation  

## Overview
What happens when a group of zombie infections spreads through a living community? Git Wreckers explores this through a 2D zombie outbreak simulation developed in Java, where humans search for food and shelter while zombies hunt and infect humans and military units attempt to control the outbreak. Without direct player control, the interactions between agents can create unexpected outcomes such as large outbreaks or naturally formed safe zones.

## Project Concepts
- **Zombies** pursue nearby humans and infect on contact
- **Humans** move, seek shelter, consume food, and become inactive when they starve
- **Military** patrol and can kill zombies
- **Medics** heal injured civilians nearby, and **traps** catch zombies that walk over them
- Behaviour emerges from local agent interactions
- **No player input.** Every agent follows simple local rules, so the outbreak curve and the formation of safe zones emerge from the interactions themselves

## Features
- World simulation
- Tick Loop
- Human movement and survival
- Zombie infection and chasing
- Military patrol and shooting
- Buildings and shelter
- Food/resources
- Population statistics
- Start, pause, reset, and speed controls
- Medics, traps, and a button to place extra traps
- A headless experiment that measures whether medics and traps help

## Class Structures

[View the UML diagram](https://drive.google.com/file/d/1-3FDkD7lQL_UJ8YTpuyqcfL1BkNXXclc/view?usp=sharing)

The generic design is demonstrated by `Human.findNearestEntity()`, which searches for the nearest entity of a requested subtype. The Food constructor uses the standard `double` type because it already accepts both integer and decimal literals.

## Team Responsibilities
Use the agreed division:

| Area | Responsibility | Member | Student ID |
|---|---|---|---|
| World/Main | Tick loop, spawning, and neighbour queries | Tran Khoi Nguyen (Kian) Nguyen | 48769266 |
| Human/Food | Energy, starvation, food consumption, and shelter | Abir Maya Bhowmick | 49125761 |
| Zombie | Infection, chasing, and target selection | Nicholas Fang | |
| Military/MovementBehaviour | Patrol, shooting, and strategy pattern | Neev Patel | 48521558 |
| Building/SimPanel | Occupancy, rendering, and statistics | Shahriar Hasan | |
| Medic/Trap | Healing civilians, traps that catch zombies, and trap events | Abyad Razin | 61216542 |

## Rules 
- Zombies chase nearby humans.
- Humans attempt to survive by moving, finding food, and seeking shelter. They lose energy on each update, gain energy by eating nearby food, and become inactive when their energy reaches zero.
- Zombies infect humans when they catch them.
- Military units patrol and shoot zombies.
- Medics heal the weakest visible civilian within 300 units (30 energy, then a 15-tick cooldown), but only while their own energy is above 35 and no zombie is within 100 units of them or the patient.
- Traps catch any zombie within 18 units. Each trap has 2 uses and breaks when they run out. Three start near building doors, and up to three more can be placed by hand.
- The simulation progresses automatically through repeated ticks.
- Population changes are caused by interactions between entities.

## Requirements
- Git
- Java Development Kit (JDK 17 or later; the code uses records)

### How to run
1. Clone this project and move to this project
```bash
git clone https://github.com/ShahriarHasan001533/Git-Wreckers-on-a-Zombie-mission

cd Git-Wreckers-on-a-Zombie-mission
cd COMP2000
```
2. Compile the Java source files.
```bash
javac -d out src/*.java
```

3. Run the `Main` class
```bash
java -cp "out;src" Main
```

On macOS or Linux, use a colon instead of a semicolon: `java -cp "out:src" Main`.
The `src` entry makes the PNG image assets available at runtime.

4. Check the Java version and confirm that the image folders are present.
```bash 
java -version
javac -version
```

## Medic and Trap (Abyad Razin)

| Part | What it does | Main ideas |
|---|---|---|
| `Medic` | A civilian who heals the most urgent visible civilian nearby and skips patients with a zombie close by | Template Method: `Human.update()` fixes the energy step and calls `behave()`, which `Medic` overrides, so medics still get hungry. Streams pick the patient. |
| `Trap` | Catches zombies that walk over it, with limited uses. Starting traps go at random spots in front of building doors, where zombies spend most of their time. | Streams, exceptions for invalid traps and when there's no free spot |
| Trap events | Every catch is announced to listeners: the HUD counter (`TrapReport`) and the blast effect (`TrapBlasts`) | Observer pattern (`TrapListener`) |
| `TrapPlacer` | The "Place trap" button: press it, then click the map to place up to 3 extra traps | Lambda callbacks |
| `Experiment` | Runs the simulation many times without a window to compare no medics or traps, traps only, medics only, and both | Parallel stream, custom `Collector` (`ExperimentSummary`), a lambda per scenario |
| `RaceConditionDemo` | Shows what goes wrong when parallel runs share one counter, against an atomic counter and one counter per run | Race conditions, thread confinement |

Run the tools after compiling (step 2), from the `COMP2000` folder:
```bash
java -cp "out:src" Experiment 200 1500    # runs per scenario, ticks per run
java -cp "out:src" RaceConditionDemo
```

Tests: `MedicTest`, `TrapPlacerTest`, `TrapEventsTest`, `TrapPlacementTest`, `ExperimentTest`, `RaceConditionDemoTest`. Compile them with `javac -d out src/*.java test/*.java`, then run one with `java -cp "out:src" MedicTest`.

My fork has the heatmap of where zombies spend their time and later medic changes: https://github.com/AbyadRazin/Git-Wreckers-on-a-Zombie-mission

## References
- COMP2000 course materials
- Image assets are the PNG files included in this repository under `COMP2000/src`, `COMP2000/src/HumanFoodAssets` and `COMP2000/src/MedicAssets` (the trap picture is `COMP2000/src/Trap.png`).
- The simulation loads these assets from the classpath; no external image source or attribution is recorded in the submitted project.
