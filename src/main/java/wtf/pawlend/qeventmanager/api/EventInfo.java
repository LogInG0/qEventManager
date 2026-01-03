package wtf.pawlend.qeventmanager.api;

import org.bukkit.inventory.ItemStack;

/**
 * Информация о зарегистрированном ивенте
 */
public class EventInfo {

    private final String id;
    private final String displayName;
    private final String description;
    private final ItemStack icon;
    private final int minPlayers;
    private final int maxPlayers;
    private final int weight;
    private final boolean votingAllowed;
    private final boolean randomAllowed;
    private final String ownerPlugin;
    private EventStatus status;
    private boolean enabled;

    public EventInfo(QEvent event) {
        this.id = event.getId();
        this.displayName = event.getDisplayName();
        this.description = event.getDescription();
        this.icon = event.getIcon().clone();
        this.minPlayers = event.getMinPlayers();
        this.maxPlayers = event.getMaxPlayers();
        this.weight = event.getCustomWeight() > 0 ? event.getCustomWeight() : event.getPriority().getWeight();
        this.votingAllowed = event.isVotingAllowed();
        this.randomAllowed = event.isRandomAllowed();
        this.ownerPlugin = event.getOwnerPlugin();
        this.status = EventStatus.AVAILABLE;
        this.enabled = true;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public ItemStack getIcon() {
        return icon.clone();
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isVotingAllowed() {
        return votingAllowed;
    }

    public boolean isRandomAllowed() {
        return randomAllowed;
    }

    public String getOwnerPlugin() {
        return ownerPlugin;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            this.status = EventStatus.DISABLED;
        } else if (this.status == EventStatus.DISABLED) {
            this.status = EventStatus.AVAILABLE;
        }
    }
}