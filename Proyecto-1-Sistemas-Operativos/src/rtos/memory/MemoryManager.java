/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.memory;
import rtos.model.Process;
import rtos.model.ProcessState;
import rtos.structures.LinkedList;
import java.util.concurrent.Semaphore;

/**
 * @luisf
 * MemoryManager con semáforos de java.util.concurrent para sincronización concurrente
 * Usa solo LinkedList propia y estructuras creadas por ti
 */
public class MemoryManager {
    private final int maxProcessesInRAM;
    private final LinkedList<Process> processesInRAM;
    private final LinkedList<Process> readySuspendedQueue;
    private final LinkedList<Process> blockedSuspendedQueue;
    
    // ========== SEMÁFOROS PARA SINCRONIZACIÓN ==========
    private final Semaphore ramSemaphore;       // Para acceso exclusivo a RAM
    private final Semaphore readySuspendSemaphore; // Para readySuspendedQueue
    private final Semaphore blockedSuspendSemaphore; // Para blockedSuspendedQueue
    private final Semaphore operationSemaphore; // Para operaciones complejas
    
    public MemoryManager(int maxProcessesInRAM) {
        this.maxProcessesInRAM = maxProcessesInRAM;
        this.processesInRAM = new LinkedList<>();
        this.readySuspendedQueue = new LinkedList<>();
        this.blockedSuspendedQueue = new LinkedList<>();
        
        // Inicializar semáforos de java.util.concurrent
        this.ramSemaphore = new Semaphore(1);        // Mutex para RAM
        this.readySuspendSemaphore = new Semaphore(1); // Mutex para ready suspend
        this.blockedSuspendSemaphore = new Semaphore(1); // Mutex para blocked suspend
        this.operationSemaphore = new Semaphore(1);  // Mutex para operaciones
    }
    
    // ========== MÉTODO PRINCIPAL CON SEMÁFOROS ==========
    
    /**
     * Agrega proceso al sistema con sincronización segura.
     * @param process Proceso a agregar
     * @return true si entró a RAM, false si fue suspendido
     */
    public boolean addProcess(Process process) {
        try {
            // Adquirir semáforo de operación
            operationSemaphore.acquire();
            
            // Caso 1: Espacio disponible en RAM
            ramSemaphore.acquire();
            boolean hasSpace = processesInRAM.size() < maxProcessesInRAM;
            ramSemaphore.release();
            
            if (hasSpace) {
                ramSemaphore.acquire();
                processesInRAM.add(process);
                ramSemaphore.release();
                operationSemaphore.release();
                return true;
            }
            
            // Caso 2: RAM llena - intentar suspender proceso existente
            Process toSuspend = findProcessToSuspend();
            
            if (toSuspend != null) {
                suspendProcess(toSuspend);
                
                ramSemaphore.acquire();
                processesInRAM.add(process);
                ramSemaphore.release();
                
                operationSemaphore.release();
                return true;
            }
            
            // Caso 3: No se pudo suspender - nuevo proceso va suspendido
            process.setState(ProcessState.READY_SUSPENDED);
            
            readySuspendSemaphore.acquire();
            readySuspendedQueue.add(process);
            readySuspendSemaphore.release();
            
            operationSemaphore.release();
            return false;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("MemoryManager: Interrupción al agregar proceso");
            return false;
        }
    }
    
    // ========== LÓGICA DE SUSPENSIÓN CON SEMÁFOROS ==========
    
