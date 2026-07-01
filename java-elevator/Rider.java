public class Rider {
    private static int nextId = 1;

    private final int id;
    private final int weight;
    private int currentFloorNum;
    private int destFloorNum;
    private final Building building;
    private Elevator elevator;

    public Rider(Building building, int originFloorNum, int destFloorNum, int weight) {
        this.id = nextId++;
        this.weight = weight;
        this.currentFloorNum = originFloorNum;
        this.destFloorNum = destFloorNum;
        this.building = building;
        this.elevator = null;

        if (currentFloorNum < 0 || currentFloorNum >= building.getNumFloors() ||
            destFloorNum < 0 || destFloorNum >= building.getNumFloors()) {
            throw new IllegalArgumentException("Invalid origin or destination floor for Rider.");
        }

        building.getFloor(currentFloorNum).addRiderWaiting(this);
        building.addOccupant(this);
        System.out.println("Rider " + id + " created at " + getFloorName(currentFloorNum) + ", wants " + getFloorName(destFloorNum));
    }

    public int getId() { return id; }
    public int getWeight() { return weight; }
    public int getCurrentFloorNum() { return currentFloorNum; }
    public int getDestFloorNum() { return destFloorNum; }
    public void setDestFloorNum(int destFloorNum) { this.destFloorNum = destFloorNum; }
    public Elevator getElevator() { return elevator; }

    private String getFloorName(int floorNumber) {
        if (floorNumber == 0) {
            return "Lobby";
        } else if (floorNumber == building.getNumFloors() - 1) {
            return "Roof";
        } else {
            return "Floor " + (floorNumber + 1);
        }
    }

    public void requestElevator() {
        if (elevator != null) {
            System.out.println("Rider " + id + ": Already in elevator " + elevator.getId() + ", cannot request another.");
            return;
        }
        if (currentFloorNum == destFloorNum) {
            System.out.println("Rider " + id + ": Already at destination " + getFloorName(currentFloorNum) + ", no request needed.");
            return;
        }

        Floor floorObj = building.getFloor(currentFloorNum);
        if (destFloorNum > currentFloorNum) {
            System.out.println("Rider " + id + " at " + getFloorName(currentFloorNum) + " requests UP call for " + getFloorName(destFloorNum));
            floorObj.pressUpButton();
        } else if (destFloorNum < currentFloorNum) {
            System.out.println("Rider " + id + " at " + getFloorName(currentFloorNum) + " requests DOWN call for " + getFloorName(destFloorNum));
            floorObj.pressDownButton();
        }
    }

    public void enter(Elevator elevatorObj) {
        if (this.currentFloorNum != elevatorObj.getCurrentFloor()) {
            throw new IllegalStateException("Elevator is not at the rider's current floor.");
        }
        if (this.weight + elevatorObj.getCurrentWeight() > elevatorObj.getWeightLimit()) {
            throw new IllegalStateException("Not allowed - Elevator would be over weight limit.");
        }

        building.getFloor(currentFloorNum).removeRiderWaiting(this);
        elevatorObj.getOccupants().add(this);
        this.elevator = elevatorObj;
        elevatorObj.setCurrentWeight(elevatorObj.getCurrentWeight() + this.weight);
    }

    public void exitElev(Elevator elevatorObj) {
        Elevator targetElevator = elevatorObj != null ? elevatorObj : this.elevator;
        if (targetElevator == null) {
            throw new IllegalStateException("Rider is not in an elevator.");
        }

        if (!targetElevator.getOccupants().contains(this)) {
            System.out.println("Warning: Rider " + id + " was asked to exit elevator " + targetElevator.getId() + " but was not listed as an occupant.");
        } else {
            targetElevator.getOccupants().remove(this);
        }

        this.currentFloorNum = targetElevator.getCurrentFloor();
        
        // Only add them back to the floor's waiting list if they are NOT at their destination.
        if (this.currentFloorNum != this.destFloorNum) {
            building.getFloor(currentFloorNum).addRiderWaiting(this);
        }
        
        targetElevator.setCurrentWeight(targetElevator.getCurrentWeight() - this.weight);
        this.elevator = null;

        if (this.currentFloorNum == this.destFloorNum) {
            System.out.println("Rider " + id + " arrived at destination " + getFloorName(currentFloorNum) + ".");
        }
    }

    public void leaveBuilding() {
        if (currentFloorNum == 0 && elevator == null) {
            building.getFloor(0).removeRiderWaiting(this);
            building.removeOccupant(this);
            System.out.println("Rider " + id + " has left the building from the Lobby.");
        } else {
            String reason = currentFloorNum != 0 ? "not in lobby" : "in an elevator";
            System.out.println("Rider " + id + " cannot leave building: " + reason + ".");
        }
    }

    public void setNewDestination(int newDestFloorNum) {
        if (elevator != null) {
            System.out.println("Rider " + id + ": Cannot change destination while in elevator.");
            return;
        }
        if (newDestFloorNum < 0 || newDestFloorNum >= building.getNumFloors()) {
            throw new IllegalArgumentException("Invalid new destination floor.");
        }
        System.out.println("Rider " + id + " at " + getFloorName(currentFloorNum) + " now wants to go to " + getFloorName(newDestFloorNum));
        this.destFloorNum = newDestFloorNum;
        
        // Explicitly put them back into the waiting queue for their new trip
        building.getFloor(currentFloorNum).addRiderWaiting(this);
        this.requestElevator();
    }

    @Override
    public String toString() {
        return "Rider " + id + " (W:" + weight + ", At:" + getFloorName(currentFloorNum) + ", Dest:" + getFloorName(destFloorNum) + ", InElev:" + (elevator != null ? elevator.getId() : "None") + ")";
    }
}