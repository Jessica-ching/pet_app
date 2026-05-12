package com.example.pet_app.mainfeature.pet;

public class PetModel {
    private int id;
    private String name, species, gender;
    private int currentCals, goalCals;
    private float weight;
    private boolean isExpanded = false; // 🌟 記住展開狀態

    public PetModel(int id, String name, String species, String gender, int age, float weight) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.gender = gender;
        this.weight = weight;
    }

    public int getId() { return id; }
    public String getName() { return name != null ? name : "未命名"; }
    public int getCurrentCals() { return currentCals; }
    public void setCurrentCals(int currentCals) { this.currentCals = currentCals; }
    public int getGoalCals() { return goalCals; }
    public void setGoalCals(int goalCals) { this.goalCals = goalCals; }

    // 🌟 控制展開的 Getter/Setter
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}