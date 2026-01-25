/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.scheduler;

import main.process.Process;
import main.structures.CustomQueue;
/**
 *
 * @author luisf
 */
public interface Scheduler {
    void addProcess(Process process);
    Process getNextProcess();
    boolean hasNextProcess();
    String getName();
    void clear();
}
