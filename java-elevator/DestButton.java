public class DestButton extends Button {
    private final int floorNumber;
    private final Elevator elevator;

    public DestButton(int floorNumber, Elevator elevator) {
        this.floorNumber = floorNumber;
        this.elevator = elevator;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public Elevator getElevator() {
        return elevator;
    }

    @Override
    public String toString() {
        return "DestButton(Floor: " + floorNumber + ", Elevator: " + (elevator != null ? elevator.getId() : "N/A") + ", Pressed: " + pressed + ")";
    }
}