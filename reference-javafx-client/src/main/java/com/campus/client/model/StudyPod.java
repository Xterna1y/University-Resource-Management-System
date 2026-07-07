package com.campus.client.model;

public class StudyPod extends Facility {

    public StudyPod(String resourceId,
                    String name,
                    String building,
                    int capacity) {
        super(resourceId, name, building, capacity);
    }

    @Override
    public String getType() {
        return "Study Pod";
    }
}