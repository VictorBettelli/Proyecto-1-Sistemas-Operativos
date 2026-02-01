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

public class FCFSScheduler implements Scheduler {
    private Queue<Process> readyQueue;
    
    public FCFSScheduler() {
        this.readyQueue = new Queue<>();
    }
    
    @Override
    public String getName() {
        return "FCFS (First Come First Served)";
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
}
