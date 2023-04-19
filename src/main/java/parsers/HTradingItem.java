package parsers;

import gearth.extensions.parsers.HStuff;
import gearth.protocol.HPacket;

import java.util.Arrays;

public class HTradingItem {
    private final int itemId;
    private final String itemType;
    private final int roomItemId;
    private final int itemTypeId;
    private final int category;
    private final Object[] stuffData;
    private final int creationDay;
    private final int creationMonth;
    private final int creationYear;
    private final int extra;
    private final int stuffCategory;
    public final boolean isGroupable;

    public HTradingItem(HPacket packet) {
        this.itemId = packet.readInteger();
        System.out.println("itemId " + itemId);
        this.itemType = packet.readString();
        System.out.println("itemType " + itemType);
        this.roomItemId = packet.readInteger();
        System.out.println("roomItemId " + roomItemId);
        this.itemTypeId = packet.readInteger();
        System.out.println("itemTypeId " + itemTypeId);
        this.category = packet.readInteger();
        System.out.println("category " + category);
        this.isGroupable = packet.readBoolean();
        System.out.println("isGroupable " + isGroupable);
        this.stuffCategory = packet.readInteger();
        System.out.println("stuffCategory " + stuffCategory);
        this.stuffData = HStuff.readData(packet, stuffCategory);
        System.out.println("stuffData " + Arrays.toString(stuffData));
        this.creationDay = packet.readInteger();
        System.out.println("creationDay " + creationDay);
        this.creationMonth = packet.readInteger();
        System.out.println("creationMonth " + creationMonth);
        this.creationYear = packet.readInteger();
        System.out.println("creationYear " + creationYear);
        this.extra = itemType.equalsIgnoreCase("S") ? packet.readInteger() : -1;
        System.out.println("extra " + extra);

    }
    public int getStuffCategory() {
        return stuffCategory;
    }

    public boolean isGroupable() {
        return isGroupable;
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemType() {
        return itemType;
    }

    public int getRoomItemId() {
        return roomItemId;
    }

    public int getItemTypeId() {
        return itemTypeId;
    }

    public int getCategory() {
        return category;
    }

    public Object[] getStuffData() {
        return stuffData;
    }

    public int getCreationDay() {
        return creationDay;
    }

    public int getCreationMonth() {
        return creationMonth;
    }

    public int getCreationYear() {
        return creationYear;
    }

    public int getExtra() {
        return extra;
    }
}
