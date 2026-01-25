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

public class RoundRobin implements Scheduler {
    private CustomQueue<Process> readyQueue;
    private int quantum;
    private String name;
    
    public RoundRobin(int quantum) {
        readyQueue = new CustomQueue<>();
        this.quantum = quantum;
        name = "Round Robin (Quantum: " + quantum + ")";
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
    
    public void setQuantum(int quantum) {
        this.quantum = quantum;
        this.name = "Round Robin (Quantum: " + quantum + ")";
    }
    
    public int getQuantum() {
        return quantum;
    }
    
    public Object[] getQueueArray() {
        return readyQueue.toArray();
    }
}
