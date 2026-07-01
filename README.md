# Elevator Simulator

An interactive, graphical elevator simulation built with Java using Swing. The simulation implements a target-based collective LOOK algorithm to schedule elevator sweeps, manage priority queues, and coordinate passenger boarding.

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
* **Select Destination**: Click a floor button on the pop-up panel to select the embarking passenger's destination floor.
* **FIFO and Sweep Priority**: The elevator prioritizes the oldest requests in the queue, skipping intermediate opposite-direction calls, and sweeps to pick up matching passengers along its path.
* **Arrived Passengers**: Once a passenger reaches their destination, they are marked as **Done** and removed.


## Example Usage

<p align="center">
   <img src="images/example.gif" width="60%" alt="Elevator Simulator Demo">
</p>

---

<p align="center">&copy; Copyright 2026 <a href="https://blakerayvid.com">Blake Rayvid</a>. All rights reserved.</p>