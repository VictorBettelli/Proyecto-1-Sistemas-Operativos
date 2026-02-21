/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package rtos.simulation;


/**
 *
 * @author VictorB
 */
import java.util.Random;
import java.util.concurrent.Semaphore; // ← CAMBIO IMPORTANTE
import rtos.interrupt.InterruptHandler;
import rtos.interrupt.InterruptType;
import rtos.memory.MemoryManager;
import rtos.model.Process;
import rtos.model.ProcessState;
import rtos.model.ProcessType;
import rtos.scheduler.SchedulerManager;
import rtos.statistics.StatisticsTracker;
import rtos.structures.LinkedList;
import rtos.structures.Queue;
// import rtos.utils.Semaphore; ← ELIMINAR ESTA LÍNEA

/**
 * COORDINADOR PURA - Solo delega, NO tiene lógica propia
 * Conecta todos los componentes según el PDF
 * CON SEMÁFOROS de java.util.concurrent para exclusión mutua
 */
public class SimulationEngine {
    public enum CpuMode {
        IDLE,
        USER,
        KERNEL
    }

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
    private volatile CpuMode cpuMode;
    private volatile boolean kernelActivityThisCycle;
    
    // Colas (solo referencias a las de otros componentes)
    private LinkedList<Process> blockedQueue;
    
    // Callback para estadísticas
    private StatsCallback statsCallback;
    
    public SimulationEngine() {
        // =========== CREAR COMPONENTES ==========
        this.globalClock = new Clock();
        this.generator = new ProcessGenerator();
        this.statistics = new StatisticsTracker();
        this.memory = new MemoryManager(10); // 10 procesos máximo en RAM
        this.scheduler = new SchedulerManager(statistics);
        this.interrupts = new InterruptHandler(scheduler);
        
        // =========== INICIALIZAR SEMÁFOROS (java.util.concurrent) ==========
        this.executionSemaphore = new Semaphore(1);  // Semáforo binario
        this.processSemaphore = new Semaphore(1);    // Semáforo binario
        this.queueSemaphore = new Semaphore(1);      // Semáforo binario
        this.interruptSemaphore = new Semaphore(1);  // Semáforo binario
        
        // Configurar componentes
        setupComponentConnections();
        
        // Estado inicial
        this.currentProcess = null;
        this.isRunning = false;
        this.isPaused = false;
        this.cycleDurationMs = 1000;
        this.cpuMode = CpuMode.IDLE;
        this.kernelActivityThisCycle = false;
        this.blockedQueue = new LinkedList<>();
        
        // =========== GENERAR PROCESOS INICIALES CON PORCENTAJE ==========
        int porcentajeDeseado = 100; // Puse 100 para forzar a que se generen 
        generarProcesosInicialesConPorcentaje(porcentajeDeseado);
        
        System.out.println("✅ SimulationEngine COORDINADOR listo con semáforos de java.util.concurrent");
        System.out.println("   Delegando a: Scheduler, MemoryManager, InterruptHandler");
        System.out.println("   Semáforos: execution, process, queue, interrupt");
    }
    
    /**
     * Genera procesos iniciales basado en un porcentaje
     * @param porcentaje 0-100, probabilidad de que aparezcan procesos al iniciar
     */
    private void generarProcesosInicialesConPorcentaje(int porcentaje) {
        try {
            queueSemaphore.acquire();

            Random rand = new Random();
            int numeroAleatorio = rand.nextInt(100); // 0-99

            System.out.println("🎲 Generando procesos iniciales con " + porcentaje + "% de probabilidad");
            System.out.println("   Número aleatorio: " + numeroAleatorio);

            if (numeroAleatorio < porcentaje) {
                // ¡Sí! Van a aparecer procesos
                int cantidadProcesos = 3 + rand.nextInt(5); // Entre 3 y 7 procesos

                System.out.println("   ✅ ¡PROCESOS GENERADOS! Cantidad: " + cantidadProcesos);

                for (int i = 0; i < cantidadProcesos; i++) {
                    Process p = generator.generateRandomProcess();
                    addProcessToSystem(p);
                }

                logEvent("🎲 " + cantidadProcesos + " procesos iniciales generados (probabilidad " + porcentaje + "%)");
            } else {
                System.out.println("   ❌ No se generaron procesos iniciales (probabilidad no cumplida)");
                logEvent("⚠️ Sistema iniciado SIN procesos (probabilidad " + porcentaje + "% no cumplida)");
            }

            queueSemaphore.release();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("❌ Error generando procesos iniciales");
        }
    }
    
