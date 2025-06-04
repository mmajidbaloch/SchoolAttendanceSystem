package com.yourapp.attendance.models;

public class Teacher {
    private String teacherId;
    private String firstName;
    private String lastName;
    private String className;
    private String classId;
    private String gender;
    private String contact;
    private String username;
    private String password;

    public Teacher(String teacherId, String firstName, String lastName, String className,
                   String classId, String gender, String contact, String username, String password) {
        this.teacherId = teacherId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.className = className;
        this.classId = classId;
        this.gender = gender;
        this.contact = contact;
        this.username = username;
        this.password = password;
    }

    public String getTeacherId() { return teacherId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getClassName() { return className; }
    public String getClassId() { return classId; }
    public String getGender() { return gender; }
    public String getContact() { return contact; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
