package com.yourapp.attendance.models;


public class StudentRow {
    private String studentId;
    private String firstName;
    private String lastName;
    private String fatherName;
    private String gender;
    private String todayStatus;
    private int presentDays;
    private int absentDays;
    private String className;
    private boolean edited = false;

    public StudentRow(String studentId, String firstName, String lastName, String fatherName,
                      String gender, String todayStatus, int presentDays, int absentDays) {
        this.studentId = studentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fatherName = fatherName;
        this.gender = gender;
        this.todayStatus = todayStatus;
        this.presentDays = presentDays;
        this.absentDays = absentDays;
    }

    public String getStudentId() { return studentId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFatherName() { return fatherName; }
    public String getGender() { return gender; }
    public String getTodayStatus() { return todayStatus; }
    public void setTodayStatus(String s) { this.todayStatus = s; }
    public int getPresentDays() { return presentDays; }
    public int getAbsentDays() { return absentDays; }
    public void setClassName(String s) { this.className = s; }
    public String getClassName() { return this.className; }
    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }


}
