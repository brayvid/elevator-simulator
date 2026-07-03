# Elevator Simulator

An interactive graphical elevator simulation written in Java with a Swing GUI. The simulation implements the LOOK algorithm to schedule elevator sweeps, manage travel direction, and coordinate passenger boarding and drop-offs.

## Directory Structure

Ensure your files are placed in a subdirectory named `java-elevator`:

```text
java-elevator/
├── Building.java
├── Button.java
├── DestButton.java
├── Direction.java
├── Elevator.java
├── Floor.java
├── FloorRequest.java
├── Lobby.java
├── MiddleFloor.java
├── Rider.java
├── Roof.java
├── UpDownButton.java
└── Visualizer.java
```

## Prerequisites

* **Java Development Kit (JDK)**: Version 8 or higher is required to compile and run the application.

## How to Run

Follow these steps in your terminal or command prompt:

1. **Navigate to the project subdirectory**:
   ```bash
   cd path/to/java-elevator
   ```

2. **Compile all source files**:
   ```bash
   javac *.java
   ```

3. **Run the visualizer**:
   ```bash
   java Visualizer
   ```

## Using the Simulator

* **Call an Elevator**: Click the **▲ (UP)** or **▼ (DOWN)** buttons on the left side of any floor line. This spawns a passenger and calls the elevator to that floor.
* **Select Destination**: When a passenger boards, an interactive control panel pop-up will appear. Click a floor button on the panel to select their destination floor.
* **LOOK Algorithm Sweeping**: The elevator travels continuously in its active direction (UP or DOWN), dropping off occupants and picking up waiting passengers traveling in that same direction. It only reverses course when no requests remain ahead of it.
* **Arrived Passengers**: Once a passenger reaches their destination, they are marked **Done** and removed.


## Example Usage

<p align="center">
   <img src="images/example.gif" width="60%" alt="Elevator Simulator Demo">
</p>

---

<p align="center">&copy; Copyright 2026 <a href="https://blakerayvid.com">Blake Rayvid</a>. All rights reserved.</p>