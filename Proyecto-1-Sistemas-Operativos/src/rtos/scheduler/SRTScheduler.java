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
import rtos.structures.PriorityQueue;
import rtos.utils.Comparator;

public class SRTScheduler implements Scheduler {
    private PriorityQueue<Process> readyQueue;
    
    public SRTScheduler() {
        this.readyQueue = new PriorityQueue<>(new Comparator.RemainingTimeComparator());
    }
    
    @Override
    public String getName() {
        return "SRT (Shortest Remaining Time)";
    }
    
    @Override
    public void addProcess(Process process) {
        process.setState(ProcessState.READY);
        readyQueue.insert(process);
    }
    
    @Override
    public Process getNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }
        Process process = readyQueue.extractMin();
        process.setState(ProcessState.RUNNING);
        return process;
    }
    
    @Override
    public boolean isEmpty() {
        return readyQueue.isEmpty();
    }
    
    @Override
    public rtos.structures.Queue<Process> getReadyQueue() {
        // Convertir PriorityQueue a Queue para la interfaz
        rtos.structures.Queue<Process> queue = new rtos.structures.Queue<>();
        
        // Crear una copia temporal usando el mismo comparador
        PriorityQueue<Process> temp = new PriorityQueue<>(new Comparator.RemainingTimeComparator());
        
        // Extraer todos los procesos
        while (!readyQueue.isEmpty()) {
            Process p = readyQueue.extractMin();
            queue.enqueue(p);
            temp.insert(p);
        }
        
        // Restaurar la cola original
        while (!temp.isEmpty()) {
            readyQueue.insert(temp.extractMin());
        }
        
        return queue;
    }
}