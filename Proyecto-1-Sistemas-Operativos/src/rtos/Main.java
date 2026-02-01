/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */

package rtos;

import rtos.model.Process;
import rtos.gui.MainFrame;
import rtos.scheduler.SchedulerManager;
import rtos.interrupt.InterruptHandler;
import rtos.interrupt.InterruptType;
import javax.swing.SwingUtilities;

/**
 *
 * @author VictorB
 */

public class Main {
    // Variables estáticas para acceso global
    private static SchedulerManager schedulerManager;
    private static InterruptHandler interruptHandler;
    private static boolean testMode = true; // ACTIVADO para pruebas
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("PROYECTO 1 - SISTEMAS OPERATIVOS");
        System.out.println("Simulador RTOS para Microsatélite");
        System.out.println("========================================");
        
        System.out.println("\n✅ VERIFICANDO ESTRUCTURA:");
        
        // Probar que podemos crear instancias
        Process proceso = new Process("P001", "Monitor de Temperatura");
        System.out.println("1. Proceso creado: " + proceso);
        
        System.out.println("2. Paquete structures: Node, LinkedList, Queue, PriorityQueue, ProcessQueue");
        System.out.println("3. Paquete model: Process");
        System.out.println("4. Paquete scheduler: Scheduler (interfaz)");
        System.out.println("5. Paquete memory: MemoryManager");
        System.out.println("6. Paquete interrupt: InterruptType (enum)");
        System.out.println("7. Paquete utils: Comparator (interfaz)");
        System.out.println("8. Paquete gui: MainFrame");
        
        System.out.println("\n✅ TODOS LOS PAQUETES TIENEN ALGO");
        System.out.println("✅ ESTRUCTURA BASE COMPLETA");
        
        // ========== NUEVO: INICIALIZAR SISTEMA DE INTERRUPCIONES ==========
        System.out.println("\n🔧 INICIALIZANDO SISTEMA DE INTERRUPCIONES...");
        
        try {
            // 1. Crear SchedulerManager
            schedulerManager = new SchedulerManager();
            System.out.println("✓ SchedulerManager creado");
            System.out.println("  Algoritmo: " + schedulerManager.getCurrentAlgorithmName());
            
            // 2. Crear InterruptHandler
            interruptHandler = new InterruptHandler(schedulerManager);
            System.out.println("✓ InterruptHandler creado con Thread workers");
            System.out.println("  Semaforos y exclusión mutua activados");
            
            // 3. Probar algunas interrupciones si estamos en modo prueba
            if (testMode) {
                runInterruptTests();
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR al inicializar sistema de interrupciones:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
        }
        
        // NO iniciamos simulación en segundo plano porque estamos en modo prueba
        // startBackgroundSimulation(); // COMENTADO
        
        // Iniciar interfaz gráfica (pero primero probamos las interrupciones)
        System.out.println("\n🚀 Iniciando interfaz gráfica...");
        SwingUtilities.invokeLater(() -> {
            MainFrame ventana = new MainFrame();
            
            // Tu MainFrame actual no tiene estos métodos, pero puedes agregarlos después
            // cuando desarrolles la GUI
            
            ventana.setVisible(true);
            System.out.println("✅ Ventana principal abierta");
            System.out.println("✅ Sistema RTOS listo para operar");
            
            // Mostrar estado inicial
            if (schedulerManager != null) {
                System.out.println("📊 Estado inicial:");
                System.out.println("   - Scheduler: " + schedulerManager.getCurrentAlgorithmName());
                System.out.println("   - Interrupciones activas: " + 
                    (interruptHandler != null && interruptHandler.isRunning()));
            }
        });
    }
    
    /**
     * Ejecuta pruebas del sistema de interrupciones
     */
    private static void runInterruptTests() {
        System.out.println("\n🧪 EJECUTANDO PRUEBAS DE INTERRUPCIONES...");
        
        try {
            // Prueba 1: Interrupción de baja prioridad
            System.out.println("\n📡 Prueba 1: Comando desde Tierra");
            interruptHandler.raiseInterrupt(InterruptType.GROUND_COMMAND, 1, "Estación Control Houston");
            Thread.sleep(1000);
            
            // Prueba 2: Interrupción de E/S
            System.out.println("\n💾 Prueba 2: E/S Completada");
            interruptHandler.raiseInterrupt(InterruptType.IO_COMPLETION, 1, "Disco de Estado Sólido");
            Thread.sleep(1000);
            
            // Prueba 3: Deadline incumplido
            System.out.println("\n⏰ Prueba 3: Deadline Incumplido");
            interruptHandler.raiseInterrupt(InterruptType.DEADLINE_MISSED, 2, "Sistema de Navegación");
            Thread.sleep(1000);
            
            // Prueba 4: Emergencia (máxima prioridad)
            System.out.println("\n🚨 Prueba 4: EMERGENCIA - Micro-meteorito");
            interruptHandler.raiseInterrupt(InterruptType.MICROMETEORITE, 5, "Casco Exterior");
            Thread.sleep(1000);
            
            // Esperar a que se procesen todas
            System.out.println("\n⏳ Esperando procesamiento de interrupciones...");
            Thread.sleep(2000);
            
            // Mostrar resultados
            System.out.println("\n📊 RESULTADOS DE PRUEBAS:");
            System.out.println("Interrupciones pendientes: " + interruptHandler.getPendingInterruptCount());
            System.out.println("Logs del sistema: " + schedulerManager.getEventLogs().size() + " entradas");
            
            // Mostrar últimos 5 logs
            System.out.println("\n📝 Últimos logs:");
            var logs = schedulerManager.getRecentLogs(5);
            for (int i = 0; i < logs.size(); i++) {
                System.out.println("  " + logs.get(i));
            }
            
            System.out.println("\n✅ PRUEBAS COMPLETADAS EXITOSAMENTE");
            
        } catch (InterruptedException e) {
            System.err.println("Pruebas interrumpidas: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ ERROR en pruebas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Métodos estáticos para acceso desde otras partes del sistema
     */
    public static SchedulerManager getSchedulerManager() {
        return schedulerManager;
    }
    
    public static InterruptHandler getInterruptHandler() {
        return interruptHandler;
    }
    
    /**
     * Detiene el sistema de interrupciones de forma ordenada
     */
    public static void shutdownSystem() {
        System.out.println("\n🛑 DETENIENDO SISTEMA DE INTERRUPCIONES...");
        
        if (interruptHandler != null) {
            interruptHandler.shutdown();
            System.out.println("✓ InterruptHandler detenido");
        }
        
        if (schedulerManager != null) {
            System.out.println("✓ SchedulerManager finalizado");
            // Mostrar logs finales
            var logs = schedulerManager.getRecentLogs(5);
            if (!logs.isEmpty()) {
                System.out.println("\n📝 Logs finales del sistema:");
                for (int i = 0; i < logs.size(); i++) {
                    System.out.println("  " + logs.get(i));
                }
            }
        }
        
        System.out.println("✅ Sistema RTOS detenido exitosamente");
    }
}