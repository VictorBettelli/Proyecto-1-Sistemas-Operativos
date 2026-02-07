/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.memory;
import rtos.model.Process;
import rtos.model.ProcessState;
import rtos.structures.LinkedList;

public class MemoryManager {
    private LinkedList<Process> processesInMemory;
    private LinkedList<Process> suspendedProcesses;
    private int maxProcessesInMemory;
    private int currentProcessCount;
    
    public MemoryManager(int maxProcesses) {
        this.processesInMemory = new LinkedList<>();
        this.suspendedProcesses = new LinkedList<>();
        this.maxProcessesInMemory = maxProcesses;
        this.currentProcessCount = 0;
    }
    
    /**
     * Intenta cargar un proceso a memoria
     * @return true si se pudo cargar, false si hay que suspender algún proceso
     */
    public boolean loadProcess(Process process) {
        if (currentProcessCount < maxProcessesInMemory) {
            // Hay espacio en memoria
            processesInMemory.add(process);
            currentProcessCount++;
            return true;
        } else {
            // Memoria llena - suspender proceso menos prioritario
            Process toSuspend = findProcessToSuspend();
            if (toSuspend != null) {
                suspendProcess(toSuspend);
                processesInMemory.add(process);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Encuentra el proceso más adecuado para suspender
     * Prioriza: procesos no críticos, con deadlines lejanos, baja prioridad
     */
    private Process findProcessToSuspend() {
        Process candidate = null;
        int maxDeadline = Integer.MIN_VALUE;
        
        for (int i = 0; i < processesInMemory.size(); i++) {
            Process p = processesInMemory.get(i);
            
            // No suspender procesos en ejecución o críticos
            if (p.getState() == ProcessState.RUNNING || 
                p.getPriority() == 1) { // Prioridad 1 = crítica
                continue;
            }
            
            // Elegir proceso con deadline más lejano (menos urgente)
            if (p.getRemainingDeadline() > maxDeadline) {
                maxDeadline = p.getRemainingDeadline();
                candidate = p;
            }
        }
        
        return candidate;
    }
    
    /**
     * Suspende un proceso (mueve a memoria secundaria)
     */
    private void suspendProcess(Process process) {
        // Cambiar estado según si estaba listo o bloqueado
        if (process.getState() == ProcessState.READY) {
            process.setState(ProcessState.READY_SUSPENDED);
        } else if (process.getState() == ProcessState.BLOCKED) {
            process.setState(ProcessState.BLOCKED_SUSPENDED);
        }
        
        // Mover a cola de suspendidos
        processesInMemory.remove(process); // Necesitas implementar remove() en LinkedList
        suspendedProcesses.add(process);
        currentProcessCount--;
    }
    
    /**
     * Reactiva un proceso suspendido
     */
    public boolean activateProcess(Process process) {
        if (currentProcessCount < maxProcessesInMemory && 
            suspendedProcesses.contains(process)) {
            
            suspendedProcesses.remove(process);
            processesInMemory.add(process);
            currentProcessCount++;
            
            // Restaurar estado original
            if (process.getState() == ProcessState.READY_SUSPENDED) {
                process.setState(ProcessState.READY);
            } else if (process.getState() == ProcessState.BLOCKED_SUSPENDED) {
                process.setState(ProcessState.BLOCKED);
            }
            
            return true;
        }
        return false;
    }
    
    // Getters
    public LinkedList<Process> getProcessesInMemory() { return processesInMemory; }
    public LinkedList<Process> getSuspendedProcesses() { return suspendedProcesses; }
    public int getCurrentProcessCount() { return currentProcessCount; }
    public int getMaxProcessesInMemory() { return maxProcessesInMemory; }
}