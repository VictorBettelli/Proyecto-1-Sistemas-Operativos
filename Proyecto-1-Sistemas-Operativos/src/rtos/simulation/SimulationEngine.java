/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package rtos.simulation;


/**
 *
 * @author VictorB
 */

import rtos.model.Process;
import rtos.model.ProcessState;
import rtos.model.ProcessType;
import rtos.scheduler.SchedulerManager;
import rtos.memory.MemoryManager;
import rtos.interrupt.InterruptHandler;
import rtos.interrupt.InterruptType;
import rtos.statistics.StatisticsTracker;
import rtos.structures.LinkedList;
import rtos.structures.Queue;
import rtos.utils.Semaphore;

/**
 * COORDINADOR PURA - Solo delega, NO tiene lógica propia
 * Conecta todos los componentes según el PDF
 * CON SEMÁFOROS para exclusión mutua
 */
public class SimulationEngine {
    // ========== TODOS LOS COMPONENTES (solo referencias) ==========
    private final SchedulerManager scheduler;
    private final MemoryManager memory;
    private final InterruptHandler interrupts;
    private final StatisticsTracker statistics;
    private final ProcessGenerator generator;
    private final Clock globalClock;
    
    // ========== SEMÁFOROS para exclusión mutua (REQUERIMIENTO PDF) ==========
    private final Semaphore executionSemaphore;    // Protege executeOneCycle()
    private final Semaphore processSemaphore;      // Protege currentProcess
    private final Semaphore queueSemaphore;        // Protege blockedQueue
    private final Semaphore interruptSemaphore;    // Protege manejo de interrupciones
    
    // ========== ESTADO (solo coordinación) ==========
    private Process currentProcess;      // Proceso en CPU (referencia)
    private boolean isRunning;
    private boolean isPaused;
    private int cycleDurationMs;
    
    // Colas (solo referencias a las de otros componentes)
    private LinkedList<Process> blockedQueue;
    
    public SimulationEngine() {
        // ========== OBTENER/INICIALIZAR COMPONENTES ==========
        this.scheduler = new SchedulerManager();
        this.interrupts = new InterruptHandler(scheduler);
        
        // Crear componentes nuevos pero simples
        this.globalClock = new Clock();
        this.generator = new ProcessGenerator();
        this.statistics = new StatisticsTracker();
        this.memory = new MemoryManager(10); // 10 procesos máximo en RAM
        
        // ========== INICIALIZAR SEMÁFOROS (REQUERIMIENTO PDF) ==========
        this.executionSemaphore = new Semaphore(1);  // Mutex para ciclo de ejecución
        this.processSemaphore = new Semaphore(1);    // Mutex para proceso actual
        this.queueSemaphore = new Semaphore(1);      // Mutex para colas
        this.interruptSemaphore = new Semaphore(1);  // Mutex para interrupciones
        
        // Configurar componentes
        setupComponentConnections();
        
        // Estado inicial
        this.currentProcess = null;
        this.isRunning = false;
        this.isPaused = false;
        this.cycleDurationMs = 1000;
        this.blockedQueue = new LinkedList<>();
        
        System.out.println("✅ SimulationEngine COORDINADOR listo con semáforos");
        System.out.println("   Delegando a: Scheduler, MemoryManager, InterruptHandler");
        System.out.println("   Semáforos: execution, process, queue, interrupt");
    }
    
    /**
     * Conecta componentes entre sí.
     */
    private void setupComponentConnections() {
        // Configurar callback de interrupciones
        if (interrupts != null) {
            interrupts.registerInterruptCallback(this::handleIncomingInterrupt);
        }
        
        // Inicializar con algunos procesos
        initializeWithSampleProcesses();
    }
    
