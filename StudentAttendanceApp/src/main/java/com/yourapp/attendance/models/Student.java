package com.yourapp.attendance.models;

public class Student {
    private String studentId;
    private String firstName;
    private String lastName;
    private String fatherName;
    private String className;
    private String classId;
    private String gender;
    private int enrolledYear;
    private String username;
    private String password;
    private int presentDays;
    private int absentDays;
    private String takenBy;

    public Student(String studentId, String firstName, String lastName, String fatherName,
                   String className, String classId, String gender, int enrolledYear,
                   String username, String password) {
        this.studentId = studentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fatherName = fatherName;
        this.className = className;
        this.classId = classId;
        this.gender = gender;
        this.enrolledYear = enrolledYear;
        this.username = username;
        this.password = password;
    }

    public String getStudentId() { return studentId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFatherName() { return fatherName; }
    public String getClassName() { return className; }
    public String getClassId() { return classId; }
    public String getGender() { return gender; }
    public int getEnrolledYear() { return enrolledYear; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public void setPresentDays(int presentDays) {
        this.presentDays = presentDays;
    }

    public void setAbsentDays(int absentDays) {
        this.absentDays = absentDays;
    }

    public void setTakenBy(String takenBy) {
        this.takenBy = takenBy;
    }

    public int getPresentDays() {
        return presentDays;
    }

    public int getAbsentDays() {
        return absentDays;
    }

    public String getTakenBy() {
        return takenBy;
    }
}
