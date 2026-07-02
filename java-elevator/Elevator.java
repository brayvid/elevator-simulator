import java.util.*;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.GraphicsEnvironment;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Elevator {
    public static final int DOOR_OPEN_DURATION = 2;

    private final int id;
    private final int weightLimit;
    private final Building building;
    private int currentFloor = 0;
    private int currentWeight = 0;
    private final List<Rider> occupants = new ArrayList<>();
    
    // Physical and logical direction of travel
    private Direction direction = Direction.IDLE;

    private final Set<Integer> destinationRequests = new HashSet<>();
    private final Set<FloorRequest> pickupRequests = new LinkedHashSet<>();
    private final List<DestButton> buttons = new ArrayList<>();

    private boolean doorsOpen = false;
    private int doorTimer = 0;

    public Elevator(Building building, int elevatorId, int weightLimit) {
        this.id = elevatorId;
        this.weightLimit = weightLimit;
        this.building = building;

        for (int i = 0; i < building.getNumFloors(); i++) {
            buttons.add(new DestButton(i, this));
        }
    }

    public int getId() { return id; }
    public int getWeightLimit() { return weightLimit; }
    public int getCurrentFloor() { return currentFloor; }
    public int getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(int currentWeight) { this.currentWeight = currentWeight; }
    public List<Rider> getOccupants() { return occupants; }
    public Direction getDirection() { return direction; }
    
    // Map the dispatcher group helper directly to physical direction for Building.java
    public Direction getPrimaryGroup() { return direction; }
    public boolean isDoorsOpen() { return doorsOpen; }
    public Set<Integer> getDestinationRequests() { return destinationRequests; }
    public Set<FloorRequest> getPickupRequests() { return pickupRequests; }

    public void pressDestinationButton(int floorNumber) {
        if (floorNumber >= 0 && floorNumber < building.getNumFloors()) {
            buttons.get(floorNumber).push();
            if (floorNumber != currentFloor) {
                destinationRequests.add(floorNumber);
                System.out.println("Elevator " + id + ": Rider pressed button for " + getFloorName(floorNumber, building.getNumFloors()));
            }
        }
    }

    public void assignPickupRequest(int floorNumber, Direction directionStr) {
        pickupRequests.add(new FloorRequest(floorNumber, directionStr));
        System.out.println("Elevator " + id + ": Assigned pickup at " + getFloorName(floorNumber, building.getNumFloors()) + " going " + directionStr);
    }

    private boolean hasDestinationsAbove() {
        for (int f : destinationRequests) if (f > currentFloor) return true;
        return false;
    }

    private boolean hasDestinationsBelow() {
        for (int f : destinationRequests) if (f < currentFloor) return true;
        return false;
    }

    private Integer getNextTargetFloor() {
        if (destinationRequests.isEmpty() && pickupRequests.isEmpty()) {
            return null;
        }

        if (direction == Direction.UP) {
            // Valid stops above us are:
            // 1. Destinations above us
            // 2. UP pickups above us
            // 3. Turnaround apex: highest of any DOWN pickups (only if we have no more drop-offs above us)
            List<Integer> validStopsAbove = new ArrayList<>();
            for (int f : destinationRequests) {
                if (f >= currentFloor) validStopsAbove.add(f);
            }
            for (FloorRequest r : pickupRequests) {
                if (r.getFloorNumber() >= currentFloor && r.getDirection() == Direction.UP) {
                    validStopsAbove.add(r.getFloorNumber());
                }
            }
            
            // Turnaround check: Only look for an apex if we have no passengers going higher
            if (!hasDestinationsAbove()) {
                int highestDown = -1;
                for (FloorRequest r : pickupRequests) {
                    if (r.getDirection() == Direction.DOWN && r.getFloorNumber() > highestDown) {
                        highestDown = r.getFloorNumber();
                    }
                }
                if (highestDown >= currentFloor) {
                    // Apex is only valid if there are no co-directional UP requests above it
                    boolean hasUpRequestAboveHighestDown = false;
                    for (FloorRequest r : pickupRequests) {
                        if (r.getDirection() == Direction.UP && r.getFloorNumber() > highestDown) {
                            hasUpRequestAboveHighestDown = true;
                            break;
                        }
                    }
                    if (!hasUpRequestAboveHighestDown) {
                        validStopsAbove.add(highestDown);
                    }
                }
            }

            if (!validStopsAbove.isEmpty()) {
                return Collections.min(validStopsAbove); // Closest valid stop above
            }
            
            // If absolutely nothing is above us, find the lowest overall request below to turnaround
            Set<Integer> allStops = getAllStops();
            if (!allStops.isEmpty()) {
                return Collections.min(allStops);
            }
            
        } else if (direction == Direction.DOWN) {
            // Valid stops below us are:
            // 1. Destinations below us
            // 2. DOWN pickups below us
            // 3. Turnaround nadir: lowest of any UP pickups (only if we have no more drop-offs below us)
            List<Integer> validStopsBelow = new ArrayList<>();
            for (int f : destinationRequests) {
                if (f <= currentFloor) validStopsBelow.add(f);
            }
            for (FloorRequest r : pickupRequests) {
                if (r.getFloorNumber() <= currentFloor && r.getDirection() == Direction.DOWN) {
                    validStopsBelow.add(r.getFloorNumber());
                }
            }

            // Turnaround check: Only look for a nadir if we have no passengers going lower
            if (!hasDestinationsBelow()) {
                int lowestUp = building.getNumFloors() + 1;
                for (FloorRequest r : pickupRequests) {
                    if (r.getDirection() == Direction.UP && r.getFloorNumber() < lowestUp) {
                        lowestUp = r.getFloorNumber();
                    }
                }
                if (lowestUp <= currentFloor) {
                    // Nadir is only valid if there are no co-directional DOWN requests below it
                    boolean hasDownRequestBelowLowestUp = false;
                    for (FloorRequest r : pickupRequests) {
                        if (r.getDirection() == Direction.DOWN && r.getFloorNumber() < lowestUp) {
                            hasDownRequestBelowLowestUp = true;
                            break;
                        }
                    }
                    if (!hasDownRequestBelowLowestUp) {
                        validStopsBelow.add(lowestUp);
                    }
                }
            }

            if (!validStopsBelow.isEmpty()) {
                return Collections.max(validStopsBelow); // Closest valid stop below
            }

            // If absolutely nothing is below us, find the highest overall request above to turnaround
            Set<Integer> allStops = getAllStops();
            if (!allStops.isEmpty()) {
                return Collections.max(allStops);
            }
            
        } else { // IDLE: Target closest overall request to establish physical direction
            Set<Integer> allStops = getAllStops();
            if (allStops.isEmpty()) return null;

            int closestFloor = -1;
            int minDist = building.getNumFloors() + 1;
            for (int floor : allStops) {
                int dist = Math.abs(floor - currentFloor);
                if (dist < minDist) {
                    minDist = dist;
                    closestFloor = floor;
                } else if (dist == minDist && floor == currentFloor) {
                    closestFloor = floor;
                }
            }
            return closestFloor != -1 ? closestFloor : null;
        }
        return null;
    }

    private Set<Integer> getAllStops() {
        Set<Integer> allStops = new HashSet<>(destinationRequests);
        for (FloorRequest r : pickupRequests) {
            allStops.add(r.getFloorNumber());
        }
        return allStops;
    }

    private boolean hasRequestsAbove() {
        for (int f : destinationRequests) if (f > currentFloor) return true;
        for (FloorRequest r : pickupRequests) if (r.getFloorNumber() > currentFloor) return true;
        return false;
    }

    private boolean hasRequestsBelow() {
        for (int f : destinationRequests) if (f < currentFloor) return true;
        for (FloorRequest r : pickupRequests) if (r.getFloorNumber() < currentFloor) return true;
        return false;
    }

    private boolean canRiderBoard(Direction riderReqDir) {
        if (direction == Direction.IDLE) {
            return true; // Idle cabin accepts any initial passenger direction
        }
        if (direction == Direction.UP) {
            if (riderReqDir == Direction.UP) return true;
            // Only allow DOWN passengers to board if we are at the very top of our run (no more tasks above)
            if (riderReqDir == Direction.DOWN) {
                return !hasRequestsAbove();
            }
        }
        if (direction == Direction.DOWN) {
            if (riderReqDir == Direction.DOWN) return true;
            // Only allow UP passengers to board if we are at the very bottom of our run (no more tasks below)
            if (riderReqDir == Direction.UP) {
                return !hasRequestsBelow();
            }
        }
        return false;
    }

    private boolean shouldStopAtCurrentFloor() {
        // ALWAYS stop for alighting passengers
        if (destinationRequests.contains(currentFloor)) return true;

        if (pickupRequests.isEmpty()) return false;

        Integer target = getNextTargetFloor();
        // ALWAYS stop if we have reached our designated turnaround target
        if (target != null && target == currentFloor) return true;

        // Stop for any matching co-directional pickup requests
        for (FloorRequest req : pickupRequests) {
            if (req.getFloorNumber() == currentFloor) {
                if (req.getDirection() == direction) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getFloorName(int floorNumber, int numFloors) {
        if (floorNumber == 0) return "Lobby";
        else if (floorNumber == numFloors - 1) return "Roof";
        else return "Floor " + (floorNumber + 1);
    }

    private int promptForDestination(int riderId, int defaultDest, Direction riderIntendedDirection) {
        if (GraphicsEnvironment.isHeadless()) return defaultDest;

        int numFloors = building.getNumFloors();
        List<Integer> validFloors = new ArrayList<>();

        // Base valid buttons purely on what direction the rider intends to travel
        if (riderIntendedDirection == Direction.UP) {
            for (int f = currentFloor + 1; f < numFloors; f++) validFloors.add(f);
        } else if (riderIntendedDirection == Direction.DOWN) {
            for (int f = 0; f < currentFloor; f++) validFloors.add(f);
        } else {
            for (int f = 0; f < numFloors; f++) {
                if (f != currentFloor) validFloors.add(f);
            }
        }

        if (validFloors.isEmpty()) return defaultDest;

        final int[] selectedFloor = {defaultDest};
        JDialog dialog = new JDialog((Frame) null, "Elevator " + id + " Control Panel", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        
        // Dynamically append " going Up" or " going Down" to the header label
        String dirSuffix = (riderIntendedDirection == Direction.UP) ? "Up" : "Down";
        String headerText = "Rider " + riderId + " enters Elevator " + id + " at " + getFloorName(currentFloor, numFloors) + " going " + dirSuffix;
        
        JLabel titleLabel = new JLabel(headerText, JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        
        // Updated label text as specified
        JLabel promptLabel = new JLabel("Please select their destination floor below:", JLabel.CENTER);
        headerPanel.add(titleLabel);
        headerPanel.add(promptLabel);
        dialog.add(headerPanel, BorderLayout.NORTH);

        int columns = 2;
        int rows = (int) Math.ceil((double) numFloors / columns);
        JPanel gridPanel = new JPanel(new GridLayout(rows, columns, 10, 10));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        for (int f = numFloors - 1; f >= 0; f--) {
            final int floorIndex = f;
            String label = getFloorName(floorIndex, numFloors);
            JButton btn = new JButton(label);
            btn.setFont(new Font("SansSerif", Font.BOLD, 12));
            btn.setFocusPainted(false);
            
            if (f == currentFloor) {
                btn.setText(label + " (Here)");
                btn.setEnabled(false);
            } else if (validFloors.contains(f)) {
                btn.setEnabled(true);
                btn.addActionListener(e -> {
                    selectedFloor[0] = floorIndex;
                    dialog.dispose();
                });
            } else {
                btn.setEnabled(false); 
            }
            gridPanel.add(btn);
        }

        dialog.add(gridPanel, BorderLayout.CENTER);
        dialog.pack();
        
        // Increased dialog width to 380 to prevent cutting off text
        dialog.setSize(380, Math.min(650, 120 + (rows * 50)));
        dialog.setLocationRelativeTo(null); 
        dialog.setVisible(true);

        return selectedFloor[0];
    }

    private void handleAlighting() {
        for (Rider rider : new ArrayList<>(occupants)) {
            if (rider.getDestFloorNum() == currentFloor) {
                rider.exitElev(this);
                buttons.get(currentFloor).reset();
                System.out.println("Elevator " + id + ": Rider " + rider.getId() + " exited at " + getFloorName(currentFloor, building.getNumFloors()));
            }
        }
        destinationRequests.remove(currentFloor);
    }

    private void handleBoarding() {
        Floor currentFloorObj = building.getFloor(currentFloor);
        
        for (Rider rider : new ArrayList<>(currentFloorObj.getWaiting())) {
            if (currentWeight + rider.getWeight() <= weightLimit) {
                
                boolean riderWantsUp = rider.getDestFloorNum() > currentFloor;
                Direction riderReqDirection = riderWantsUp ? Direction.UP : Direction.DOWN;

                // Explicitly check state-based boarding criteria to prevent wrong-way entering
                boolean canBoard = canRiderBoard(riderReqDirection);

                FloorRequest pickupToClear = null;
                if (canBoard) {
                    for (FloorRequest pr : pickupRequests) {
                        if (pr.getFloorNumber() == currentFloor && pr.getDirection() == riderReqDirection) {
                            pickupToClear = pr;
                            break;
                        }
                    }
                }

                if (canBoard) {
                    int selectedDest = promptForDestination(rider.getId(), rider.getDestFloorNum(), riderReqDirection);
                    rider.setDestFloorNum(selectedDest);

                    rider.enter(this);
                    System.out.println("Elevator " + id + ": Rider " + rider.getId() + " entered at " + getFloorName(currentFloor, building.getNumFloors()) + " for " + getFloorName(rider.getDestFloorNum(), building.getNumFloors()));
                    pressDestinationButton(rider.getDestFloorNum());

                    if (pickupToClear != null && pickupRequests.contains(pickupToClear)) {
                        pickupRequests.remove(pickupToClear);
                        building.clearFloorRequest(pickupToClear.getFloorNumber(), pickupToClear.getDirection());
                    }
                }
            }
        }

        // Clean up visual floor buttons
        List<FloorRequest> pickupsAtThisFloor = new ArrayList<>();
        for (FloorRequest pr : pickupRequests) {
            if (pr.getFloorNumber() == currentFloor && canRiderBoard(pr.getDirection())) {
                pickupsAtThisFloor.add(pr);
            }
        }

        for (FloorRequest pr : pickupsAtThisFloor) {
            boolean ridersStillWaitingForThisPickup = false;
            for (Rider waitingRider : currentFloorObj.getWaiting()) {
                boolean waitingRiderWantsUp = waitingRider.getDestFloorNum() > currentFloor;
                Direction waitDir = waitingRiderWantsUp ? Direction.UP : Direction.DOWN;
                if (waitDir == pr.getDirection()) {
                    ridersStillWaitingForThisPickup = true;
                    break;
                }
            }

            if (!ridersStillWaitingForThisPickup) {
                if (pickupRequests.contains(pr)) {
                    pickupRequests.remove(pr);
                    building.clearFloorRequest(pr.getFloorNumber(), pr.getDirection());
                    System.out.println("Elevator " + id + ": Cleared pickup request " + pr + " as no more relevant riders are waiting.");
                }
            }
        }
    }

    public void update() {
        if (doorsOpen) {
            doorTimer++;
            if (doorTimer >= DOOR_OPEN_DURATION) {
                doorsOpen = false;
                doorTimer = 0;
                System.out.println("Elevator " + id + ": Doors closing at " + getFloorName(currentFloor, building.getNumFloors()));
            } else {
                handleBoarding();
                return;
            }
        }

        // Pre-evaluate direction based on next target before evaluating stop
        Integer nextTarget = getNextTargetFloor();
        if (nextTarget != null) {
            if (nextTarget > currentFloor) direction = Direction.UP;
            else if (nextTarget < currentFloor) direction = Direction.DOWN;
        } else {
            direction = Direction.IDLE;
        }

        if (shouldStopAtCurrentFloor()) {
            if (!doorsOpen) {
                doorsOpen = true;
                doorTimer = 0;
                System.out.println("Elevator " + id + ": Doors opening at " + getFloorName(currentFloor, building.getNumFloors()));
                handleAlighting();
            }
            return;
        }

        if (direction == Direction.UP) {
            currentFloor++;
            System.out.println("Elevator " + id + ": Moving UP to " + getFloorName(currentFloor, building.getNumFloors()));
        } else if (direction == Direction.DOWN) {
            currentFloor--;
            System.out.println("Elevator " + id + ": Moving DOWN to " + getFloorName(currentFloor, building.getNumFloors()));
        }
    }

    @Override
    public String toString() {
        List<Integer> sortedDests = new ArrayList<>(destinationRequests);
        Collections.sort(sortedDests);
        List<FloorRequest> sortedPickups = new ArrayList<>(pickupRequests);
        sortedPickups.sort(Comparator.comparingInt(FloorRequest::getFloorNumber));

        return "Elevator " + id + " (F" + currentFloor + ", Dir:" + direction + ", " +
                "Weight:" + currentWeight + "/" + weightLimit + ", " +
                "Occ:" + occupants.size() + ", Doors:" + (doorsOpen ? "Open" : "Closed") + ", " +
                "Dests:" + sortedDests + ", Pickups:" + sortedPickups + ")";
    }
}