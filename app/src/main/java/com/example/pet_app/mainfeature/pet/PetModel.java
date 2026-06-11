package com.example.pet_app.mainfeature.pet;

public class PetModel {
    private int id;
    private String name, species, gender;
    private int currentCals, goalCals;
    // 🌟 補上飲水變數
    private int currentWater, goalWater;
    private float weight;
    private boolean isExpanded = false;

    public PetModel(int id, String name, String species, String gender, int age, float weight) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.gender = gender;
        this.weight = weight;
    }

    public int getId() { return id; }
    public String getName() { return name != null ? name : "未命名"; }

    // 熱量相關
    public int getCurrentCals() { return currentCals; }
    public void setCurrentCals(int currentCals) { this.currentCals = currentCals; }
    public int getGoalCals() { return goalCals; }
    public void setGoalCals(int goalCals) { this.goalCals = goalCals; }

    // 🌟 補上飲水相關的 Getter/Setter
    public int getCurrentWater() { return currentWater; }
    public void setCurrentWater(int currentWater) { this.currentWater = currentWater; }
    public int getGoalWater() { return goalWater; }
    public void setGoalWater(int goalWater) { this.goalWater = goalWater; }

    // 控制展開
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}