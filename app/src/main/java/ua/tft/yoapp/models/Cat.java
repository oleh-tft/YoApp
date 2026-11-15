package ua.tft.yoapp.models;

import java.io.Serializable;

// Реалізуємо Serializable, щоб об'єкт можна було покласти в Bundle при повороті
public class Cat implements Serializable {
    private String name;
    private int age;
    private String color;

    // Конструктор
    public Cat(String name, int age, String color) {
        this.name = name;
        this.age = age;
        this.color = color;
    }

    // Геттери та Сеттери
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}