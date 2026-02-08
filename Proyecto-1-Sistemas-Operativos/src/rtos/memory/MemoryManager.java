/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.memory;
import rtos.model.Process;
import rtos.model.ProcessState;
import rtos.structures.LinkedList;


/**
 * MemoryManager SIN usar java.util.*
 * Usa solo LinkedList propia y estructuras creadas por ti
 */

public class MemoryManager {
    private final int maxProcessesInRAM;
    private final LinkedList<Process> processesInRAM;
    private final LinkedList<Process> readySuspendedQueue;
    private final LinkedList<Process> blockedSuspendedQueue;
    
    public MemoryManager(int maxProcessesInRAM) {
        this.maxProcessesInRAM = maxProcessesInRAM;
        this.processesInRAM = new LinkedList<>();
        this.readySuspendedQueue = new LinkedList<>();
        this.blockedSuspendedQueue = new LinkedList<>();
    }
    
    // ========== MÉTODO PRINCIPAL MEJORADO ==========
    
    /**
     * Agrega proceso al sistema.
     * @param process Proceso a agregar
     * @return true si entró a RAM, false si fue suspendido
     */
    public boolean addProcess(Process process) {
        // Caso 1: Espacio disponible en RAM
        if (processesInRAM.size() < maxProcessesInRAM) {
            processesInRAM.add(process);
            return true; // Entró a RAM
        }
        
        // Caso 2: RAM llena - intentar suspender proceso existente
        Process toSuspend = findProcessToSuspend();
        
        if (toSuspend != null) {
            suspendProcess(toSuspend);
            processesInRAM.add(process);
            return true; // Entró a RAM (suspendiendo otro)
        }
        
        // Caso 3: No se pudo suspender - nuevo proceso va suspendido
        process.setState(ProcessState.READY_SUSPENDED);
        readySuspendedQueue.add(process);
        return false; // Fue suspendido
    }
    
    // ========== LÓGICA DE SUSPENSIÓN ==========
    
    private Process findProcessToSuspend() {
        Process candidate = null;
        int farthestDeadline = -1;
        
        for (int i = 0; i < processesInRAM.size(); i++) {
            Process p = processesInRAM.get(i);
            
            // NO suspender procesos RUNNING o prioridad 1
            if (p.getState() == ProcessState.RUNNING || p.getPriority() == 1) {
                continue;
            }
            
            // Verificar si está cerca de terminar (opcional, más seguro)
            if (p.getTotalInstructions() > 0 && 
                p.getExecutedInstructions() >= p.getTotalInstructions() * 0.9) {
                continue;
            }
            
            // Seleccionar deadline más lejano
            if (p.getRemainingDeadline() > farthestDeadline) {
                farthestDeadline = p.getRemainingDeadline();
                candidate = p;
            }
        }
        
        return candidate;
    }
    
    private void suspendProcess(Process process) {
        processesInRAM.remove(process);
        
        if (process.getState() == ProcessState.READY) {
            process.setState(ProcessState.READY_SUSPENDED);
            readySuspendedQueue.add(process);
        } else if (process.getState() == ProcessState.BLOCKED) {
            process.setState(ProcessState.BLOCKED_SUSPENDED);
            blockedSuspendedQueue.add(process);
        }
    }
    
    // ========== LÓGICA DE ACTIVACIÓN ==========
    
    /**
     * Llama este método cuando un proceso termina o se libera espacio.
     */
    public void tryActivateSuspendedProcesses() {
        while (processesInRAM.size() < maxProcessesInRAM && 
               !readySuspendedQueue.isEmpty()) {
            
            Process toActivate = getSuspendedProcessToActivate();
            if (toActivate == null) break;
            
            activateProcess(toActivate);
        }
    }
    
    private Process getSuspendedProcessToActivate() {
        if (readySuspendedQueue.isEmpty()) return null;
        
        Process best = null;
        int nearestDeadline = Integer.MAX_VALUE;
        
        for (int i = 0; i < readySuspendedQueue.size(); i++) {
            Process p = readySuspendedQueue.get(i);
            if (p.getRemainingDeadline() < nearestDeadline) {
                nearestDeadline = p.getRemainingDeadline();
                best = p;
            }
        }
        
        return best;
    }
    
    private void activateProcess(Process process) {
        if (readySuspendedQueue.remove(process)) {
            process.setState(ProcessState.READY);
            processesInRAM.add(process);
        } else if (blockedSuspendedQueue.remove(process)) {
            process.setState(ProcessState.BLOCKED);
            processesInRAM.add(process);
        }
    }
    
    // ========== MÉTODOS PARA SIMULATIONENGINE ==========
    
    /**
     * Remueve proceso terminado y activa suspendidos si hay espacio.
     */
    public void processTerminated(Process process) {
        if (processesInRAM.remove(process)) {
            tryActivateSuspendedProcesses();
        } else {
            readySuspendedQueue.remove(process);
            blockedSuspendedQueue.remove(process);
        }
    }
    
    /**
     * Proceso completó E/S y está listo.
     */
    public void processIOCompleted(Process process) {
        // Si estaba bloqueado-suspendido, cambiar a ready-suspendido
        if (blockedSuspendedQueue.remove(process)) {
            process.setState(ProcessState.READY_SUSPENDED);
            readySuspendedQueue.add(process);
        }
    }
    
    // ========== GETTERS ==========
    
    public boolean hasSpaceInRAM() {
        return processesInRAM.size() < maxProcessesInRAM;
    }
    
    public int getAvailableSpaceInRAM() {
        return maxProcessesInRAM - processesInRAM.size();
    }
    
    public int getRAMUsage() {
        return processesInRAM.size();
    }
    
    public int getMaxRAMCapacity() {
        return maxProcessesInRAM;
    }
    
    public int getReadySuspendedCount() {
        return readySuspendedQueue.size();
    }
    
    public int getBlockedSuspendedCount() {
        return blockedSuspendedQueue.size();
    }
    
    public LinkedList<Process> getProcessesInRAM() {
        return processesInRAM;
    }
    
    public LinkedList<Process> getReadySuspendedQueue() {
        return readySuspendedQueue;
    }
    
    public LinkedList<Process> getBlockedSuspendedQueue() {
        return blockedSuspendedQueue;
    }
}