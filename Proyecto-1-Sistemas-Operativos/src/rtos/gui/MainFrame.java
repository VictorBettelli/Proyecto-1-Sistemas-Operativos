/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.gui;
import javax.swing.*;
/**
 *
 * @author VictorB
 */


public class MainFrame extends JFrame {
    public MainFrame() {
        initComponents();
    }
    
    private void initComponents() {
        setTitle("RTOS Simulator - Proyecto 1 - Sistemas Operativos");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel panel = new JPanel();
        JLabel label = new JLabel("<html><center><h1>RTOS Simulator</h1>"
            + "<p>Interfaz gráfica en desarrollo</p>"
            + "<p>Proyecto 1 - Sistemas Operativos</p></center></html>", 
            SwingConstants.CENTER);
        
        panel.add(label);
        add(panel);
    }
}