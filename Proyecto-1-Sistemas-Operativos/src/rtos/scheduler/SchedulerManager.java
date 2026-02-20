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
import rtos.structures.LinkedList;
import rtos.interrupt.InterruptType;
import rtos.interrupt.InterruptRequest;
import rtos.interrupt.InterruptHandler;
import rtos.statistics.StatisticsTracker;
import java.util.concurrent.Semaphore; 

public class SchedulerManager {
    private Scheduler currentScheduler;
    private FCFSScheduler fcfsScheduler;
    private RoundRobinScheduler rrScheduler;
    private SRTScheduler srtScheduler;
    private PriorityScheduler priorityScheduler;
    private EDFScheduler edfScheduler;
    private StatisticsTracker statistics;
    private Semaphore schedulerSemaphore;
    
    // Para manejo de logs
    private LinkedList<String> eventLogs;
    
    // ========== SEMÁFOROS PARA SINCRONIZACIÓN (java.util.concurrent) ==========
    private Semaphore readyQueueSemaphore;      // Para cola de listos
    private Semaphore blockedQueueSemaphore;    // Para cola de bloqueados
    private Semaphore suspendedQueueSemaphore;  // Para cola de suspendidos
    private Semaphore currentProcessSemaphore;  // Para proceso actual en ejecución
    private Semaphore logSemaphore;             // Para logs (escritura concurrente)
    private Semaphore interruptSemaphore;       // Para manejo de interrupciones
    private Algorithm algorithm;
    
    // ========== COLAS DE ESTADO ==========
    private LinkedList<Process> blockedQueue;
    private LinkedList<Process> suspendedQueue;
    private Process currentProcess;
    private int systemClock;
    
    // ========== ESTADÍSTICAS ==========
    private int processesCreated;
    private int processesCompleted;
    private int deadlineMisses;
    private int contextSwitches;
    
    // Interrupt Handler
    private InterruptHandler interruptHandler;
    
    public enum Algorithm {
        FCFS, ROUND_ROBIN, SRT, PRIORITY, EDF
    }
    
    public SchedulerManager(StatisticsTracker statistics) {
        // Crear todos los schedulers
        this.fcfsScheduler = new FCFSScheduler();
        this.rrScheduler = new RoundRobinScheduler(4); // Quantum por defecto: 4
        this.srtScheduler = new SRTScheduler();
        this.priorityScheduler = new PriorityScheduler();
        this.edfScheduler = new EDFScheduler();
        
        // Establecer FCFS como scheduler por defecto
        this.currentScheduler = fcfsScheduler;
        
        // Inicializar logs
        this.eventLogs = new LinkedList<>();
        
        // ========== INICIALIZAR SEMÁFOROS (java.util.concurrent) ==========
        this.readyQueueSemaphore = new Semaphore(1);     // Mutex para cola de listos
        this.blockedQueueSemaphore = new Semaphore(1);   // Mutex para cola de bloqueados
        this.suspendedQueueSemaphore = new Semaphore(1); // Mutex para cola de suspendidos
        this.currentProcessSemaphore = new Semaphore(1); // Mutex para proceso actual
        this.logSemaphore = new Semaphore(1);            // Mutex para logs
        this.interruptSemaphore = new Semaphore(1);      // Mutex para interrupciones
        this.schedulerSemaphore = new Semaphore(1);      // Mutex para scheduler
        
        this.statistics = statistics;
        
        // ========== INICIALIZAR COLAS ==========
        this.blockedQueue = new LinkedList<>();
        this.suspendedQueue = new LinkedList<>();
        this.currentProcess = null;
        this.systemClock = 0;
        
        // ========== INICIALIZAR ESTADÍSTICAS ==========
        this.processesCreated = 0;
        this.processesCompleted = 0;
        this.deadlineMisses = 0;
        this.contextSwitches = 0;
        
        // ========== INICIALIZAR INTERRUPT HANDLER ==========
        this.interruptHandler = new InterruptHandler(this);
        
        addLogEntry("SchedulerManager inicializado con semáforos de java.util.concurrent");
    }
    
