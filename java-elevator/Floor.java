import java.util.ArrayList;
import java.util.List;

public abstract class Floor {
    protected final int floorNumber;
    protected final Building building;
    protected final List<Rider> waiting = new ArrayList<>();

    public Floor(int floorNumber, Building building) {
        if (floorNumber < 0) {
            throw new IllegalArgumentException("'floorNumber' must be a non-negative integer.");
        }
        this.floorNumber = floorNumber;
        this.building = building;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public List<Rider> getWaiting() {
        return waiting;
    }

    public void addRiderWaiting(Rider rider) {
        if (!waiting.contains(rider)) {
            waiting.add(rider);
        }
    }

    public void removeRiderWaiting(Rider rider) {
        waiting.remove(rider);
    }

    public abstract void pressUpButton();
    public abstract void pressDownButton();
    public abstract boolean isUpButtonPressed();
    public abstract boolean isDownButtonPressed();
    public abstract void resetUpButton();
    public abstract void resetDownButton();
}