    private Process findProcessToSuspend() {
        try {
            ramSemaphore.acquire();
            
            if (processesInRAM.isEmpty()) {
                ramSemaphore.release();
                return null;
            }
            
            Process blockedCandidate = null;
            int blockedFarthestDeadline = -1;
            Process readyCandidate = null;
            int readyFarthestDeadline = -1;
            
            for (int i = 0; i < processesInRAM.size(); i++) {
                Process p = processesInRAM.get(i);
                
                // NO suspender procesos RUNNING o prioridad 1
                if (p.getState() == ProcessState.RUNNING || p.getPriority() == 1) {
                    continue;
                }
                
                // Verificar si está cerca de terminar
                if (p.getTotalInstructions() > 0 && 
                    p.getExecutedInstructions() >= p.getTotalInstructions() * 0.9) {
                    continue;
                }
                
                // Preferir procesos BLOCKED para poblar cola BLOCKED_SUSPENDED
                if (p.getState() == ProcessState.BLOCKED) {
                    if (p.getRemainingDeadline() > blockedFarthestDeadline) {
                        blockedFarthestDeadline = p.getRemainingDeadline();
                        blockedCandidate = p;
                    }
                } else {
                    if (p.getRemainingDeadline() > readyFarthestDeadline) {
                        readyFarthestDeadline = p.getRemainingDeadline();
                        readyCandidate = p;
                    }
                }
            }
            
            ramSemaphore.release();
            return blockedCandidate != null ? blockedCandidate : readyCandidate;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    private void suspendProcess(Process process) {
        try {
            // Remover de RAM
            ramSemaphore.acquire();
            boolean removed = processesInRAM.remove(process);
            ramSemaphore.release();
            
            if (!removed) return;
            
            // Mover a la cola suspendida correspondiente
            if (process.getState() == ProcessState.READY) {
                process.setState(ProcessState.READY_SUSPENDED);
                readySuspendSemaphore.acquire();
                readySuspendedQueue.add(process);
                readySuspendSemaphore.release();
                
            } else if (process.getState() == ProcessState.BLOCKED) {
                process.setState(ProcessState.BLOCKED_SUSPENDED);
                blockedSuspendSemaphore.acquire();
                blockedSuspendedQueue.add(process);
                blockedSuspendSemaphore.release();
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // ========== LÓGICA DE ACTIVACIÓN CON SEMÁFOROS ==========
    
    /**
     * Intenta activar procesos suspendidos cuando hay espacio.
     */
    public void tryActivateSuspendedProcesses() {
        boolean operationAcquired = false;
        try {
            operationSemaphore.acquire();
            operationAcquired = true;
            activateSuspendedProcessesUnderOperationLock();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (operationAcquired) {
                operationSemaphore.release();
            }
        }
    }
    
    /**
     * Activa procesos suspendidos.
     * Debe llamarse cuando operationSemaphore ya fue adquirido.
     */
    private void activateSuspendedProcessesUnderOperationLock() {
        while (hasSpaceInRAM() && getReadySuspendedCount() > 0) {
            Process toActivate = getSuspendedProcessToActivate();
            if (toActivate == null) break;
            activateProcess(toActivate);
        }
    }
    
    private Process getSuspendedProcessToActivate() {
        try {
            readySuspendSemaphore.acquire();
            
            if (readySuspendedQueue.isEmpty()) {
                readySuspendSemaphore.release();
                return null;
            }
            
            Process best = null;
            int nearestDeadline = Integer.MAX_VALUE;
            
            for (int i = 0; i < readySuspendedQueue.size(); i++) {
                Process p = readySuspendedQueue.get(i);
                if (p.getRemainingDeadline() < nearestDeadline) {
                    nearestDeadline = p.getRemainingDeadline();
                    best = p;
                }
            }
            
            readySuspendSemaphore.release();
            return best;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    private void activateProcess(Process process) {
        try {
            // Intentar remover de ready suspended primero
            readySuspendSemaphore.acquire();
            boolean fromReady = readySuspendedQueue.remove(process);
            readySuspendSemaphore.release();
            
            if (fromReady) {
                process.setState(ProcessState.READY);
                ramSemaphore.acquire();
                processesInRAM.add(process);
                ramSemaphore.release();
                return;
            }
            
            // Si no estaba en ready, intentar blocked suspended
            blockedSuspendSemaphore.acquire();
            boolean fromBlocked = blockedSuspendedQueue.remove(process);
            blockedSuspendSemaphore.release();
            
            if (fromBlocked) {
                process.setState(ProcessState.BLOCKED);
                ramSemaphore.acquire();
                processesInRAM.add(process);
                ramSemaphore.release();
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // ========== MÉTODOS PARA SIMULATIONENGINE CON SEMÁFOROS ==========
    
    /**
     * Remueve proceso terminado y activa suspendidos si hay espacio.
     */
    public void processTerminated(Process process) {
        boolean operationAcquired = false;
        try {
            operationSemaphore.acquire();
            operationAcquired = true;
            
            // Intentar remover de RAM
            ramSemaphore.acquire();
            boolean wasInRAM = processesInRAM.remove(process);
            ramSemaphore.release();
            
            if (wasInRAM) {
                // Intentar activar suspendidos
                activateSuspendedProcessesUnderOperationLock();
            } else {
                // Remover de colas suspendidas
                readySuspendSemaphore.acquire();
                readySuspendedQueue.remove(process);
                readySuspendSemaphore.release();
                
                blockedSuspendSemaphore.acquire();
                blockedSuspendedQueue.remove(process);
                blockedSuspendSemaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (operationAcquired) {
                operationSemaphore.release();
            }
        }
    }
    
    /**
     * Proceso completó E/S y está listo.
     */
    public void processIOCompleted(Process process) {
        try {
            blockedSuspendSemaphore.acquire();
            boolean wasBlockedSuspended = blockedSuspendedQueue.remove(process);
            blockedSuspendSemaphore.release();
            
            if (wasBlockedSuspended) {
                process.setState(ProcessState.READY_SUSPENDED);
                
                readySuspendSemaphore.acquire();
                readySuspendedQueue.add(process);
                readySuspendSemaphore.release();
                
                // Intentar activar si hay espacio
                tryActivateSuspendedProcesses();
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Mueve un proceso bloqueado de RAM a BLOCKED_SUSPENDED para liberar memoria.
     * @return true si fue movido exitosamente.
     */
    public boolean moveBlockedProcessToSuspended(Process process) {
        if (process == null) return false;

        boolean operationAcquired = false;
        try {
            operationSemaphore.acquire();
            operationAcquired = true;

            if (process.getState() != ProcessState.BLOCKED) {
                return false;
            }

            ramSemaphore.acquire();
            boolean wasInRAM = processesInRAM.remove(process);
            ramSemaphore.release();

            if (!wasInRAM) {
                return false;
            }

            process.setState(ProcessState.BLOCKED_SUSPENDED);

            blockedSuspendSemaphore.acquire();
            blockedSuspendedQueue.add(process);
            blockedSuspendSemaphore.release();

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (operationAcquired) {
                operationSemaphore.release();
            }
        }
    }
    
    // ========== GETTERS SEGUROS CON SEMÁFOROS ==========
    
    public boolean hasSpaceInRAM() {
        try {
            ramSemaphore.acquire();
            boolean hasSpace = processesInRAM.size() < maxProcessesInRAM;
            ramSemaphore.release();
            return hasSpace;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public int getAvailableSpaceInRAM() {
        try {
            ramSemaphore.acquire();
            int available = maxProcessesInRAM - processesInRAM.size();
            ramSemaphore.release();
            return available;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }
    
    public int getRAMUsage() {
        try {
            ramSemaphore.acquire();
            int usage = processesInRAM.size();
            ramSemaphore.release();
            return usage;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }
    
    public int getMaxRAMCapacity() {
        return maxProcessesInRAM;
    }
    
    public int getReadySuspendedCount() {
        try {
            readySuspendSemaphore.acquire();
            int count = readySuspendedQueue.size();
            readySuspendSemaphore.release();
            return count;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }
    
    public int getBlockedSuspendedCount() {
        try {
            blockedSuspendSemaphore.acquire();
            int count = blockedSuspendedQueue.size();
            blockedSuspendSemaphore.release();
            return count;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }
    
    public LinkedList<Process> getProcessesInRAM() {
        try {
            ramSemaphore.acquire();
            LinkedList<Process> copy = new LinkedList<>();
            for (int i = 0; i < processesInRAM.size(); i++) {
                copy.add(processesInRAM.get(i));
            }
            ramSemaphore.release();
            return copy;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LinkedList<>();
        }
    }
    
    public LinkedList<Process> getReadySuspendedQueue() {
        try {
            readySuspendSemaphore.acquire();
            LinkedList<Process> copy = new LinkedList<>();
            for (int i = 0; i < readySuspendedQueue.size(); i++) {
                copy.add(readySuspendedQueue.get(i));
            }
            readySuspendSemaphore.release();
            return copy;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LinkedList<>();
        }
    }
    
    public LinkedList<Process> getBlockedSuspendedQueue() {
        try {
            blockedSuspendSemaphore.acquire();
            LinkedList<Process> copy = new LinkedList<>();
            for (int i = 0; i < blockedSuspendedQueue.size(); i++) {
                copy.add(blockedSuspendedQueue.get(i));
            }
            blockedSuspendSemaphore.release();
            return copy;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LinkedList<>();
        }
    }
    
    // ========== MÉTODO PARA ESTADO DEL MEMORY MANAGER ==========
    
    public String getStatus() {
        try {
            StringBuilder status = new StringBuilder();
            status.append("=== Memory Manager Status ===\n");
            
            ramSemaphore.acquire();
            status.append("RAM: ").append(processesInRAM.size())
                  .append("/").append(maxProcessesInRAM).append(" procesos\n");
            ramSemaphore.release();
            
            readySuspendSemaphore.acquire();
            status.append("Ready Suspended: ").append(readySuspendedQueue.size()).append("\n");
            readySuspendSemaphore.release();
            
            blockedSuspendSemaphore.acquire();
            status.append("Blocked Suspended: ").append(blockedSuspendedQueue.size()).append("\n");
            blockedSuspendSemaphore.release();
            
            status.append("Semáforos: RAM[").append(ramSemaphore.availablePermits())
                  .append("] ReadySuspend[").append(readySuspendSemaphore.availablePermits())
                  .append("] BlockedSuspend[").append(blockedSuspendSemaphore.availablePermits())
                  .append("]");
            
            return status.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "MemoryManager interrumpido";
        }
    }
}