    /**
     * Inicializa con procesos de ejemplo.
     */
    private void initializeWithSampleProcesses() {
        try {
            queueSemaphore.acquire();
            // Generar 5 procesos iniciales (según PDF)
            for (int i = 0; i < 5; i++) {
                Process p = generator.generateRandomProcess();
                addProcessToSystem(p);
            }
            queueSemaphore.release();
            logEvent("🎲 5 procesos iniciales generados");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("❌ Error inicializando procesos: " + e.getMessage());
        }
    }
    /**
     * Ejecuta UN ciclo de coordinación.
     * CON SEMÁFOROS para protección de recursos.
     */
    public void executeOneCycle() {
        if (!isRunning || isPaused) return;
        
        try {
            // SEMÁFORO: Proteger ciclo completo de ejecución
            executionSemaphore.acquire();
            
            // 1. Avanzar reloj
            globalClock.tick();
            statistics.setCurrentCycle(globalClock.getCurrentCycle());
            
            // 2. Verificar interrupciones (delegar a InterruptHandler)
            checkForInterrupts();
            
            // 3. Actualizar deadlines de todos los procesos
            updateAllProcessDeadlines();
            
            // 4. Verificar deadlines incumplidos
            checkForDeadlineMisses();
            
            // 5. Procesar E/S completadas
            processCompletedIO();
            
            // 6. Manejar memoria (delegar a MemoryManager)
            manageMemory();
            
            // 7. Ejecutar proceso actual (si hay)
            executeCurrentProcess();
            
            // 8. Planificar próximo proceso (delegar a Scheduler)
            scheduleNextProcess();
            
            // 9. Generar eventos aleatorios
            generateRandomEvents();
            
            // 10. Actualizar estadísticas (delegar a StatisticsTracker)
            updateStatistics();
            
            executionSemaphore.release();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("❌ Interrupción en ciclo de simulación");
        }
    }   
    // ========== MÉTODOS DE COORDINACIÓN CON SEMÁFOROS ==========
    
