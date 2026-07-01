import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Visualizer extends JFrame {
    private final Building building;
    private final SimulationPanel panel;
    private final Timer timer;

    public Visualizer() {
        // DEFAULT CONFIGURATION: 10 floors, 1 elevator
        building = new Building(10, 1);

        setTitle("Elevator Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new SimulationPanel(building);
        add(panel, BorderLayout.CENTER);
        
        // pack() tells the JFrame to size itself exactly to the SimulationPanel's preferred size.
        pack(); 
        setLocationRelativeTo(null);

        // Tick interval: 1000 milliseconds (1.0 second)
        timer = new Timer(1000, e -> {
            // 1. Process fading of arrived occupants before building ticks
            List<Rider> occupants = new ArrayList<>(building.getOccupants());
            for (Rider r : occupants) {
                if (r.getElevator() == null && r.getCurrentFloorNum() == r.getDestFloorNum()) {
                    // Mark as arrived and register in the GUI fade map if not already present
                    if (!panel.getFadeCounters().containsKey(r)) {
                        panel.getFadeCounters().put(r, 2); // Exactly 2 ticks of visibility before disappearing
                    }
                }
            }

            // Decrement active fade counters
            List<Rider> toRemove = new ArrayList<>();
            for (Rider r : new ArrayList<>(panel.getFadeCounters().keySet())) {
                int ticksLeft = panel.getFadeCounters().get(r) - 1;
                if (ticksLeft <= 0) {
                    toRemove.add(r);
                } else {
                    panel.getFadeCounters().put(r, ticksLeft);
                }
            }

            // Permanently remove riders who completed their visual duration
            for (Rider r : toRemove) {
                panel.getFadeCounters().remove(r);
                building.removeOccupant(r);
                building.getFloor(r.getCurrentFloorNum()).removeRiderWaiting(r);
                System.out.println("Rider " + r.getId() + " left the building.");
            }

            // 2. Call the core physics logic tick
            building.tick();
            panel.repaint();
        });
        timer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Visualizer gui = new Visualizer();
            gui.setVisible(true);
        });
    }
}

class SimulationPanel extends JPanel {
    private final Building building;
    
    // Tracks current visibility state of arrived riders
    private final Map<Rider, Integer> fadeCounters = new HashMap<>();

    // Layout geometry constants
    private final int marginY = 50;
    private final int buttonWidth = 22;
    private final int buttonHeight = 20;
    private final int buttonXUp = 120; 
    private final int buttonXDown = 150; 
    private final int queueRightBound = 420; 
    
    // Elevator Shaft Geometry Constants
    private final int shaftXStart = 450;
    private final int shaftSpacing = 120;
    private final int shaftWidth = 70;
    private final int rightMargin = 50; // Distance from the right tip of floor lines to the window edge

