/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package rtos.simulation;

/**
 *
 * @author VictorB
 */


import rtos.Main;
import rtos.model.Process;
import rtos.model.ProcessState;
import rtos.scheduler.SchedulerManager;
import rtos.memory.MemoryManager;
import rtos.interrupt.InterruptHandler;
import rtos.interrupt.InterruptType;
import rtos.statistics.StatisticsTracker;
import rtos.structures.LinkedList;
import rtos.structures.Queue;

/**
 * COORDINADOR PURA - Solo delega, NO tiene lógica propia
 * Conecta todos los componentes según el PDF
 */
public class SimulationEngine {
    // ========== TODOS LOS COMPONENTES (solo referencias) ==========
    private final SchedulerManager scheduler;
    private final MemoryManager memory;
    private final InterruptHandler interrupts;
    private final StatisticsTracker statistics;
    private final ProcessGenerator generator;
    private final Clock globalClock;
    
    // ========== ESTADO (solo coordinación) ==========
    private Process currentProcess;      // Proceso en CPU (referencia)
    private boolean isRunning;
    private boolean isPaused;
    private int cycleDurationMs;
    
    // Colas (solo referencias a las de otros componentes)
    private LinkedList<Process> blockedQueue;
    
    public SimulationEngine() {
        // ========== OBTENER/INICIALIZAR COMPONENTES ==========
        this.scheduler = Main.getSchedulerManager();
        this.interrupts = Main.getInterruptHandler();
        
        // Crear componentes nuevos pero simples
        this.globalClock = new Clock();
        this.generator = new ProcessGenerator();
        this.statistics = new StatisticsTracker();
        this.memory = new MemoryManager(10); // 10 procesos máximo en RAM
        
        // Configurar componentes
        setupComponentConnections();
        
        // Estado inicial
        this.currentProcess = null;
        this.isRunning = false;
        this.isPaused = false;
        this.cycleDurationMs = 1000;
        this.blockedQueue = new LinkedList<>();
        
        System.out.println("✅ SimulationEngine COORDINADOR listo");
        System.out.println("   Delegando a: Scheduler, MemoryManager, InterruptHandler");
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
        // Generar 5 procesos iniciales (según PDF)
        for (int i = 0; i < 5; i++) {
            Process p = generator.generateRandomProcess();
            addProcessToSystem(p);
        }
        logEvent("🎲 5 procesos iniciales generados");
    }
    
    // ========== CICLO DE COORDINACIÓN ==========
    
    /**
     * Ejecuta UN ciclo de coordinación.
     * NO hace trabajo, solo DELEGA.
     */
    public void executeOneCycle() {
        if (!isRunning || isPaused) return;
        
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
    }
    
    // ========== MÉTODOS DE COORDINACIÓN (solo delegan) ==========
    
    private void checkForInterrupts() {
        // Delegar a InterruptHandler
        if (interrupts != null && interrupts.getPendingInterruptCount() > 0) {
            // Si hay interrupciones críticas, notificar
            // (La lógica real está en InterruptHandler)
        }
    }
    
    private void updateAllProcessDeadlines() {
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
        
        // 3. Proceso actual
        if (currentProcess != null) {
            currentProcess.updateDeadline();
        }
        
        // 4. Procesos suspendidos (delegar a MemoryManager)
        LinkedList<Process> suspended = memory.getReadySuspendedQueue();
        for (int i = 0; i < suspended.size(); i++) {
            suspended.get(i).updateDeadline();
        }
    }
    
    private void checkForDeadlineMisses() {
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
    }
    
    private void processCompletedIO() {
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
    }
    
    private void manageMemory() {
        // Delegar TODO a MemoryManager
        // 1. Si hay procesos suspendidos y espacio, activar
        memory.tryActivateSuspendedProcesses();
        
        // 2. Si un proceso terminó, MemoryManager ya lo maneja internamente
        // (porque llamamos memory.processTerminated())
    }
    
    private void executeCurrentProcess() {
        if (currentProcess == null) {
            statistics.recordIdleCycle();
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
            return;
        }
        
        // Verificar si inicia E/S
        if (currentProcess.isRequiresIO() && 
            currentProcess.getExecutedInstructions() == currentProcess.getIoStartCycle()) {
            
            logEvent("⏳ E/S iniciada: " + currentProcess.getId());
            currentProcess.setState(ProcessState.BLOCKED);
            blockedQueue.add(currentProcess);
            currentProcess = null;
        }
    }
    
