package com.example.pet_app.mainfeature.pet;

public class PetModel {
    private int id;
    private String name;
    private String species;
    private String gender;
    private int age;
    private float weight;

    public PetModel(int id, String name, String species, String gender, int age, float weight) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.gender = gender;
        this.age = age;
        this.weight = weight;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
