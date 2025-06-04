package com.yourapp.attendance.models;

public class TeacherRow {
    private String teacherId;
    private String firstName;
    private String lastName;
    private String gender;
    private String assignedClass;
    private String todayStatus;
    private int presentDays;
    private int absentDays;
    private int leaveDays;
    private String attendancePercentage;
    private boolean edited = false;

    public TeacherRow(String teacherId, String firstName, String lastName,
                      String gender, String assignedClass, String todayStatus,
                      int presentDays, int absentDays, int leaveDays, String attendancePercentage) {
        this.teacherId = teacherId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.assignedClass = assignedClass;
        this.todayStatus = todayStatus;
        this.presentDays = presentDays;
        this.absentDays = absentDays;
        this.leaveDays = leaveDays;
        this.attendancePercentage = attendancePercentage;
    }

    // Getters
    public String getTeacherId() { return teacherId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getGender() { return gender; }
    public String getClassName() { return assignedClass; }
    public String getTodayStatus() { return todayStatus; }
    public int getPresentDays() { return presentDays; }
    public int getAbsentDays() { return absentDays; }
    public int getLeaveDays() { return leaveDays; }
    public String getAttendancePercentage() { return attendancePercentage; }

    // Setters
    public void setTodayStatus(String s) { this.todayStatus = s; }
    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }
}