    public SimulationPanel(Building building) {
        this.building = building;
        setBackground(new Color(245, 245, 245));

        // Dynamically calculate EXACT canvas width needed so the rightmost elevator is flush
        int numElevators = building.getNumElevators();
        int desiredWidth = shaftXStart + ((numElevators - 1) * shaftSpacing) + shaftWidth + rightMargin;
        setPreferredSize(new Dimension(desiredWidth, 750));

        // Add mouse listener to detect clicks on the UP/DOWN floor buttons
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });
    }

    public Map<Rider, Integer> getFadeCounters() {
        return fadeCounters;
    }

    private void handleMouseClick(int mouseX, int mouseY) {
        int numFloors = building.getNumFloors();
        int height = getHeight();
        int usableHeight = height - (2 * marginY);
        int floorHeight = usableHeight / numFloors;

        for (int i = 0; i < numFloors; i++) {
            int yLine = height - marginY - (i * floorHeight);
            int buttonY = yLine - 25;
            Floor floorObj = building.getFloor(i);

            // Check if click lies vertically within this floor's button row
            if (mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                // Check if UP button is clicked
                if (i < numFloors - 1 && mouseX >= buttonXUp && mouseX <= buttonXUp + buttonWidth) {
                    if (!floorObj.isUpButtonPressed()) {
                        spawnRiderAt(i, Direction.UP);
                        repaint();
                    }
                    return;
                }
                // Check if DOWN button is clicked
                if (i > 0 && mouseX >= buttonXDown && mouseX <= buttonXDown + buttonWidth) {
                    if (!floorObj.isDownButtonPressed()) {
                        spawnRiderAt(i, Direction.DOWN);
                        repaint();
                    }
                    return;
                }
            }
        }
    }

    private void spawnRiderAt(int floor, Direction dir) {
        int numFloors = building.getNumFloors();
        int destination = floor;

        if (dir == Direction.UP) {
            int range = numFloors - 1 - floor;
            if (range > 0) {
                destination = floor + 1 + (int) (Math.random() * range);
            }
        } else if (dir == Direction.DOWN) {
            if (floor > 0) {
                destination = (int) (Math.random() * floor);
            }
        }

        if (destination != floor) {
            int randomWeight = 100 + (int) (Math.random() * 100);
            Rider rider = new Rider(building, floor, destination, randomWeight);
            rider.requestElevator();
        }
    }

    private String getFloorLabel(int floorNumber, int numFloors) {
        if (floorNumber == 0) {
            return "Lobby";
        } else if (floorNumber == numFloors - 1) {
            return "Roof";
        } else {
            return "Floor " + (floorNumber + 1);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int numFloors = building.getNumFloors();
        
        int usableHeight = height - (2 * marginY);
        int floorHeight = usableHeight / numFloors;

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Click ▲ or ▼ to spawn a passenger and call an elevator:", 50, 30);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i < numFloors; i++) {
            int yLine = height - marginY - (i * floorHeight);
            int buttonY = yLine - 25;
            
            // Draw floor line up to the right margin
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(50, yLine, width - rightMargin, yLine);

            g2.setColor(Color.DARK_GRAY);
            String floorName = getFloorLabel(i, numFloors);
            g2.drawString(floorName, 50, yLine - 5);

            if (i < numFloors - 1) {
                Floor floorObj = building.getFloor(i);
                if (floorObj.isUpButtonPressed()) {
                    g2.setColor(new Color(255, 140, 0)); 
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fillRect(buttonXUp, buttonY, buttonWidth, buttonHeight);
                g2.setColor(Color.BLACK);
                g2.drawRect(buttonXUp, buttonY, buttonWidth, buttonHeight);
                g2.drawString("▲", buttonXUp + 5, buttonY + 14);
            }

            if (i > 0) {
                Floor floorObj = building.getFloor(i);
                if (floorObj.isDownButtonPressed()) {
                    g2.setColor(new Color(255, 140, 0)); 
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fillRect(buttonXDown, buttonY, buttonWidth, buttonHeight);
                g2.setColor(Color.BLACK);
                g2.drawRect(buttonXDown, buttonY, buttonWidth, buttonHeight);
                g2.drawString("▼", buttonXDown + 5, buttonY + 14);
            }

            int currentX = queueRightBound;
            List<Rider> occupantsInBuilding = new ArrayList<>(building.getOccupants());
            
            for (Rider rider : occupantsInBuilding) {
                if (rider.getElevator() == null && rider.getCurrentFloorNum() == i) {
                    boolean isWaiting = building.getFloor(i).getWaiting().contains(rider);

                    g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                    FontMetrics fm = g2.getFontMetrics();
                    String idStr = String.valueOf(rider.getId());
                    int textWidth = fm.stringWidth(idStr);
                    
                    int iconHeight = 14;
                    int iconWidth = Math.max(14, textWidth + 8); 

                    int riderX = currentX - iconWidth;
                    int iconY = yLine - 25;
                    
                    int textX = riderX + (iconWidth - textWidth) / 2;
                    int textY = iconY + ((iconHeight - fm.getHeight()) / 2) + fm.getAscent();

                    if (isWaiting) {
                        g2.setColor(new Color(70, 130, 180));
                        g2.fillRoundRect(riderX, iconY, iconWidth, iconHeight, iconHeight, iconHeight);
                        g2.setColor(Color.WHITE);
                        g2.drawString(idStr, textX, textY);
                    } else {
                        Composite originalComposite = g2.getComposite();
                        float alpha = 1.0f;
                        if (fadeCounters.containsKey(rider)) {
                            alpha = fadeCounters.get(rider) / 2.0f;
                            if (alpha < 0.0f) alpha = 0.0f;
                            if (alpha > 1.0f) alpha = 1.0f;
                        }

                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                        g2.setColor(new Color(46, 139, 87)); 
                        g2.fillRoundRect(riderX, iconY, iconWidth, iconHeight, iconHeight, iconHeight);
                        g2.setColor(Color.WHITE);
                        g2.drawString(idStr, textX, textY);
                        
                        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                        g2.setColor(new Color(46, 139, 87));
                        int doneWidth = g2.getFontMetrics().stringWidth("Done");
                        g2.drawString("Done", riderX + (iconWidth - doneWidth) / 2, yLine - 30);

                        g2.setComposite(originalComposite);
                    }

                    g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    currentX = riderX - 18; 
                }
            }
        }

        List<Elevator> elevators = building.getElevators();

        for (int i = 0; i < elevators.size(); i++) {
            Elevator elev = elevators.get(i);
            int x = shaftXStart + (i * shaftSpacing);
            int currentFloor = elev.getCurrentFloor();
            int y = height - marginY - (currentFloor * floorHeight) - floorHeight + 5;

            g2.setColor(new Color(230, 230, 230));
            g2.fillRect(x, marginY, shaftWidth, usableHeight);
            g2.setColor(Color.GRAY);
            g2.drawRect(x, marginY, shaftWidth, usableHeight);

            if (elev.isDoorsOpen()) {
                g2.setColor(new Color(144, 238, 144)); 
            } else {
                g2.setColor(new Color(112, 128, 144)); 
            }
            g2.fillRect(x + 5, y, shaftWidth - 10, floorHeight - 10);
            g2.setColor(Color.BLACK);
            g2.drawRect(x + 5, y, shaftWidth - 10, floorHeight - 10);

            g2.setColor(Color.BLACK);
            g2.drawString("E" + elev.getId(), x + 10, y + 18);
            g2.drawString("Occ: " + elev.getOccupants().size(), x + 10, y + 33);
            
            String dirSymbol = "";
            if (elev.getDirection() == Direction.UP) dirSymbol = "▲";
            else if (elev.getDirection() == Direction.DOWN) dirSymbol = "▼";
            else dirSymbol = "■";
            g2.drawString(dirSymbol, x + 50, y + 18);
        }
    }
}