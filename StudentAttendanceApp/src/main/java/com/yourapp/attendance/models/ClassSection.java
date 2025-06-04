package com.yourapp.attendance.models;

public class ClassSection {
    private String classId;
    private String className;
    private int capacity;

    public ClassSection(String classId, String className, int capacity) {
        this.classId = classId;
        this.className = className;
        this.capacity = capacity;
    }

    public String getClassId() { return classId; }
    public String getClassName() { return className; }
    public int getCapacity() { return capacity; }

    public void setClassId(String classId) { this.classId = classId; }
    public void setClassName(String className) { this.className = className; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}