    /**
     * Constructor por defecto (para compatibilidad)
     */
    public SchedulerManager() {
        this(new StatisticsTracker()); // Llama al otro constructor
    }

    // ========== MÉTODOS SINCRONIZADOS CON SEMÁFOROS ==========
    
    /**
     * Agrega un proceso al sistema de manera segura (thread-safe)
     */
    public void addProcess(Process process) {
        try {
            readyQueueSemaphore.acquire();
            currentScheduler.addProcess(process);
            process.setCreationTime(systemClock);
            processesCreated++;
            readyQueueSemaphore.release();
            
            addLogEntry("Proceso añadido: " + process.getId() + " - " + process.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            addLogEntry("ERROR: Interrupción al añadir proceso " + process.getId());
        }
    }
    
    /**
     * Determina si el proceso actual debe ser preemptado
     */
    public boolean shouldPreempt(Process current) {
        if (current == null) return false;

        // Obtener el siguiente proceso que está esperando en el scheduler actual
        Process next = this.currentScheduler.getReadyQueue().peek(); 

        if (next != null) {
            // Regla de prioridad: 1 es la más alta, 5 la más baja.
            // Si el que espera (next) tiene un número menor, es más prioritario.
            if (next.getPriority() < current.getPriority()) {
                return true; // ¡Sí! El proceso actual debe ser expulsado
            }
        }
        return false;
    }
    
    /**
     * Obtiene el próximo proceso a ejecutar de manera segura
     */
    public Process getNextProcess() {
        try {
            readyQueueSemaphore.acquire();
            currentProcessSemaphore.acquire();
            
            Process nextProcess = currentScheduler.getNextProcess();
            
            if (nextProcess != null) {
                if (currentProcess != null && currentProcess.getState() == ProcessState.RUNNING) {
                    // Hacer cambio de contexto
                    performContextSwitch(currentProcess, nextProcess);
                }
                
                currentProcess = nextProcess;
                contextSwitches++;
                addLogEntry("Cambio de contexto a: " + currentProcess.getId());
            }
            
            currentProcessSemaphore.release();
            readyQueueSemaphore.release();
            
            return nextProcess;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    /**
     * Ejecuta un ciclo del proceso actual
     */
    public void executeCurrentCycle() {
        try {
            currentProcessSemaphore.acquire();
            
            if (currentProcess != null && currentProcess.getState() == ProcessState.RUNNING) {
                boolean finished = currentProcess.executeInstruction();
                
                // Verificar deadlines
                currentProcess.updateDeadline();
                if (currentProcess.getRemainingDeadline() <= 0 && !currentProcess.isFinished()) {
                    handleDeadlineMiss(currentProcess);
                }
                
                if (finished) {
                    finishProcess(currentProcess);
                    currentProcess = null;
                }
            }
            
            currentProcessSemaphore.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Mueve un proceso a la cola de bloqueados (E/S)
     */
    public void blockProcessForIO(Process process, int ioDuration) {
        try {
            currentProcessSemaphore.acquire();
            blockedQueueSemaphore.acquire();
            
            process.setState(ProcessState.BLOCKED);
            process.setIoStartCycle(systemClock);
            process.setIoDuration(ioDuration);
            process.setIoCompletionTime(systemClock + ioDuration);
            process.setRequiresIO(true);
            
            blockedQueue.add(process);
            
            currentProcessSemaphore.release();
            blockedQueueSemaphore.release();
            
            addLogEntry("Proceso bloqueado por E/S: " + process.getId() + " (Duración: " + ioDuration + ")");
            
            // Generar interrupción de E/S
            interruptHandler.raiseInterrupt(InterruptType.IO_COMPLETION, 1, "IO_Device");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Verifica y reactiva procesos bloqueados que completaron E/S
     */
    public void checkBlockedProcesses() {
        try {
            blockedQueueSemaphore.acquire();
            readyQueueSemaphore.acquire();
            
            LinkedList<Process> completedIO = new LinkedList<>();
            
            for (int i = 0; i < blockedQueue.size(); i++) {
                Process p = blockedQueue.get(i);
                if (p.isIOCompleted(systemClock)) {
                    p.completeIO();
                    completedIO.add(p);
                }
            }
            
            // Mover procesos completados de vuelta a la cola de listos
            for (int i = 0; i < completedIO.size(); i++) {
                Process p = completedIO.get(i);
                blockedQueue.remove(p);
                currentScheduler.addProcess(p);
                addLogEntry("E/S completada para: " + p.getId());
            }
            
            readyQueueSemaphore.release();
            blockedQueueSemaphore.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Suspende un proceso cuando la memoria está llena
     */
    public void suspendProcess(Process process) {
        try {
            suspendedQueueSemaphore.acquire();
            
            if (process.getState() == ProcessState.READY) {
                process.setState(ProcessState.READY_SUSPENDED);
            } else if (process.getState() == ProcessState.BLOCKED) {
                process.setState(ProcessState.BLOCKED_SUSPENDED);
            }
            
            suspendedQueue.add(process);
            
            suspendedQueueSemaphore.release();
            
            addLogEntry("Proceso suspendido: " + process.getId() + " (Memoria llena)");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Reactiva un proceso suspendido
     */
    public boolean activateProcess(Process process) {
        try {
            suspendedQueueSemaphore.acquire();
            readyQueueSemaphore.acquire();

            if (suspendedQueue.contains(process)) {
                suspendedQueue.remove(process);

                if (process.getState() == ProcessState.READY_SUSPENDED) {
                    process.setState(ProcessState.READY);
                } else if (process.getState() == ProcessState.BLOCKED_SUSPENDED) {
                    process.setState(ProcessState.BLOCKED);
                }

                currentScheduler.addProcess(process);

                suspendedQueueSemaphore.release();
                readyQueueSemaphore.release();

                addLogEntry("Proceso reactivado: " + process.getId());
                return true;
            }

            suspendedQueueSemaphore.release();
            readyQueueSemaphore.release();

            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Obtiene el nombre del algoritmo actual
     */
    public String getCurrentAlgorithmName() {
        return currentScheduler.getName();
    }
    
    /**
     * Cambia el algoritmo de planificación de manera segura
     */
    public void switchAlgorithm(Algorithm algorithm) {
        try {
            // Adquirir todos los semáforos necesarios para una transición segura
            readyQueueSemaphore.acquire();
            currentProcessSemaphore.acquire();

            System.out.println("Cambiando algoritmo a: " + algorithm);
            addLogEntry("Cambio de algoritmo a: " + algorithm);

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

            currentProcessSemaphore.release();
            readyQueueSemaphore.release();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            addLogEntry("ERROR: Interrupción durante cambio de algoritmo");
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
    
    // ========== NUEVOS MÉTODOS PARA INTERRUPCIONES ==========
    
    /**
     * Maneja una interrupción de emergencia (MICROMETEORITE, SYSTEM_ERROR)
     */
    public void handleEmergency() {
        String message = "🚨 EMERGENCIA: Activando protocolos de seguridad";
        System.out.println(message);
        addLogEntry(message);
        
        // En una implementación real: suspender procesos no críticos
        // Priorizar procesos de sistema
        // Cambiar posiblemente a EDF para manejar deadlines críticos
    }

    /**
     * Registra un evento en el log del sistema
     */
    public void logEvent(String message) {
        addLogEntry(message);
    }
    
    /**
     * Maneja un deadline incumplido
     */
    public void handleDeadlineMissed() {
        String message = "⏰ Deadline Incumplido: Replanificando tareas";
        System.out.println(message);
        addLogEntry(message);
        
        // Sugerencia: Cambiar a EDF si no está ya activo
        if (!(currentScheduler instanceof EDFScheduler)) {
            System.out.println("   -> Cambiando a EDF para mejor manejo de deadlines");
            switchAlgorithm(Algorithm.EDF);
        }
    }

    /**
     * Notifica la finalización de una operación de E/S
     */
    public void notifyIOCompletion() {
        String message = "✅ E/S Completada: Revisando procesos bloqueados";
        System.out.println(message);
        addLogEntry(message);
        
        // En una implementación real: mover procesos de BLOCKED a READY
        // queueBlockedToReady();
    }

    /**
     * Maneja un error del sistema
     */
    public void handleSystemError() {
        String message = "❌ Error del Sistema: Iniciando diagnóstico";
        System.out.println(message);
        addLogEntry(message);
        
    }
    
    /**
     * Incrementa el reloj del sistema y actualiza estados
     */
    public void tick() {
        systemClock++;
        
        // Actualizar deadlines de todos los procesos
        updateAllDeadlines();
        
        // Verificar procesos bloqueados
        checkBlockedProcesses();
        
        // Verificar memoria y suspender procesos si es necesario
        manageMemory();
        
        // Generar interrupciones aleatorias (para simulación)
        if (systemClock % 50 == 0) { // Cada 50 ciclos
            generateRandomInterrupt();
        }
    }
    
    // ========== MÉTODOS DE APOYO ==========
    
    private void performContextSwitch(Process oldProcess, Process newProcess) {
        if (oldProcess != null) {
            oldProcess.setState(ProcessState.READY);
            currentScheduler.addProcess(oldProcess);
        }
        
        if (newProcess.getStartTime() == -1) {
            newProcess.setStartTime(systemClock);
        }
    }
    
    private void finishProcess(Process process) {
        process.finishProcess(systemClock);
        process.setState(ProcessState.TERMINATED);
        processesCompleted++;
        
        if (process.isDeadlineMissed()) {
            deadlineMisses++;
            interruptHandler.raiseInterrupt(InterruptType.DEADLINE_MISSED, 2, "Scheduler");
        }
        
        addLogEntry("Proceso terminado: " + process.getId() + 
                   " - Turnaround: " + process.getTurnaroundTime());
    }
    
    private void handleDeadlineMiss(Process process) {
        process.setDeadlineMissed(true);
        deadlineMisses++;
        
        addLogEntry("⚠️ Deadline incumplido: " + process.getId());
        
        // Generar interrupción de deadline missed
        interruptHandler.raiseInterrupt(InterruptType.DEADLINE_MISSED, 2, "Scheduler");
    }
    
    private void updateAllDeadlines() {
        try {
            readyQueueSemaphore.acquire();
            
            // Actualizar deadlines en cola de listos
            Queue<Process> tempQueue = new Queue<>();
            while (!currentScheduler.isEmpty()) {
                Process p = currentScheduler.getNextProcess();
                if (p != null) {
                    p.updateDeadline();
                    tempQueue.enqueue(p);
                }
            }
            
            // Restaurar cola
            while (!tempQueue.isEmpty()) {
                currentScheduler.addProcess(tempQueue.dequeue());
            }
            
            readyQueueSemaphore.release();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void manageMemory() {
        // Lógica simple de gestión de memoria
        // Si hay más de 8 procesos en cola de listos, suspender algunos
        try {
            readyQueueSemaphore.acquire();
            
            if (currentScheduler.getReadyQueue().size() > 8) {
                // Suspender proceso con deadline más lejano
                Process toSuspend = findProcessToSuspend();
                if (toSuspend != null) {
                    suspendProcess(toSuspend);
                }
            }
            
            readyQueueSemaphore.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private Process findProcessToSuspend() {
        // Encuentra proceso con deadline más lejano (menos urgente)
        Process candidate = null;
        int maxDeadline = Integer.MIN_VALUE;
        
        Queue<Process> tempQueue = new Queue<>();
        
        try {
            readyQueueSemaphore.acquire();
            
            while (!currentScheduler.isEmpty()) {
                Process p = currentScheduler.getNextProcess();
                if (p != null) {
                    if (p.getRemainingDeadline() > maxDeadline) {
                        maxDeadline = p.getRemainingDeadline();
                        candidate = p;
                    }
                    tempQueue.enqueue(p);
                }
            }
            
            // Restaurar cola
            while (!tempQueue.isEmpty()) {
                currentScheduler.addProcess(tempQueue.dequeue());
            }
            
            readyQueueSemaphore.release();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return candidate;
    }
    
    // ========== MÉTODOS PARA INTERRUPCIONES ==========
    
    public void generateRandomInterrupt() {
        interruptHandler.generateRandomInterrupt();
    }
    
    public void generateInterrupt(InterruptType type, int priority, String source) {
        interruptHandler.raiseInterrupt(type, priority, source);
    }
    
    // ========== GETTERS SEGUROS ==========
    
    public Queue<Process> getReadyQueue() {
        try {
            readyQueueSemaphore.acquire();
            Queue<Process> queue = currentScheduler.getReadyQueue();
            readyQueueSemaphore.release();
            return queue;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Queue<>();
        }
    }
    
    public LinkedList<Process> getBlockedQueue() {
        try {
            blockedQueueSemaphore.acquire();
            LinkedList<Process> copy = new LinkedList<>();
            for (int i = 0; i < blockedQueue.size(); i++) {
                copy.add(blockedQueue.get(i));
            }
            blockedQueueSemaphore.release();
            return copy;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LinkedList<>();
        }
    }
    
    public LinkedList<Process> getSuspendedQueue() {
        try {
            suspendedQueueSemaphore.acquire();
            LinkedList<Process> copy = new LinkedList<>();
            for (int i = 0; i < suspendedQueue.size(); i++) {
                copy.add(suspendedQueue.get(i));
            }
            suspendedQueueSemaphore.release();
            return copy;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LinkedList<>();
        }
    }
    
    public Process getCurrentProcess() {
        try {
            currentProcessSemaphore.acquire();
            Process cp = currentProcess;
            currentProcessSemaphore.release();
            return cp;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    public int getSystemClock() {
        return systemClock;
    }
    
    // ========== MÉTODOS DE LOG ==========
    
    private void addLogEntry(String message) {
        try {
            logSemaphore.acquire();
            String timestamp = String.format("[%d]", systemClock);
            String logEntry = timestamp + " " + message;
            eventLogs.add(logEntry);
            
            // Mantener solo los últimos 100 logs
            if (eventLogs.size() > 100) {
                eventLogs.remove(0);
            }
            
            logSemaphore.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public LinkedList<String> getEventLogs() {
        try {
            logSemaphore.acquire();
            LinkedList<String> copy = new LinkedList<>();
            for (int i = 0; i < eventLogs.size(); i++) {
                copy.add(eventLogs.get(i));
            }
            logSemaphore.release();
            return copy;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LinkedList<>();
        }
    }   
    /**
     * Obtiene los últimos N logs
     */
    public LinkedList<String> getRecentLogs(int count) {
        LinkedList<String> recent = new LinkedList<>();
        int start = Math.max(0, eventLogs.size() - count);
        
        for (int i = start; i < eventLogs.size(); i++) {
            recent.add(eventLogs.get(i));
        }
        
        return recent;
    }
    
    // ========== ESTADÍSTICAS ==========
    
    public int getProcessesCreated() { return processesCreated; }
    public int getProcessesCompleted() { return processesCompleted; }
    public int getDeadlineMisses() { return deadlineMisses; }
    public int getContextSwitches() { return contextSwitches; }
    
    public double getSuccessRate() {
        if (processesCompleted == 0) return 100.0;
        double missedRate = (double) deadlineMisses / processesCompleted * 100;
        return 100.0 - missedRate;
    }
    
    public double getThroughput() {
        if (systemClock == 0) return 0.0;
        return (double) processesCompleted / systemClock;
    }
    
    public double getCPUUtilization() {
        if (systemClock == 0) return 0.0;
        // En una implementación real, llevarías registro del tiempo de CPU usado
        return 85.0; // Valor de ejemplo
    }
}

