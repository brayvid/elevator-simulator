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

### Installing Java (If Not Already Installed)

To verify if you have the JDK installed, open your terminal or command prompt and run:
```bash
java -version
javac -version
```
If either command is not recognized, you will need to install the JDK using one of the methods below:

#### **Windows**
* **Via Command Line**: Open PowerShell or Command Prompt as administrator and run:
  ```cmd
  winget install EclipseAdoptium.Temurin.17.JDK
  ```
* **Manual Installation**: Download and run the installer for JDK 17 or 21 from [Adoptium (Eclipse Temurin)](https://adoptium.net/). Ensure the option to **"Add to PATH"** is enabled during the setup process.

#### **macOS**
* **Via Homebrew**: Open Terminal and run:
  ```bash
  brew install openjdk
  ```
* **Manual Installation**: Download and run the macOS `.pkg` installer from [Adoptium (Eclipse Temurin)](https://adoptium.net/). Make sure to select the correct architecture for your system (Apple Silicon/M-series or Intel).

#### **Linux (Ubuntu/Debian)**
Open your terminal and run:
```bash
sudo apt update
sudo apt install default-jdk
```

---

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