    private void checkForInterrupts() {
        try {
            interruptSemaphore.acquire();
            // Delegar a InterruptHandler
            if (interrupts != null && interrupts.getPendingInterruptCount() > 0) {
                // Si hay interrupciones críticas, notificar
                logEvent("⚠️ Interrupciones pendientes: " + 
                        interrupts.getPendingInterruptCount());
            }
            interruptSemaphore.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void updateAllProcessDeadlines() {
        try {
            queueSemaphore.acquire();
            
            // 1. Procesos en scheduler
            Queue<Process> readyQueue = scheduler.getReadyQueue();
            LinkedList<Process> readyList = readyQueue.toLinkedList();
            for (int i = 0; i < readyList.size(); i++) {
                readyList.get(i).updateDeadline();
            }
            
            // 2. Procesos bloqueados
            for (int i = 0; i < blockedQueue.size(); i++) {
                blockedQueue.get(i).updateDeadline();
            }
            
            queueSemaphore.release();
            
            // 3. Proceso actual (con su propio semáforo)
            processSemaphore.acquire();
            if (currentProcess != null) {
                currentProcess.updateDeadline();
            }
            processSemaphore.release();
            
            // 4. Procesos suspendidos (delegar a MemoryManager)
            // MemoryManager maneja su propio semáforo internamente
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void checkForDeadlineMisses() {
        try {
            processSemaphore.acquire();
            // Verificar proceso actual
            if (currentProcess != null && 
                currentProcess.getRemainingDeadline() <= 0 && 
                !currentProcess.isDeadlineMissed()) {
                
                currentProcess.setDeadlineMissed(true);
                logEvent("⏰ Deadline incumplido: " + currentProcess.getId());
                
                // Generar interrupción (delegar a InterruptHandler)
                if (interrupts != null) {
                    interrupts.raiseInterrupt(InterruptType.DEADLINE_MISSED, 3, 
                                             "Proceso " + currentProcess.getId());
                }
            }
            processSemaphore.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void processCompletedIO() {
        try {
            queueSemaphore.acquire();
            
            // Verificar procesos bloqueados que completaron E/S
            LinkedList<Process> completed = new LinkedList<>();
            
            for (int i = 0; i < blockedQueue.size(); i++) {
                Process p = blockedQueue.get(i);
                if (p.isIOCompleted(globalClock.getCurrentCycle())) {
                    p.completeIO();
                    completed.add(p);
                    logEvent("✅ E/S completada: " + p.getId());
                }
            }
            
            // Mover de vuelta al sistema
            for (int i = 0; i < completed.size(); i++) {
                Process p = completed.get(i);
                blockedQueue.remove(p);
                addProcessToSystem(p); // Delegar a MemoryManager + Scheduler
            }
            
            queueSemaphore.release();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void manageMemory() {
        // Delegar TODO a MemoryManager
        // MemoryManager debe manejar sus propios semáforos internamente
        // 1. Si hay procesos suspendidos y espacio, activar
        memory.tryActivateSuspendedProcesses();
    }
    
    private void executeCurrentProcess() {
        try {
            processSemaphore.acquire();
            
            if (currentProcess == null) {
                statistics.recordIdleCycle();
                processSemaphore.release();
                return;
            }
            
            // Marcar inicio si es primera vez
            if (currentProcess.getStartTime() < 0) {
                currentProcess.setStartTime(globalClock.getCurrentCycle());
            }
            
            // Ejecutar instrucción (PC++, MAR++)
            boolean finished = currentProcess.executeInstruction();
            statistics.recordInstructionExecution(1);
            
            if (finished) {
                // Proceso terminó - delegar limpieza
                finishProcess(currentProcess);
                currentProcess = null;
                processSemaphore.release();
                return;
            }
            
            // Verificar si inicia E/S
            if (currentProcess.isRequiresIO() && 
                currentProcess.getExecutedInstructions() == currentProcess.getIoStartCycle()) {
                
                logEvent("⏳ E/S iniciada: " + currentProcess.getId());
                currentProcess.setState(ProcessState.BLOCKED);
                
                try {
                    queueSemaphore.acquire();
                    blockedQueue.add(currentProcess);
                    queueSemaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                currentProcess = null;
            }
            
            processSemaphore.release();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void scheduleNextProcess() {
        try {
            processSemaphore.acquire();
            
            if (currentProcess != null) {
                processSemaphore.release();
                return; // CPU ocupada
            }
            
            // Delegar a Scheduler (Scheduler maneja su propio semáforo)
            Process next = scheduler.getNextProcess();
            if (next != null) {
                currentProcess = next;
                currentProcess.setState(ProcessState.RUNNING);
                logEvent("⚡ Ejecutando: " + currentProcess.getId());
            }
            
            processSemaphore.release();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void generateRandomEvents() {
        // 5% chance de interrupción aleatoria
        if (Math.random() < 0.05 && interrupts != null) {
            interrupts.generateRandomInterrupt();
        }
        
        // 10% chance de proceso aperiódico
        if (Math.random() < 0.10) {
            Process p = generator.generateRandomProcess();
            addProcessToSystem(p);
            logEvent("🎲 Proceso aleatorio generado: " + p.getId());
        }
    }
    
    private void updateStatistics() {
        // Delegar todo a StatisticsTracker
        // StatisticsTracker debe manejar sus propios semáforos internamente
    }
    
    // ========== OPERACIONES DE PROCESOS CON SEMÁFOROS ==========
    
    /**
     * Agrega proceso al sistema (coordina MemoryManager + Scheduler).
     * CON SEMÁFOROS para exclusión mutua.
     */
    private void addProcessToSystem(Process process) {
        // Establecer tiempo de creación
        process.setCreationTime(globalClock.getCurrentCycle());
        
        // 1. Intentar agregar a RAM (delegar a MemoryManager)
        // MemoryManager maneja sus propios semáforos internamente
        boolean addedToRAM = memory.addProcess(process);
        
        if (addedToRAM) {
            // 2. Si entró a RAM, agregar al scheduler
            // SchedulerManager maneja sus propios semáforos internamente
            scheduler.addProcess(process);
            statistics.recordProcessCreation(process);
            logEvent("➕ Proceso agregado: " + process.getId());
        } else {
            // 3. Si no entró a RAM, ya está suspendido por MemoryManager
            logEvent("⏸️ Proceso suspendido al crear: " + process.getId());
        }
    }
    
    /**
     * Finaliza proceso (coordina limpieza).
     */
    private void finishProcess(Process process) {
        try {
            processSemaphore.acquire();
            
            process.finishProcess(globalClock.getCurrentCycle());
            process.setState(ProcessState.TERMINATED);
            
            // Delegar limpieza a componentes (cada uno maneja sus semáforos)
            memory.processTerminated(process);
            statistics.recordProcessCompletion(process);
            
            logEvent("✅ Proceso terminado: " + process.getId());
            
            // Si es periódico, reiniciarlo
            if (process.getType() == ProcessType.PERIODIC) {
                restartPeriodicProcess(process);
            }
            
            processSemaphore.release();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void restartPeriodicProcess(Process process) {
        // Crear nueva instancia del proceso periódico
        Process newProcess = generator.generatePeriodicProcess(
            process.getId() + "-R",
            process.getName(),
            globalClock.getCurrentCycle()
        );
        
        addProcessToSystem(newProcess);
        logEvent("🔄 Periódico reiniciado: " + process.getId());
    }
    
    // ========== MANEJO DE INTERRUPCIONES CON SEMÁFOROS ==========
    
    private void handleIncomingInterrupt(rtos.interrupt.InterruptRequest request) {
        try {
            interruptSemaphore.acquire();
            
            logEvent("⚡ Interrupción recibida: " + request.getType());
            
            // Si es interrupción crítica, suspender proceso actual
            if (request.getPriority() >= 4) {
                try {
                    processSemaphore.acquire();
                    
                    if (currentProcess != null) {
                        logEvent("🚨 Interrupción crítica - suspendiendo proceso actual");
                        
                        // Suspender proceso actual
                        currentProcess.setState(ProcessState.READY);
                        scheduler.addProcess(currentProcess);
                        currentProcess = null;
                    }
                    
                    processSemaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            interruptSemaphore.release();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // ========== CONTROL PÚBLICO CON SEMÁFOROS ==========
    
    public void start() {
        try {
            executionSemaphore.acquire();
            isRunning = true;
            isPaused = false;
            executionSemaphore.release();
            logEvent("🚀 Simulación iniciada");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void pause() {
        try {
            executionSemaphore.acquire();
            isPaused = true;
            executionSemaphore.release();
            logEvent("⏸️ Simulación pausada");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void resume() {
        try {
            executionSemaphore.acquire();
            isPaused = false;
            executionSemaphore.release();
            logEvent("▶️ Simulación reanudada");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void stop() {
        try {
            executionSemaphore.acquire();
            isRunning = false;
            executionSemaphore.release();
            logEvent("⏹️ Simulación detenida");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void generate20Processes() {
        try {
            queueSemaphore.acquire();
            for (int i = 0; i < 20; i++) {
                Process p = generator.generateRandomProcess();
                addProcessToSystem(p);
            }
            queueSemaphore.release();
            logEvent("🎲 20 procesos aleatorios generados");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void addEmergencyProcess() {
        try {
            queueSemaphore.acquire();
            Process emergency = generator.generateEmergencyProcess();
            addProcessToSystem(emergency);
            queueSemaphore.release();
            logEvent("🚨 Proceso de emergencia añadido");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void changeAlgorithm(String algorithm) {
        // Delegar a SchedulerManager (maneja sus propios semáforos)
        try {
            rtos.scheduler.SchedulerManager.Algorithm algo = 
                rtos.scheduler.SchedulerManager.Algorithm.valueOf(algorithm);
            scheduler.switchAlgorithm(algo);
            logEvent("🔀 Algoritmo cambiado a: " + algorithm);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Algoritmo no válido: " + algorithm);
        }
    }
    
    // ========== GETTERS SEGUROS CON SEMÁFOROS ==========
    
    public boolean isRunning() { 
        return isRunning; 
    }
    
    public boolean isPaused() { 
        return isPaused; 
    }
    
    public int getCurrentCycle() { 
        return globalClock.getCurrentCycle(); 
    }
    
    public Process getCurrentProcess() { 
        try {
            processSemaphore.acquire();
            Process temp = currentProcess;
            processSemaphore.release();
            return temp;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    public LinkedList<Process> getReadyQueue() {
        return scheduler.getReadyQueue().toLinkedList();
    }
    
    public LinkedList<Process> getBlockedQueue() { 
        try {
            queueSemaphore.acquire();
            LinkedList<Process> copy = new LinkedList<>();
            for (int i = 0; i < blockedQueue.size(); i++) {
                copy.add(blockedQueue.get(i));
            }
            queueSemaphore.release();
            return copy;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LinkedList<>();
        }
    }
    
    public LinkedList<Process> getReadySuspendedQueue() {
        return memory.getReadySuspendedQueue();
    }
    
    public LinkedList<Process> getBlockedSuspendedQueue() {
        return memory.getBlockedSuspendedQueue();
    }
    
    public StatisticsTracker getStatisticsTracker() { 
        return statistics; 
    }
    
    public MemoryManager getMemoryManager() { 
        return memory; 
    }
    
    public SchedulerManager getSchedulerManager() { 
        return scheduler; 
    }
    
    public InterruptHandler getInterruptHandler() {
        return interrupts;
    }
    
    // ========== LOGGING ==========
    
    private void logEvent(String message) {
        String logMsg = "[Ciclo " + globalClock.getCurrentCycle() + "] " + message;
        System.out.println(logMsg);
        scheduler.logEvent(logMsg);
    }
}