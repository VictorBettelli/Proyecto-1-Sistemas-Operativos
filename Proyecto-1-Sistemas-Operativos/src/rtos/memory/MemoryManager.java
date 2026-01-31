/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.memory;
import rtos.model.Process;
import rtos.structures.LinkedList;
/**
 *
 * @author VictorB
 */
public class MemoryManager {

    private int maxProcesses;
    private LinkedList<Process> processesInMemory;
    
    public MemoryManager(int maxProcesses) {
        this.maxProcesses = maxProcesses;
        this.processesInMemory = new LinkedList<>();
    }
    
    public boolean hasSpace() {
        return processesInMemory.size() < maxProcesses;
    }
    
    public int getAvailableSpace() {
        return maxProcesses - processesInMemory.size();
    }
    
    public int getMaxProcesses() {
        return maxProcesses;
    }
}