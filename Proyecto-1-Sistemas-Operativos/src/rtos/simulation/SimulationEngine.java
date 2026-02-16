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
    // =========== CREAR COMPONENTES ==========
    this.globalClock = new Clock();
    this.generator = new ProcessGenerator();
    this.statistics = new StatisticsTracker();
    this.memory = new MemoryManager(10); // 10 procesos máximo en RAM
    this.scheduler = new SchedulerManager(statistics);
    this.interrupts = new InterruptHandler(scheduler);
    
    // =========== INICIALIZAR SEMÁFOROS ==========
    this.executionSemaphore = new Semaphore(1);
    this.processSemaphore = new Semaphore(1);
    this.queueSemaphore = new Semaphore(1);
    this.interruptSemaphore = new Semaphore(1);
    
    // Configurar componentes
    setupComponentConnections();
    
    // Estado inicial
    this.currentProcess = null;
    this.isRunning = false;
    this.isPaused = false;
    this.cycleDurationMs = 1000;
    this.blockedQueue = new LinkedList<>();
    
    // =========== GENERAR PROCESOS INICIALES CON PORCENTAJE ==========
    int porcentajeDeseado = 30; // 30% de probabilidad de generar procesos iniciales
    generarProcesosInicialesConPorcentaje(porcentajeDeseado);
    
    System.out.println("✅ SimulationEngine COORDINADOR listo con semáforos");
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

        // ❌ ELIMINAR LA LLAMADA A initializeWithSampleProcesses()
        // ✅ NO generar procesos automáticamente

        System.out.println("🔌 Componentes conectados. Sistema listo.");
    }
    
    /**
     * Inicializa con procesos de ejemplo.
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
    */
    /**
     * Ejecuta UN ciclo de coordinación.
     * CON SEMÁFOROS para protección de recursos.
     */
    public void executeOneCycle() {
        if (!isRunning || isPaused) return;

        try {
            executionSemaphore.acquire();

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

            // 7. EJECUTAR PROCESO ACTUAL (versión unificada)
            boolean processFinished = executeCurrentProcess(); // AHORA devuelve boolean

            // 8. Si terminó, liberar recursos
            if (processFinished) {
                freeResourcesOfTerminatedProcess(); // ¡NUEVO!
            }

            // 9. Planificar próximo proceso
            scheduleNextProcess();

            // 10. Generar eventos aleatorios
            generateRandomEvents();

            // 11. Actualizar estadísticas
            updateStatistics();

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
                // Nota: MemoryManager debería tener método para activar
                // memory.activateProcess(p);
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

        // AQUÍ DEBES ACTUALIZAR LA GUI
        // Necesitas una referencia a MainFrame o usar un callback
        if (statsCallback != null) {
            statsCallback.onStatsUpdated(successRate, throughput, cpuUsage, totalProcesses);
        }
    }

    // Interfaz para callback
    public interface StatsCallback {
        void onStatsUpdated(double successRate, double throughput, int cpuUsage, int totalProcesses);
    }

    private StatsCallback statsCallback;

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

            // Verificar si el proceso está en RAM
            if (!isProcessInRAM(currentProcess)) {
                System.out.println("⚠️ " + currentProcess.getId() + " no está en RAM");
                currentProcess = null;
                processSemaphore.release();
                return false;
            }

            // Marcar inicio si es primera vez
            if (currentProcess.getStartTime() < 0) {
                currentProcess.setStartTime(globalClock.getCurrentCycle());
            }

            // Ejecutar instrucción
            boolean executed = currentProcess.executeInstruction();

            if (executed) {
                statistics.recordInstructionExecution(1);

                System.out.println("⚡ " + currentProcess.getId() + 
                                  " ejecutó " + currentProcess.getExecutedInstructions() + 
                                  "/" + currentProcess.getTotalInstructions());

                // VERIFICAR PREEMPCIÓN
                if (scheduler.shouldPreempt(currentProcess)) {
                    System.out.println("⚠️ Preemptando " + currentProcess.getId());
                    currentProcess.setState(ProcessState.READY);
                    scheduler.addProcess(currentProcess);
                    currentProcess = null;
                    processSemaphore.release();
                    return false;
                }

                // VERIFICAR SI TERMINÓ
                if (currentProcess.getExecutedInstructions() >= currentProcess.getTotalInstructions()) {
                    processSemaphore.release();
                    return finishCurrentProcess();
                }

                // VERIFICAR SI INICIA E/S
                if (currentProcess.isRequiresIO() && 
                    currentProcess.getExecutedInstructions() == currentProcess.getIoStartCycle()) {
                    startIOForCurrentProcess();
                }
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
                currentProcess = next;
                currentProcess.setState(ProcessState.RUNNING);
                logEvent("⚡ Ejecutando: " + currentProcess.getId());
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
        logEvent("✅ " + currentProcess.getId() + " TERMINÓ!");

        // Marcar como completado
        currentProcess.setState(ProcessState.TERMINATED);
        currentProcess.setCompletionTime(globalClock.getCurrentCycle());

        // Registrar en estadísticas
        statistics.recordProcessCompletion(currentProcess);

        // NOTIFICAR A MEMORY MANAGER (¡ESTO YA FUNCIONA!)
        if (memory != null) {
            memory.processTerminated(currentProcess); // ← LLAMA AL MÉTODO DE MEMORYMANAGER
        }

        // NOTIFICAR A SCHEDULER (si es necesario)
        if (scheduler != null) {
            // Aquí podrías tener un método similar en SchedulerManager
            // scheduler.processTerminated(currentProcess);
        }

        currentProcess = null;
        return true;
    }

   /**
    * Libera recursos del proceso terminado
    */
   private void freeResourcesOfTerminatedProcess() {
       // Log opcional, ya se hizo la limpieza en finishCurrentProcess
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
            logEvent("⏳ E/S iniciada: " + currentProcess.getId());

            // Registrar el ciclo de bloqueo
            currentProcess.setState(ProcessState.BLOCKED);
            currentProcess.setBlockedTime(globalClock.getCurrentCycle());

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
        // ❌ NO generar procesos aleatorios automáticamente
        // Solo interrupciones (que son eventos, no procesos)

        // 5% chance de interrupción aleatoria SOLAMENTE
        if (Math.random() < 0.05 && interrupts != null) {
            interrupts.generateRandomInterrupt();
        }

    }
    
    private void addProcessToSystem(Process process) {
        // 1. Verificar límite GLOBAL del sistema
        int totalProcesses = getTotalProcessesInSystem(); // ¡Ahora funciona!
        if (totalProcesses >= 30) {
            logEvent("❌ SISTEMA LLENO: No se puede agregar " + process.getId());
            return;
        }

        // 2. Establecer tiempo de creación
        process.setCreationTime(globalClock.getCurrentCycle());

        // 3. Intentar agregar a RAM (usando 'memory' en lugar de 'memoryManager')
        boolean addedToRAM = memory.addProcess(process); // ¡CORREGIDO!

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
    
    /**
    * Genera 20 procesos de forma CONTROLADA (NO todos al mismo tiempo)
    */
    public void generate20Processes() {
        logEvent("🎲 Iniciando generación controlada de 20 procesos...");

        // Usar un hilo separado para no bloquear la GUI
        new Thread(() -> {
            int created = 0;
            int maxAttempts = 30;

            while (created < 20 && isRunning) {
                // Verificar límite
                if (getTotalProcessesInSystem() >= maxAttempts) {
                    logEvent("⚠️ Límite máximo alcanzado (" + maxAttempts + ")");
                    break;
                }

                // Crear proceso
                Process p = generator.generateRandomProcess();

                // Agregar al sistema
                addProcessToSystem(p);
                created++;

                // ESPERAR entre cada proceso
                try {
                    Thread.sleep(500); // 500ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // Pausa cada 5 procesos
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
            logSystemStatus(); // Mostrar estado final
        }).start();
    }

   /**
    * Cuenta el total de procesos en TODO el sistema (RAM + suspendidos)
    */
    private int getTotalProcessesInSystem() {
        int total = 0;

        // Usar 'memory' en lugar de 'memoryManager'
        total += memory.getRAMUsage();                    // Procesos en RAM
        total += memory.getReadySuspendedCount();         // Ready suspendidos
        total += memory.getBlockedSuspendedCount();       // Blocked suspendidos

        // Proceso actual (si existe y no está ya contado)
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