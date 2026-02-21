/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.model;

/**
 * Enumeración que representa los estados de un proceso
 * Según el modelo de estados completo requerido en el proyecto
 */
public enum ProcessState {
    NEW,                 // Recién creado
    READY,               // En cola de listos, esperando CPU
    RUNNING,             // Ejecutándose en la CPU
    BLOCKED,             // Esperando operación de E/S
    TERMINATED,          // Finalizó su ejecución
    READY_SUSPENDED,     // En memoria secundaria, listo para ejecutar
    BLOCKED_SUSPENDED    // En memoria secundaria, bloqueado por E/S
}
