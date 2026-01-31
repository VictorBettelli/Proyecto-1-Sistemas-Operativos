/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.scheduler;

/**
 *
 * @author VictorB
 */
import rtos.model.Process;
import rtos.structures.Queue;

public interface Scheduler {
    String getName();
    void addProcess(Process process);
    Process getNextProcess();
    boolean isEmpty();
    Queue<Process> getReadyQueue();
}