/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.scheduler;

/**
 *
 * @author luisf
 */
import main.process.Process;
import main.structures.CustomQueue;

public class FCFS implements Scheduler {
    private final CustomQueue<Process> readyQueue;
    private final String name;
    
    public FCFS() {
        readyQueue = new CustomQueue<>();
        name = "FCFS (First Come First Served)";
    }
    
    @Override
    public void addProcess(Process process) {
        readyQueue.enqueue(process);
    }
    
    @Override
    public Process getNextProcess() {
        return readyQueue.dequeue();
    }
    
    @Override
    public boolean hasNextProcess() {
        return !readyQueue.isEmpty();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void clear() {
        readyQueue.clear();
    }
    
    public Object[] getQueueArray() {
        return readyQueue.toArray();
    }
}