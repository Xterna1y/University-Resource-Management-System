package com.campus.client.model;

/**
 * Base type for a bookable campus facility. Kept intentionally simple: the
 * authoritative facility data lives in the server's text files and is
 * retrieved as free text via MCP (campus://facilities, check_room_availability).
 * These subclasses exist to satisfy the OOP design (report Table: Model
 * classes) and can be populated once the real facilities.txt format is known.
 */
public abstract class Facility {

    protected String resourceId;
    protected String name;
    protected String building;
    protected int capacity;

    protected Facility(String resourceId, String name, String building, int capacity) {
        this.resourceId = resourceId;
        this.name = name;
        this.building = building;
        this.capacity = capacity;
    }

    public String getResourceId() { return resourceId; }
    public String getName() { return name; }
    public String getBuilding() { return building; }
    public int getCapacity() { return capacity; }

    /** Short label for the facility type, shown in the UI. */
    public abstract String getType();

    @Override
    public String toString() {
        return name + " (" + getType() + ", " + building + ", capacity " + capacity + ")";
    }
}