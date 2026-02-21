/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package rtos.scheduler;

/**
 *
 * @author luisf
 */

import rtos.model.Process;
import rtos.model.ProcessState;
import rtos.structures.Queue;

public class RoundRobinScheduler implements Scheduler {
    private Queue<Process> readyQueue;
    private int quantum;
    
    public RoundRobinScheduler(int quantum) {
        this.readyQueue = new Queue<>();
        this.quantum = quantum;
    }
    
    @Override
    public String getName() {
        return "Round Robin (Quantum: " + quantum + ")";
    }
    
    @Override
    public void addProcess(Process process) {
        process.setState(ProcessState.READY);
        readyQueue.enqueue(process);
    }
    
    @Override
    public Process getNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }
        Process process = readyQueue.dequeue();
        process.setState(ProcessState.RUNNING);
        return process;
    }
    
    @Override
    public boolean isEmpty() {
        return readyQueue.isEmpty();
    }
    
    @Override
    public Queue<Process> getReadyQueue() {
        return readyQueue;
    }
    
    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }
    
    public int getQuantum() {
        return quantum;
    }
}
