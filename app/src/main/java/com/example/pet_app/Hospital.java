package com.example.pet_app;

public class Hospital {
    private String name;
    private String address;
    private String phone;

    public Hospital(String name, String address, String phone) {
        this.name = name;
        this.address = address;
        this.phone = phone;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
}