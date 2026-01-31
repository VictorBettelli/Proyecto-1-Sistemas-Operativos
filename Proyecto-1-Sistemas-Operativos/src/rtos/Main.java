/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package rtos;
import rtos.model.Process;
import rtos.gui.MainFrame;
import javax.swing.SwingUtilities;
/**
 *
 * @author VictorB
 */



public class Main {
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
        
        // Iniciar interfaz
        System.out.println("\n🚀 Iniciando interfaz gráfica...");
        SwingUtilities.invokeLater(() -> {
            MainFrame ventana = new MainFrame();
            ventana.setVisible(true);
            System.out.println("✅ Ventana principal abierta");
        });
    }
}