    private void scheduleNextProcess() {
        if (currentProcess != null) return; // CPU ocupada
        
        // Delegar a Scheduler
        Process next = scheduler.getNextProcess();
        if (next != null) {
            currentProcess = next;
            currentProcess.setState(ProcessState.RUNNING);
            logEvent("⚡ Ejecutando: " + currentProcess.getId());
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
        // Ya se actualiza con recordInstructionExecution(), etc.
    }
    
    // ========== OPERACIONES DE PROCESOS (coordinación) ==========
    
    /**
     * Agrega proceso al sistema (coordina MemoryManager + Scheduler).
     */
    private void addProcessToSystem(Process process) {
        // Establecer tiempo de creación
        process.setCreationTime(globalClock.getCurrentCycle());
        
        // 1. Intentar agregar a RAM (delegar a MemoryManager)
        boolean addedToRAM = memory.addProcess(process);
        
        if (addedToRAM) {
            // 2. Si entró a RAM, agregar al scheduler
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
        process.finishProcess(globalClock.getCurrentCycle());
        process.setState(ProcessState.TERMINATED);
        
        // Delegar limpieza a componentes
        memory.processTerminated(process);
        statistics.recordProcessCompletion(process);
        
        logEvent("✅ Proceso terminado: " + process.getId());
        
        // Si es periódico, reiniciarlo
        if (process.getType() == rtos.model.ProcessType.PERIODIC) {
            restartPeriodicProcess(process);
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
    
    // ========== MANEJO DE INTERRUPCIONES (callback) ==========
    
    private void handleIncomingInterrupt(rtos.interrupt.InterruptRequest request) {
        logEvent("⚡ Interrupción recibida: " + request.getType());
        
        // Si es interrupción crítica, suspender proceso actual
        if (request.getPriority() >= 4 && currentProcess != null) {
            logEvent("🚨 Interrupción crítica - suspendiendo proceso actual");
            
            // Suspender proceso actual
            currentProcess.setState(ProcessState.READY);
            scheduler.addProcess(currentProcess);
            currentProcess = null;
        }
    }
    
    // ========== CONTROL PÚBLICO (solo coordinación) ==========
    
    public void start() {
        isRunning = true;
        isPaused = false;
        logEvent("🚀 Simulación iniciada");
    }
    
    public void pause() {
        isPaused = true;
        logEvent("⏸️ Simulación pausada");
    }
    
    public void resume() {
        isPaused = false;
        logEvent("▶️ Simulación reanudada");
    }
    
    public void stop() {
        isRunning = false;
        logEvent("⏹️ Simulación detenida");
    }
    
    public void generate20Processes() {
        for (int i = 0; i < 20; i++) {
            Process p = generator.generateRandomProcess();
            addProcessToSystem(p);
        }
        logEvent("🎲 20 procesos aleatorios generados");
    }
    
    public void addEmergencyProcess() {
        Process emergency = generator.generateEmergencyProcess();
        addProcessToSystem(emergency);
        logEvent("🚨 Proceso de emergencia añadido");
    }
    
    public void changeAlgorithm(String algorithm) {
        // Delegar a SchedulerManager
        scheduler.switchAlgorithm(rtos.scheduler.SchedulerManager.Algorithm.valueOf(algorithm));
        logEvent("🔀 Algoritmo cambiado a: " + algorithm);
    }
    
    // ========== GETTERS PARA GUI (solo devuelven referencias) ==========
    
    public boolean isRunning() { return isRunning; }
    public boolean isPaused() { return isPaused; }
    public int getCurrentCycle() { return globalClock.getCurrentCycle(); }
    public Process getCurrentProcess() { return currentProcess; }
    
    public LinkedList<Process> getReadyQueue() {
        return scheduler.getReadyQueue().toLinkedList();
    }
    
    public LinkedList<Process> getBlockedQueue() { return blockedQueue; }
    
    public LinkedList<Process> getReadySuspendedQueue() {
        return memory.getReadySuspendedQueue();
    }
    
    public LinkedList<Process> getBlockedSuspendedQueue() {
        return memory.getBlockedSuspendedQueue();
    }
    
    public StatisticsTracker getStatisticsTracker() { return statistics; }
    public MemoryManager getMemoryManager() { return memory; }
    public SchedulerManager getSchedulerManager() { return scheduler; }
    
    // ========== LOGGING ==========
    
    private void logEvent(String message) {
        String logMsg = "[Ciclo " + globalClock.getCurrentCycle() + "] " + message;
        System.out.println(logMsg);
        scheduler.logEvent(logMsg);
    }
}