    /**
     * Conecta componentes entre sí.
     */
    private void setupComponentConnections() {
        // Configurar callback de interrupciones
        if (interrupts != null) {
            interrupts.registerInterruptCallback(this::handleIncomingInterrupt);
        }

        System.out.println("🔌 Componentes conectados. Sistema listo.");
    }
    
    /**
     * Ejecuta UN ciclo de coordinación.
     * CON SEMÁFOROS para protección de recursos.
     */
    public void executeOneCycle() {
        if (!isRunning || isPaused) return;

        try {
            executionSemaphore.acquire();
            kernelActivityThisCycle = false;
            enterKernelMode();

            // 1. Avanzar reloj
            globalClock.tick();
            statistics.setCurrentCycle(globalClock.getCurrentCycle());

            // 2. Verificar interrupciones
            checkForInterrupts();

            // 3. Actualizar deadlines
            updateAllProcessDeadlines();

            // 4. Verificar deadlines incumplidos
            checkForDeadlineMisses();

            // 5. Procesar E/S completadas
            processCompletedIO();

            // 6. Manejar memoria
            manageMemory();

            // 7. EJECUTAR PROCESO ACTUAL
            boolean processFinished = executeCurrentProcess();

            // 8. Si terminó, liberar recursos
            if (processFinished) {
                freeResourcesOfTerminatedProcess();
            }

            // 9. Planificar próximo proceso
            scheduleNextProcess();

            // 10. Generar eventos aleatorios
            generateRandomEvents();

            // 11. Actualizar estadísticas
            updateStatistics();

            // 12. Ajustar modo visible final del ciclo
            refreshCpuModeAtCycleEnd();

            executionSemaphore.release();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("❌ Interrupción en ciclo de simulación");
        }
    }
    
    // ========== MÉTODOS DE COORDINACIÓN CON SEMÁFOROS ==========
    
    private void activateSuspendedProcesses() {
        if (memory == null) return;

        // Si hay espacio en RAM, activar suspendidos
        if (memory.hasSpaceInRAM()) {
            System.out.println("🔄 Hay espacio en RAM (" + memory.getAvailableSpaceInRAM() + 
                              ") - Activando suspendidos...");

            LinkedList<Process> readySuspended = memory.getReadySuspendedQueue();
            LinkedList<Process> toActivate = new LinkedList<>();

            // Seleccionar los de mayor prioridad (menor número)
            for (int i = 0; i < readySuspended.size(); i++) {
                Process p = readySuspended.get(i);
                if (toActivate.size() < memory.getAvailableSpaceInRAM()) {
                    toActivate.add(p);
                }
            }

            // Activar los seleccionados
            for (int i = 0; i < toActivate.size(); i++) {
                Process p = toActivate.get(i);
                System.out.println("   ✅ Activando proceso suspendido: " + p.getId());
                p.setState(ProcessState.READY);
                scheduler.addProcess(p);
            }
        }
    }
    
