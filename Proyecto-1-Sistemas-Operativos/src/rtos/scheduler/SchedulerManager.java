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

/**
 * Gestiona múltiples algoritmos de planificación y permite cambiar entre ellos
 */
public class SchedulerManager {
    private Scheduler currentScheduler;
    private FCFSScheduler fcfsScheduler;
    private RoundRobinScheduler rrScheduler;
    private SRTScheduler srtScheduler;
    private PriorityScheduler priorityScheduler;
    private EDFScheduler edfScheduler;
    
    public enum Algorithm {
        FCFS, ROUND_ROBIN, SRT, PRIORITY, EDF
    }
    
    public SchedulerManager() {
        // Crear todos los schedulers
        this.fcfsScheduler = new FCFSScheduler();
        this.rrScheduler = new RoundRobinScheduler(4); // Quantum por defecto: 4
        this.srtScheduler = new SRTScheduler();
        this.priorityScheduler = new PriorityScheduler();
        this.edfScheduler = new EDFScheduler();
        
        // Establecer FCFS como scheduler por defecto
        this.currentScheduler = fcfsScheduler;
    }
    
    /**
     * Cambia el algoritmo de planificación actual
     */
    public void switchAlgorithm(Algorithm algorithm) {
        System.out.println("Cambiando algoritmo a: " + algorithm);
        
        // Mover procesos del scheduler actual al nuevo scheduler
        Queue<Process> currentQueue = currentScheduler.getReadyQueue();
        
        // Guardar referencia al scheduler anterior
        Scheduler oldScheduler = currentScheduler;
        
        // Cambiar al nuevo scheduler
        switch(algorithm) {
            case FCFS:
                currentScheduler = fcfsScheduler;
                break;
            case ROUND_ROBIN:
                currentScheduler = rrScheduler;
                break;
            case SRT:
                currentScheduler = srtScheduler;
                break;
            case PRIORITY:
                currentScheduler = priorityScheduler;
                break;
            case EDF:
                currentScheduler = edfScheduler;
                break;
        }
        
        // Transferir procesos del scheduler anterior al nuevo
        transferProcesses(oldScheduler, currentScheduler, currentQueue);
    }
    
    /**
     * Agrega un proceso al scheduler actual
     */
    public void addProcess(Process process) {
        currentScheduler.addProcess(process);
    }
    
    /**
     * Obtiene el próximo proceso a ejecutar
     */
    public Process getNextProcess() {
        return currentScheduler.getNextProcess();
    }
    
    /**
     * Verifica si no hay procesos en el scheduler actual
     */
    public boolean isEmpty() {
        return currentScheduler.isEmpty();
    }
    
    /**
     * Obtiene la cola de listos del scheduler actual
     */
    public Queue<Process> getReadyQueue() {
        return currentScheduler.getReadyQueue();
    }
    
    /**
     * Obtiene el nombre del algoritmo actual
     */
    public String getCurrentAlgorithmName() {
        return currentScheduler.getName();
    }
    
    /**
     * Obtiene el algoritmo actual
     */
    public Algorithm getCurrentAlgorithm() {
        if (currentScheduler instanceof FCFSScheduler) return Algorithm.FCFS;
        if (currentScheduler instanceof RoundRobinScheduler) return Algorithm.ROUND_ROBIN;
        if (currentScheduler instanceof SRTScheduler) return Algorithm.SRT;
        if (currentScheduler instanceof PriorityScheduler) return Algorithm.PRIORITY;
        if (currentScheduler instanceof EDFScheduler) return Algorithm.EDF;
        return Algorithm.FCFS;
    }
    
    /**
     * Establece el quantum para Round Robin
     */
    public void setQuantum(int quantum) {
        if (rrScheduler != null) {
            rrScheduler.setQuantum(quantum);
        }
    }
    
    /**
     * Transfiere procesos entre schedulers
     */
    private void transferProcesses(Scheduler from, Scheduler to, Queue<Process> queue) {
        // Copiar procesos de la cola anterior
        Queue<Process> tempQueue = new Queue<>();
        
        // Crear una copia de los procesos
        while (!queue.isEmpty()) {
            Process p = queue.dequeue();
            tempQueue.enqueue(p);
        }
        
        // Transferir procesos al nuevo scheduler
        while (!tempQueue.isEmpty()) {
            Process p = tempQueue.dequeue();
            p.setState(ProcessState.READY); // Asegurar que esté en estado READY
            to.addProcess(p);
            // También agregar de vuelta a la cola original para mantener consistencia
            queue.enqueue(p);
        }
    }
}
