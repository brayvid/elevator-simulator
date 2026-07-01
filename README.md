# Elevator Simulator

An interactive, graphical elevator simulation built with Java using Swing. The simulation implements a target-based collective LOOK algorithm to schedule elevator sweeps, manage priority queues, and coordinate passenger boarding and alighting.

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

2. **Compile the source files**:
   Compile all Java source files in the directory:
   ```bash
   javac *.java
   ```

3. **Run the visualizer**:
   Start the graphical interface:
   ```bash
   java Visualizer
   ```

## Using the Simulator

* **Call an Elevator**: Click the **▲ (UP)** or **▼ (DOWN)** buttons on the left side of any floor line. This spawns a passenger and calls the elevator to that floor.
* **Select Destination**: When the elevator doors open (indicated by the elevator box turning light green) and a passenger boards, an interactive control panel pop-up will appear. Click a button on the panel to select that passenger's destination floor.
* **FIFO and Sweep Priority**: The elevator prioritizes the oldest requests in the queue, skipping intermediate opposite-direction calls, and then sweeps to pick up matching passengers along its physical path.
* **Arrived Passengers**: Once a passenger reaches their destination, they are briefly marked as **Done** for 2 ticks before exiting the building.


## Example Usage

<img src="images/example.gif" width="50%" alt="Elevator Simulator Demo">