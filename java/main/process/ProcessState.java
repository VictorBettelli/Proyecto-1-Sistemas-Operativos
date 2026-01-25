/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.process;

/**
 *
 * @author luisf
 */
public enum ProcessState {
    NEW,           // Nuevo
    READY,         // Listo
    RUNNING,       // Ejecución
    BLOCKED,       // Bloqueado
    TERMINATED,    // Terminado
    READY_SUSPENDED,   // Listo/Suspendido
    BLOCKED_SUSPENDED  // Bloqueado/Suspendido
}