    public void forceActivateSuspended() {
        try {
            executionSemaphore.acquire();
            activateSuspendedProcesses();
            executionSemaphore.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void checkForInterrupts() {
        try {
            interruptSemaphore.acquire();
            // Delegar a InterruptHandler
            if (interrupts != null && interrupts.getPendingInterruptCount() > 0) {
                markKernelActivity();
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

            if (blockedQueue.isEmpty()) {
                queueSemaphore.release();
                return;
            }

            LinkedList<Process> completed = new LinkedList<>();
            int currentCycle = globalClock.getCurrentCycle();

            // Verificar cada proceso bloqueado
            for (int i = 0; i < blockedQueue.size(); i++) {
                Process p = blockedQueue.get(i);

                // Verificar si completó la E/S
                if (p.isIOCompleted(currentCycle)) {
                    completed.add(p);
                    logEvent("✅ E/S completada para: " + p.getId() + 
                            " (bloqueado por " + (currentCycle - p.getBlockedTime()) + " ciclos)");
                }
            }

            // Mover los procesos completados de vuelta al sistema
            for (int i = 0; i < completed.size(); i++) {
                Process p = completed.get(i);

                // Remover de cola bloqueada
                blockedQueue.remove(p);

                // Limpiar estado de bloqueo
                p.clearBlocked();

                // Poner en estado READY
                p.setState(ProcessState.READY);

                // Devolver al scheduler
                scheduler.addProcess(p);

                logEvent("🔄 Proceso desbloqueado: " + p.getId() + " vuelve a READY");
            }

            queueSemaphore.release();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logEvent("❌ Error en processCompletedIO: " + e.getMessage());
        }
    }
    
    private void manageMemory() {
        // Delegar TODO a MemoryManager
        // MemoryManager debe manejar sus propios semáforos internamente
        // 1. Si hay procesos suspendidos y espacio, activar
        memory.tryActivateSuspendedProcesses();
    }
    
    /**
     * Actualiza las estadísticas de la simulación
     */
    private void updateStatistics() {
        if (statistics == null) return;

        int cycle = globalClock.getCurrentCycle();

        // FORZAR cálculo de estadísticas
        double successRate = statistics.calculateSuccessRate();
        double throughput = statistics.calculateThroughput();
        int cpuUsage = statistics.calculateCPUUtilization();
        int totalProcesses = statistics.getTotalProcessesCreated();
        int completed = statistics.getTotalProcessesCompleted();

        // LOG para debugging
        System.out.println("📊 Stats - Ciclo " + cycle + 
                          " | CPU: " + cpuUsage + 
                          "% | Completados: " + completed +
                          "/" + totalProcesses +
                          " | Throughput: " + String.format("%.3f", throughput));

        // Actualizar GUI si hay callback
        if (statsCallback != null) {
            statsCallback.onStatsUpdated(successRate, throughput, cpuUsage, totalProcesses);
        }
    }

    // Interfaz para callback
    public interface StatsCallback {
        void onStatsUpdated(double successRate, double throughput, int cpuUsage, int totalProcesses);
    }

    public void setStatsCallback(StatsCallback callback) {
        this.statsCallback = callback;
    }
    
    /**
     * Ejecuta el proceso actual
     * @return true si el proceso terminó
     */
    private boolean executeCurrentProcess() {
        if (currentProcess == null) {
            return false;
        }

        try {
            processSemaphore.acquire();
            enterUserMode();

            // Verificar si el proceso está en RAM
            if (!isProcessInRAM(currentProcess)) {
                System.out.println("⚠️ " + currentProcess.getId() + " no está en RAM");
                currentProcess = null;
                markKernelActivity();
                processSemaphore.release();
                return false;
            }

            // Marcar inicio si es primera vez
            if (currentProcess.getStartTime() < 0) {
                currentProcess.setStartTime(globalClock.getCurrentCycle());
            }

            // Ejecutar instrucción
            boolean finished = currentProcess.executeInstruction();
            statistics.recordInstructionExecution(1);

            int executedNow = currentProcess.getExecutedInstructions();
            int total = currentProcess.getTotalInstructions();

            System.out.println("⚡ " + currentProcess.getId() + 
                              " ejecutó " + executedNow + "/" + total);

            // 🥇 PRIMERO: Verificar si TERMINÓ
            if (finished || executedNow >= total) {
                System.out.println("   ✅ " + currentProcess.getId() + " COMPLETÓ TODAS LAS INSTRUCCIONES");
                markKernelActivity();
                processSemaphore.release();
                return finishCurrentProcess();
            }

            // 🥈 SEGUNDO: Verificar si debe iniciar E/S
            if (currentProcess.isRequiresIO() &&
                executedNow == currentProcess.getIoStartCycle()) {
                System.out.println("   ⏳ " + currentProcess.getId() + " inicia E/S");
                markKernelActivity();
                processSemaphore.release();
                startIOForCurrentProcess();
                return false;
            }

            // 🥉 TERCERO: Verificar preempción (solo si no terminó)
            if (scheduler.shouldPreempt(currentProcess)) {
                System.out.println("   ⚠️ Preemptando " + currentProcess.getId());
                markKernelActivity();
                currentProcess.setState(ProcessState.READY);
                scheduler.addProcess(currentProcess);
                currentProcess = null;
                processSemaphore.release();
                return false;
            }

            processSemaphore.release();
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
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
                markKernelActivity();
                currentProcess = next;
                currentProcess.setState(ProcessState.RUNNING);
                logEvent("⚡ Ejecutando: " + currentProcess.getId());
            } else {
                setCpuMode(CpuMode.IDLE);
            }
            
            processSemaphore.release();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Finaliza el proceso actual
     */
    private boolean finishCurrentProcess() {
        if (currentProcess == null) return false;
        markKernelActivity();
        
        String processId = currentProcess.getId();
        int executed = currentProcess.getExecutedInstructions();
        int total = currentProcess.getTotalInstructions();
        
        System.out.println("\n🎯🎯🎯 FINALIZANDO: " + processId + 
                          " (" + executed + "/" + total + ") 🎯🎯🎯");
        
        // 1. Marcar como TERMINATED
        currentProcess.setState(ProcessState.TERMINATED);
        currentProcess.setCompletionTime(globalClock.getCurrentCycle());
        
        // 2. Registrar en estadísticas
        statistics.recordProcessCompletion(currentProcess);
        System.out.println("   📊 Estadísticas actualizadas");
        
        // 3. Notificar a MemoryManager
        if (memory != null) {
            memory.processTerminated(currentProcess);
            System.out.println("   ✅ MemoryManager notificado - proceso eliminado de RAM");
            System.out.println("      RAM ahora: " + memory.getRAMUsage() + "/" + memory.getMaxRAMCapacity());
        }
        
        // 4. Liberar el proceso
        System.out.println("   🧹 Proceso " + processId + " ELIMINADO del sistema");
        currentProcess = null;
        
        // 5. Intentar activar procesos suspendidos
        activateSuspendedProcessesIfSpaceAvailable();
        
        return true;
    }

    /**
     * Libera recursos del proceso terminado
     */
    private void freeResourcesOfTerminatedProcess() {
        logEvent("🧹 Recursos liberados");
    }

    /**
     * Verifica si un proceso está en RAM
     */
    private boolean isProcessInRAM(Process process) {
        if (memory == null) return true;

        LinkedList<Process> processesInRAM = memory.getProcessesInRAM();
        for (int i = 0; i < processesInRAM.size(); i++) {
            if (processesInRAM.get(i).getId().equals(process.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inicia E/S para el proceso actual
     */
    private void startIOForCurrentProcess() {
        if (currentProcess == null) return;

        try {
            markKernelActivity();
            logEvent("⏳ E/S iniciada: " + currentProcess.getId());

            // Registrar el ciclo de bloqueo
            currentProcess.setState(ProcessState.BLOCKED);
            currentProcess.setBlockedTime(globalClock.getCurrentCycle());
            currentProcess.setIoCompletionTime(
                globalClock.getCurrentCycle() + currentProcess.getIoDuration()
            );

            // Adquirir semáforo para la cola bloqueada
            queueSemaphore.acquire();

            // Añadir a la cola de bloqueados
            blockedQueue.add(currentProcess);
            logEvent("📋 Proceso bloqueado: " + currentProcess.getId() + 
                    " | Cola blocked: " + blockedQueue.size());

            queueSemaphore.release();

            // El proceso actual ya no está en CPU
            currentProcess = null;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logEvent("❌ Error iniciando E/S: " + e.getMessage());
        }
    }
    
    private void generateRandomEvents() {
        // 5% chance de interrupción aleatoria SOLAMENTE
        if (Math.random() < 0.05 && interrupts != null) {
            interrupts.generateRandomInterrupt();
        }
    }
    
    /**
     * Activa procesos suspendidos si hay espacio en RAM
     */
    private void activateSuspendedProcessesIfSpaceAvailable() {
        if (memory == null) return;
        
        if (memory.hasSpaceInRAM()) {
            System.out.println("🔄 Espacio liberado en RAM (" + memory.getAvailableSpaceInRAM() + 
                              ") - Activando procesos suspendidos...");
            memory.tryActivateSuspendedProcesses();
        }
    }
    
    private void addProcessToSystem(Process process) {
        // 1. Verificar límite GLOBAL del sistema
        int totalProcesses = getTotalProcessesInSystem();
        if (totalProcesses >= 30) {
            logEvent("❌ SISTEMA LLENO: No se puede agregar " + process.getId());
            return;
        }

        // 2. Establecer tiempo de creación
        process.setCreationTime(globalClock.getCurrentCycle());

        // 3. Intentar agregar a RAM
        boolean addedToRAM = memory.addProcess(process);

        if (addedToRAM) {
            // 4. Si entró a RAM, agregar al scheduler
            scheduler.addProcess(process);
            statistics.recordProcessCreation(process);
            logEvent("➕ Proceso agregado a RAM: " + process.getId() + 
                    " (RAM: " + memory.getRAMUsage() + "/" + 
                    memory.getMaxRAMCapacity() + ")");
        } else {
            // 5. Si no entró a RAM, está suspendido
            logEvent("⏸️ Proceso suspendido: " + process.getId() + 
                    " (Suspendidos: " + memory.getReadySuspendedCount() + ")");
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
            
            // Delegar limpieza a componentes
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
            markKernelActivity();
            
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
            setCpuMode(CpuMode.IDLE);
            executionSemaphore.release();
            logEvent("⏹️ Simulación detenida");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Genera 20 procesos de forma CONTROLADA
     */
    public void generate20Processes() {
        logEvent("🎲 Iniciando generación controlada de 20 procesos...");

        new Thread(() -> {
            int created = 0;
            int maxAttempts = 30;

            while (created < 20 && isRunning) {
                if (getTotalProcessesInSystem() >= maxAttempts) {
                    logEvent("⚠️ Límite máximo alcanzado (" + maxAttempts + ")");
                    break;
                }

                Process p = generator.generateRandomProcess();
                addProcessToSystem(p);
                created++;

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (created % 5 == 0 && created < 20) {
                    logEvent("⏳ " + created + " procesos generados...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            logEvent("✅ Generación completada: " + created + " procesos");
            logSystemStatus();
        }).start();
    }

    /**
     * Cuenta el total de procesos en TODO el sistema
     */
    private int getTotalProcessesInSystem() {
        int total = 0;

        total += memory.getRAMUsage();
        total += memory.getReadySuspendedCount();
        total += memory.getBlockedSuspendedCount();

        if (currentProcess != null) {
            boolean alreadyCounted = false;
            LinkedList<Process> inRAM = memory.getProcessesInRAM();

            for (int i = 0; i < inRAM.size(); i++) {
                if (inRAM.get(i).getId().equals(currentProcess.getId())) {
                    alreadyCounted = true;
                    break;
                }
            }

            if (!alreadyCounted) {
                total++;
            }
        }

        return total;
    }
    
    private void logSystemStatus() {
        System.out.println("\n🔍 DIAGNÓSTICO DEL SISTEMA - Ciclo " + globalClock.getCurrentCycle());
        System.out.println("  Proceso actual: " + (currentProcess != null ? currentProcess.getId() : "ninguno"));
        System.out.println("  RAM: " + memory.getRAMUsage() + "/" + memory.getMaxRAMCapacity());
        System.out.println("  Ready Suspended: " + memory.getReadySuspendedCount());
        System.out.println("  Blocked Suspended: " + memory.getBlockedSuspendedCount());
        System.out.println("  Total en sistema: " + getTotalProcessesInSystem());
        System.out.println("  Cola ready: " + scheduler.getReadyQueue().size());
        System.out.println("  Cola blocked: " + blockedQueue.size());
        System.out.println("=====================================\n");
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
        if (algorithm == null) return;

        String normalized = algorithm.trim().toUpperCase();
        rtos.scheduler.SchedulerManager.Algorithm algo;

        switch (normalized) {
            case "FCFS":
                algo = rtos.scheduler.SchedulerManager.Algorithm.FCFS;
                break;
            case "ROUND ROBIN":
            case "ROUND_ROBIN":
            case "RR":
                algo = rtos.scheduler.SchedulerManager.Algorithm.ROUND_ROBIN;
                break;
            case "SRT":
                algo = rtos.scheduler.SchedulerManager.Algorithm.SRT;
                break;
            case "PRIORITY":
            case "PRIORIDAD":
                algo = rtos.scheduler.SchedulerManager.Algorithm.PRIORITY;
                break;
            case "EDF":
                algo = rtos.scheduler.SchedulerManager.Algorithm.EDF;
                break;
            default:
                System.out.println("❌ Algoritmo no válido: " + algorithm);
                return;
        }

        scheduler.switchAlgorithm(algo);
        logEvent("🔀 Algoritmo cambiado a: " + algorithm);
    }

    public void setRoundRobinQuantum(int quantum) {
        scheduler.setRoundRobinQuantum(quantum);
        logEvent("⏱️ Quantum RR ajustado a: " + quantum);
    }
    
    // ========== GETTERS SEGUROS CON SEMÁFOROS ==========
    
    public boolean isRunning() { 
        return isRunning; 
    }
    
    public boolean isPaused() { 
        return isPaused; 
    }

    public CpuMode getCpuMode() {
        return cpuMode;
    }

    public String getCpuModeLabel() {
        switch (cpuMode) {
            case KERNEL:
                return "KERNEL";
            case USER:
                return "USUARIO";
            default:
                return "IDLE";
        }
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

    private void setCpuMode(CpuMode mode) {
        this.cpuMode = mode;
    }

    private void enterUserMode() {
        setCpuMode(CpuMode.USER);
    }

    private void enterKernelMode() {
        setCpuMode(CpuMode.KERNEL);
    }

    private void markKernelActivity() {
        kernelActivityThisCycle = true;
        enterKernelMode();
    }

    private void refreshCpuModeAtCycleEnd() {
        if (kernelActivityThisCycle) {
            enterKernelMode();
            return;
        }

        try {
            processSemaphore.acquire();
            if (currentProcess != null) {
                enterUserMode();
            } else {
                setCpuMode(CpuMode.IDLE);
            }
            processSemaphore.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
