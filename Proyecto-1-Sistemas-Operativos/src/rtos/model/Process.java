/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.model;

/**
 *
 * @author VictorB
 */
public class Process {
  
    private String id;
    private String name;
    
    public Process(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    
    @Override
    public String toString() {
        return "Process[" + id + ": " + name + "]";
    }
}

