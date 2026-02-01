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
    private int currentQuantum;
    private Process currentProcess;
    
    public RoundRobinScheduler(int quantum) {
        this.readyQueue = new Queue<>();
        this.quantum = quantum;
        this.currentQuantum = 0;
        this.currentProcess = null;
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
        // Si hay un proceso actual y aún tiene quantum
        if (currentProcess != null && currentQuantum < quantum) {
            currentQuantum++;
            return currentProcess;
        }
        
        // Si se acabó el quantum o no hay proceso actual
        if (currentProcess != null) {
            // Devolver el proceso actual a la cola
            currentProcess.setState(ProcessState.READY);
            readyQueue.enqueue(currentProcess);
        }
        
        // Tomar nuevo proceso
        if (!readyQueue.isEmpty()) {
            currentProcess = readyQueue.dequeue();
            currentProcess.setState(ProcessState.RUNNING);
            currentQuantum = 1;
            return currentProcess;
        }
        
        currentProcess = null;
        currentQuantum = 0;
        return null;
    }
    
    @Override
    public boolean isEmpty() {
        return readyQueue.isEmpty() && currentProcess == null;
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