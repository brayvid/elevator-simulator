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
    
    // Physical movement direction (Up, Down, Stopped)
    private Direction direction = Direction.IDLE;

    private final Set<Integer> destinationRequests = new HashSet<>();
    
    // LinkedHashSet is CRITICAL: it strictly preserves the exact chronological order buttons were pressed
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
    public boolean isDoorsOpen() { return doorsOpen; }
    public Set<Integer> getDestinationRequests() { return destinationRequests; }
    public Set<FloorRequest> getPickupRequests() { return pickupRequests; }

    // Determines the active sweep group based on the OLDEST request in the queue
    public Direction getPrimaryGroup() {
        if (!pickupRequests.isEmpty()) {
            return pickupRequests.iterator().next().getDirection();
        }
        return Direction.IDLE;
    }

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

    private Integer getNextTargetFloor() {
        if (destinationRequests.isEmpty() && pickupRequests.isEmpty()) {
            return null;
        }

        // 1. If we have passengers, prioritize dropping them off in our current travel path
        if (!destinationRequests.isEmpty()) {
            if (direction == Direction.UP) {
                int min = Integer.MAX_VALUE;
                for (int d : destinationRequests) if (d > currentFloor && d < min) min = d;
                if (min != Integer.MAX_VALUE) return min; // Closest stop above us
                
                int max = -1;
                for (int d : destinationRequests) if (d < currentFloor && d > max) max = d;
                if (max != -1) return max; // Turnaround to highest stop below us
            } else if (direction == Direction.DOWN) {
                int max = -1;
                for (int d : destinationRequests) if (d < currentFloor && d > max) max = d;
                if (max != -1) return max; // Closest stop below us
                
                int min = Integer.MAX_VALUE;
                for (int d : destinationRequests) if (d > currentFloor && d < min) min = d;
                if (min != Integer.MAX_VALUE) return min; // Turnaround to lowest stop above us
            }
            return destinationRequests.iterator().next(); // Fallback
        }

        // 2. If empty, strictly establish the sweep based on the primary queue group
        Direction primary = getPrimaryGroup();
        
        List<Integer> groupFloors = new ArrayList<>();
        for (FloorRequest req : pickupRequests) {
            if (req.getDirection() == primary) {
                groupFloors.add(req.getFloorNumber());
            }
        }

        if (primary == Direction.DOWN) {
            return Collections.max(groupFloors); // Go directly to the highest DOWN request (Apex)
        } else if (primary == Direction.UP) {
            return Collections.min(groupFloors); // Go directly to the lowest UP request (Nadir)
        }

        return pickupRequests.iterator().next().getFloorNumber();
    }

    // Helper centralizing the logic for "Who is allowed to get on, and when?"
    private boolean isDirectionBeingServiced(Direction dirToCheck) {
        if (!occupants.isEmpty()) {
            // Opportunistic Mode: If carrying people, pick up ANYONE going the exact same direction
            return dirToCheck == direction;
        } else {
            // Strict Sweep Mode: We are empty and fetching a primary queue group
            Direction primaryGroup = getPrimaryGroup();
            if (primaryGroup == Direction.IDLE) return true;
            
            Integer target = getNextTargetFloor();
            boolean isTurnaround = (target != null && target == currentFloor);
            
            if (isTurnaround) {
                // Arrived at the apex/nadir. We can now load passengers for the sweep down/up!
                return dirToCheck == primaryGroup;
            } else {
                // Still traveling to the apex/nadir. ONLY pick up people who match our sweep AND physical movement.
                return dirToCheck == primaryGroup && dirToCheck == direction;
            }
        }
    }

    private boolean shouldStopAtCurrentFloor() {
        // ALWAYS stop if someone inside wants to get off
        if (destinationRequests.contains(currentFloor)) return true;

        if (pickupRequests.isEmpty()) return false;

        Integer target = getNextTargetFloor();
        // ALWAYS stop if we have reached our designated turnaround target
        if (target != null && target == currentFloor) return true;

        // Check if any requests on this floor align with our active service logic
        for (FloorRequest req : pickupRequests) {
            if (req.getFloorNumber() == currentFloor) {
                if (isDirectionBeingServiced(req.getDirection())) {
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

        // Base valid buttons purely on what direction the rider is intending to travel
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
        
        String headerText = "Rider " + riderId + " enters Elevator " + id + " at " + getFloorName(currentFloor, numFloors);
        JLabel titleLabel = new JLabel(headerText, JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel promptLabel = new JLabel("Please select destination floor on the panel:", JLabel.CENTER);
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
        dialog.setSize(320, Math.min(650, 120 + (rows * 50)));
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

                boolean canBoard = isDirectionBeingServiced(riderReqDirection);

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
            if (pr.getFloorNumber() == currentFloor && isDirectionBeingServiced(pr.getDirection())) {
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

        if (shouldStopAtCurrentFloor()) {
            if (!doorsOpen) {
                doorsOpen = true;
                doorTimer = 0;
                System.out.println("Elevator " + id + ": Doors opening at " + getFloorName(currentFloor, building.getNumFloors()));
                handleAlighting();
            }
            return;
        }

        Integer nextTarget = getNextTargetFloor();

        if (nextTarget != null) {
            if (nextTarget > currentFloor) {
                direction = Direction.UP;
                currentFloor++;
                System.out.println("Elevator " + id + ": Moving UP to " + getFloorName(currentFloor, building.getNumFloors()));
            } else if (nextTarget < currentFloor) {
                direction = Direction.DOWN;
                currentFloor--;
                System.out.println("Elevator " + id + ": Moving DOWN to " + getFloorName(currentFloor, building.getNumFloors()));
            }
        } else {
            if (direction != Direction.IDLE) {
                System.out.println("Elevator " + id + ": No more requests. Becoming IDLE at " + getFloorName(currentFloor, building.getNumFloors()) + ".");
            }
            direction = Direction.IDLE;
        }
    }

    @Override
    public String toString() {
        List<Integer> sortedDests = new ArrayList<>(destinationRequests);
        Collections.sort(sortedDests);
        List<FloorRequest> sortedPickups = new ArrayList<>(pickupRequests);
        sortedPickups.sort(Comparator.comparingInt(FloorRequest::getFloorNumber));

        return "Elevator " + id + " (F" + currentFloor + ", Dir:" + direction + ", Grp:" + getPrimaryGroup() + ", " +
                "Weight:" + currentWeight + "/" + weightLimit + ", " +
                "Occ:" + occupants.size() + ", Doors:" + (doorsOpen ? "Open" : "Closed") + ", " +
                "Dests:" + sortedDests + ", Pickups:" + sortedPickups + ")";
    }
}