import java.util.*;

public class Building {
    private final int numFloors;
    private final int numElevators;
    private final List<Rider> occupants = new ArrayList<>();
    private final List<Floor> floors = new ArrayList<>();
    private final List<Elevator> elevators = new ArrayList<>();
    private final Set<FloorRequest> floorRequests = new HashSet<>();

    public Building(int numFloors, int numElevators) {
        if (numFloors < 2) {
            throw new IllegalArgumentException("Building must have at least 2 floors.");
        }
        if (numElevators < 1) {
            throw new IllegalArgumentException("Building must have at least 1 elevator.");
        }

        this.numFloors = numFloors;
        this.numElevators = numElevators;

        floors.add(new Lobby(0, this));
        for (int i = 1; i < numFloors - 1; i++) {
            floors.add(new MiddleFloor(i, this));
        }
        floors.add(new Roof(numFloors - 1, this));

        for (int i = 0; i < numElevators; i++) {
            elevators.add(new Elevator(this, i + 1, 2000));
        }
    }

    public int getNumFloors() { return numFloors; }
    public int getNumElevators() { return numElevators; }
    public List<Rider> getOccupants() { return occupants; }
    public Floor getFloor(int index) { return floors.get(index); }
    public List<Elevator> getElevators() { return elevators; }
    public Set<FloorRequest> getFloorRequests() { return floorRequests; }

    private String getFloorName(int floorNumber) {
        if (floorNumber == 0) {
            return "Lobby";
        } else if (floorNumber == numFloors - 1) {
            return "Roof";
        } else {
            return "Floor " + (floorNumber + 1);
        }
    }

    public void addOccupant(Rider rider) {
        if (!occupants.contains(rider)) {
            occupants.add(rider);
        }
    }

    public void removeOccupant(Rider rider) {
        occupants.remove(rider);
    }

    public void addFloorRequest(int floorNumber, Direction direction) {
        FloorRequest request = new FloorRequest(floorNumber, direction);
        if (!floorRequests.contains(request)) {
            floorRequests.add(request);
            System.out.println("Building: Received floor request at " + getFloorName(floorNumber) + " for " + direction + ".");
            dispatchRequest(request);
        }
    }

    public void clearFloorRequest(int floorNumber, Direction direction) {
        FloorRequest request = new FloorRequest(floorNumber, direction);
        if (floorRequests.contains(request)) {
            floorRequests.remove(request);
            System.out.println("Building: Cleared floor request at " + getFloorName(floorNumber) + " for " + direction + ".");
            Floor floorObj = floors.get(floorNumber);
            if (direction == Direction.UP) {
                floorObj.resetUpButton();
            } else if (direction == Direction.DOWN) {
                floorObj.resetDownButton();
            }
        }
    }

    private void dispatchRequest(FloorRequest request) {
        int reqFloor = request.getFloorNumber();
        Direction reqDir = request.getDirection();

        Elevator bestElevator = null;
        int minScore = Integer.MAX_VALUE;

        for (Elevator elev : elevators) {
            int dist = Math.abs(elev.getCurrentFloor() - reqFloor);
            int score;

            if (elev.getPrimaryGroup() == Direction.IDLE) {
                score = dist; // IDLE elevators are great candidates
            } else if (elev.getPrimaryGroup() == reqDir) {
                score = dist + 5; // Matches the elevator's current active queue group
            } else {
                score = dist + 30; // Opposing queue (will do this after current jobs finish)
            }

            if (score < minScore) {
                minScore = score;
                bestElevator = elev;
            }
        }

        if (bestElevator != null) {
            System.out.println("Building: Dispatching request " + request + " to Elevator " + bestElevator.getId());
            bestElevator.assignPickupRequest(reqFloor, reqDir);
        } else {
            System.out.println("Building: No suitable elevator found immediately for " + request + ".");
        }
    }

    public void tick() {
        System.out.println("\n--- Tick ---");

        for (Elevator elevator : elevators) {
            elevator.update();
        }

        for (FloorRequest req : new ArrayList<>(floorRequests)) {
            boolean isHandled = false;
            for (Elevator elev : elevators) {
                if (elev.getPickupRequests().contains(req)) {
                    isHandled = true;
                    break;
                }
            }
            if (!isHandled) {
                dispatchRequest(req);
            }
        }

        statusReport();
    }

    public void statusReport() {
        System.out.println("Building Status:");
        System.out.println("  Floor Requests: " + floorRequests);
        for (Floor floor : floors) {
            List<Integer> waitingRidersIds = new ArrayList<>();
            for (Rider r : floor.getWaiting()) {
                waitingRidersIds.add(r.getId());
            }
            String upPressed = floor.isUpButtonPressed() ? " (UP)" : "";
            String downPressed = floor.isDownButtonPressed() ? " (DOWN)" : "";
            System.out.println("  " + getFloorName(floor.getFloorNumber()) + ": Waiting: " + waitingRidersIds + upPressed + downPressed);
        }
        for (Elevator elevator : elevators) {
            System.out.println("  " + elevator);
        }
        List<Integer> occupantIds = new ArrayList<>();
        for (Rider r : occupants) {
            occupantIds.add(r.getId());
        }
        System.out.println("  Occupants in building: " + occupantIds);
        System.out.println("--- End Tick ---");
    }
}