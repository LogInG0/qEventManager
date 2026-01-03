package wtf.pawlend.qeventmanager.config;

import java.util.ArrayList;
import java.util.List;

public class Schedule {

    private final String name;
    private final String type;
    private final String time;
    private final List<String> days;
    private final List<String> events;
    private final String mode;
    private final int minPlayers;
    private final int maxPlayers;
    private boolean enabled;

    public Schedule(String name, String type, String time, List<String> days,
                    List<String> events, String mode, int minPlayers, int maxPlayers, boolean enabled) {
        this.name = name;
        this.type = type;
        this.time = time;
        this.days = days != null ? days : new ArrayList<>();
        this.events = events != null ? events : new ArrayList<>();
        this.mode = mode;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getTime() {
        return time;
    }

    public List<String> getDays() {
        return days;
    }

    public List<String> getEvents() {
        return events;
    }

    public String getMode() {
        return mode;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}