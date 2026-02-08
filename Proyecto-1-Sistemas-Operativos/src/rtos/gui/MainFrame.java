/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.gui;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import rtos.scheduler.SchedulerManager;
import rtos.simulation.SimulationEngine;
import rtos.model.Process;
import rtos.model.ProcessState;
import rtos.memory.MemoryManager;
import rtos.statistics.StatisticsTracker;
import rtos.structures.LinkedList;

public class MainFrame extends JFrame {
    // Managers
    private SimulationEngine simulationEngine;
    private SchedulerManager schedulerManager;
    private MemoryManager memoryManager;
    private StatisticsTracker statisticsTracker;
    
    // Componentes de la GUI
    private JLabel clockLabel;
    private JLabel memoryUsageLabel;
    private JLabel successRateLabel;
    private JLabel throughputLabel;
    private JLabel cpuUsageLabel;
    
    // Tablas para mostrar procesos
    private JTable readyQueueTable;
    private JTable blockedQueueTable;
    private JTable suspendedQueueTable;
    private JTable runningProcessTable;
    
    // Área de log
    private JTextArea logArea;
    
    // Controles
    private JComboBox<String> algorithmComboBox;
    private JSpinner quantumSpinner;
    private JButton generateProcessesButton;
    private JButton addEmergencyButton;
    private JButton startButton;
    private JButton pauseButton;
    private JButton resetButton;
    
    // Estado de simulación
    private boolean simulationRunning = false;
    private Timer simulationTimer;
    
    public MainFrame() {
        initManagers();
        initComponents();
        setupSimulationTimer();
    }
    
    private void initManagers() {
        simulationEngine = new SimulationEngine();
        schedulerManager = simulationEngine.getSchedulerManager();
        memoryManager = simulationEngine.getMemoryManager();
        statisticsTracker = simulationEngine.getStatisticsTracker();
    }
    
    private void initComponents() {
        setTitle("UNIMET-Sat RTOS Simulator - Memory Management & Swap");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLayout(new BorderLayout(10, 10));
        
        // ========== PANEL SUPERIOR ==========
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // Título
        JLabel titleLabel = new JLabel("UNIMET-Sat RTOS Simulator - Memory Management & Swap", 
                                      SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Panel de reloj y controles
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        
        // Reloj de misión
        clockLabel = new JLabel("MISSION CLOCK: Cycle 0");
        clockLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        controlPanel.add(clockLabel);
        
        // Selector de algoritmo
        controlPanel.add(new JLabel("Algorithm:"));
        algorithmComboBox = new JComboBox<>(new String[]{
            "FCFS", "Round Robin", "SRT", "Priority", "EDF"
        });
        controlPanel.add(algorithmComboBox);
        
        // Quantum para Round Robin
        quantumSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 20, 1));
        controlPanel.add(new JLabel("Quantum:"));
        controlPanel.add(quantumSpinner);
        quantumSpinner.setVisible(false); // Solo visible para Round Robin
        
        topPanel.add(controlPanel, BorderLayout.CENTER);
        
        // Botones de control
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        startButton = new JButton("▶ Start");
        pauseButton = new JButton("⏸ Pause");
        pauseButton.setEnabled(false);
        resetButton = new JButton("⏹ Reset");
        generateProcessesButton = new JButton("Generate 20 Processes");
        addEmergencyButton = new JButton("Add Emergency Process");
        
        buttonPanel.add(startButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(generateProcessesButton);
        buttonPanel.add(addEmergencyButton);
        
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        
        // ========== PANEL CENTRAL ==========
        JPanel centerPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Cola de Listos (Ready Queue)
        centerPanel.add(createQueuePanel("READY QUEUE (RAM)", 
            new String[]{"ID", "Name", "Priority", "Deadline", "Instructions"}, 
            readyQueueTable = new JTable()));
        
        // Proceso en Ejecución (Running)
        centerPanel.add(createProcessPanel("RUNNING PROCESS (CPU)", 
            runningProcessTable = createSingleProcessTable()));
        
        // Cola de Bloqueados (Blocked Queue)
        centerPanel.add(createQueuePanel("BLOCKED QUEUE (I/O)", 
            new String[]{"ID", "Name", "IO Type", "Remaining"}, 
            blockedQueueTable = new JTable()));
        
        // Colas de Suspendidos
        centerPanel.add(createQueuePanel("SUSPENDED QUEUES", 
            new String[]{"ID", "Name", "State", "Time Suspended"}, 
            suspendedQueueTable = new JTable()));
        
        // Uso de Memoria
        centerPanel.add(createMemoryPanel());
        
        // Estadísticas
        centerPanel.add(createStatsPanel());
        
        add(centerPanel, BorderLayout.CENTER);
        
        // ========== PANEL INFERIOR ==========
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // Panel de Interrupciones de Emergencia
        JPanel interruptPanel = new JPanel();
        interruptPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.RED, 2),
            "EMERGENCY INTERRUPTION",
            TitledBorder.CENTER,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            Color.RED));
        
        JLabel interruptLabel = new JLabel("No active interruptions");
        interruptLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        interruptPanel.add(interruptLabel);
        
        bottomPanel.add(interruptPanel, BorderLayout.NORTH);
        
        // Área de Log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Event Log"));
        
        logArea = new JTextArea(8, 80);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);
        
        bottomPanel.add(logPanel, BorderLayout.CENTER);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // ========== CONFIGURAR LISTENERS ==========
        setupEventListeners();
        
        // Actualizar GUI inicial
        updateAllDisplays();
    }
    
    private JPanel createQueuePanel(String title, String[] columns, JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table.setModel(model);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createProcessPanel(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        
        // Tabla para un solo proceso
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Property", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table.setModel(model);
        table.setRowHeight(25);
        
        // Agregar filas iniciales
        model.addRow(new Object[]{"Process ID", "None"});
        model.addRow(new Object[]{"Process Name", "Idle"});
        model.addRow(new Object[]{"State", "IDLE"});
        model.addRow(new Object[]{"PC/MAR", "0/0"});
        model.addRow(new Object[]{"Deadline", "∞"});
        model.addRow(new Object[]{"Instructions", "0/0"});
        model.addRow(new Object[]{"Priority", "-"});
        
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JTable createSingleProcessTable() {
        JTable table = new JTable();
        table.setRowHeight(25);
        return table;
    }
    
    private JPanel createMemoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Memory Usage"));
        
        // Barra de progreso
        JProgressBar memoryBar = new JProgressBar(0, 100);
        memoryBar.setValue(0);
        memoryBar.setStringPainted(true);
        memoryBar.setForeground(new Color(0, 150, 0));
        
        memoryUsageLabel = new JLabel("0% (0/10 processes)", SwingConstants.CENTER);
        memoryUsageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(memoryBar, BorderLayout.CENTER);
        contentPanel.add(memoryUsageLabel, BorderLayout.SOUTH);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Información de swap
        JPanel swapPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        JLabel swapOutLabel = new JLabel("Swap Out: 0 processes", SwingConstants.CENTER);
        swapOutLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        
        JLabel swapInLabel = new JLabel("Swap In: 0 processes", SwingConstants.CENTER);
        swapInLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        
        swapPanel.add(swapOutLabel);
        swapPanel.add(swapInLabel);
        
        panel.add(swapPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Mission Statistics"));
        
        JPanel statsPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        
        successRateLabel = createStatLabel("Success Rate: 100%", Color.GREEN);
        throughputLabel = createStatLabel("Throughput: 0 processes/cycle", Color.BLUE);
        cpuUsageLabel = createStatLabel("CPU Usage: 0%", Color.ORANGE);
        JLabel deadlineMissLabel = createStatLabel("Deadline Misses: 0", Color.RED);
        JLabel processCountLabel = createStatLabel("Total Processes: 0", Color.BLACK);
        
        statsPanel.add(successRateLabel);
        statsPanel.add(throughputLabel);
        statsPanel.add(cpuUsageLabel);
        statsPanel.add(deadlineMissLabel);
        statsPanel.add(processCountLabel);
        
        panel.add(statsPanel, BorderLayout.CENTER);
        
        // Botón para mostrar gráficas
        JButton showChartsButton = new JButton("Show Performance Charts");
        showChartsButton.addActionListener(e -> showPerformanceCharts());
        panel.add(showChartsButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JLabel createStatLabel(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(color);
        return label;
    }
    
    private void setupEventListeners() {
        startButton.addActionListener((ActionEvent e) -> startSimulation());
        pauseButton.addActionListener((ActionEvent e) -> pauseSimulation());
        resetButton.addActionListener((ActionEvent e) -> resetSimulation());
        generateProcessesButton.addActionListener((ActionEvent e) -> generateRandomProcesses());
        addEmergencyButton.addActionListener((ActionEvent e) -> addEmergencyProcess());
        algorithmComboBox.addActionListener((ActionEvent e) -> changeAlgorithm());
    }
    
    private void setupSimulationTimer() {
        simulationTimer = new Timer(1000, (ActionEvent e) -> { // 1 segundo por ciclo
            if (simulationRunning && simulationEngine != null) {
                simulationEngine.executeOneCycle();
                updateAllDisplays();
            }
        });
    }
    
    private void updateAllDisplays() {
        if (simulationEngine == null) return;
        
        // Actualizar reloj
        int currentCycle = simulationEngine.getCurrentCycle();
        clockLabel.setText("MISSION CLOCK: Cycle " + currentCycle);
        
        // Actualizar todas las tablas
        updateReadyQueueTable();
        updateRunningProcessTable();
        updateBlockedQueueTable();
        updateSuspendedQueueTable();
        updateMemoryUsage();
        updateStatistics();
        
        // Agregar entrada al log
        logEvent("Cycle " + currentCycle + " completed");
    }
    
    private void updateReadyQueueTable() {
        DefaultTableModel model = (DefaultTableModel) readyQueueTable.getModel();
        model.setRowCount(0);
        
        if (simulationEngine != null) {
            LinkedList<Process> readyQueue = simulationEngine.getReadyQueue();
            if (readyQueue != null) {
                for (int i = 0; i < readyQueue.size(); i++) {
                    Process p = readyQueue.get(i);
                    if (p != null) {
                        model.addRow(new Object[]{
                            p.getId(),
                            p.getName(),
                            p.getPriority(),
                            p.getRemainingDeadline(),
                            p.getExecutedInstructions() + "/" + p.getTotalInstructions()
                        });
                    }
                }
            }
        }
    }
    
    private void updateRunningProcessTable() {
        DefaultTableModel model = (DefaultTableModel) runningProcessTable.getModel();
        
        if (simulationEngine != null) {
            Process current = simulationEngine.getCurrentProcess();
            
            if (current != null) {
                model.setValueAt(current.getId(), 0, 1);
                model.setValueAt(current.getName(), 1, 1);
                model.setValueAt(current.getState().toString(), 2, 1);
                model.setValueAt(current.getExecutedInstructions() + "/" + 
                               current.getTotalInstructions(), 3, 1);
                model.setValueAt(current.getRemainingDeadline() + " cycles", 4, 1);
                model.setValueAt(current.getExecutedInstructions() + "/" + 
                               current.getTotalInstructions(), 5, 1);
                model.setValueAt(current.getPriority(), 6, 1);
            } else {
                // CPU idle
                model.setValueAt("None", 0, 1);
                model.setValueAt("Idle", 1, 1);
                model.setValueAt("IDLE", 2, 1);
                model.setValueAt("0/0", 3, 1);
                model.setValueAt("∞", 4, 1);
                model.setValueAt("0/0", 5, 1);
                model.setValueAt("-", 6, 1);
            }
        }
    }
    
    private void updateBlockedQueueTable() {
        DefaultTableModel model = (DefaultTableModel) blockedQueueTable.getModel();
        model.setRowCount(0);
        
        if (simulationEngine != null) {
            LinkedList<Process> blockedQueue = simulationEngine.getBlockedQueue();
            if (blockedQueue != null) {
                for (int i = 0; i < blockedQueue.size(); i++) {
                    Process p = blockedQueue.get(i);
                    if (p != null) {
                        String ioType = p.isRequiresIO() ? "I/O Active" : "Blocked";
                        String remaining = p.getRemainingDeadline() + " cycles";
                        model.addRow(new Object[]{
                            p.getId(),
                            p.getName(),
                            ioType,
                            remaining
                        });
                    }
                }
            }
        }
    }
    
    private void updateSuspendedQueueTable() {
        DefaultTableModel model = (DefaultTableModel) suspendedQueueTable.getModel();
        model.setRowCount(0);
        
        if (memoryManager != null) {
            // Ready suspended
            LinkedList<Process> readySuspended = memoryManager.getReadySuspendedQueue();
            if (readySuspended != null) {
                for (int i = 0; i < readySuspended.size(); i++) {
                    Process p = readySuspended.get(i);
                    if (p != null) {
                        model.addRow(new Object[]{
                            p.getId(),
                            p.getName(),
                            "READY_SUSPENDED",
                            (simulationEngine.getCurrentCycle() - p.getCreationTime()) + " cycles"
                        });
                    }
                }
            }
            
            // Blocked suspended
            LinkedList<Process> blockedSuspended = memoryManager.getBlockedSuspendedQueue();
            if (blockedSuspended != null) {
                for (int i = 0; i < blockedSuspended.size(); i++) {
                    Process p = blockedSuspended.get(i);
                    if (p != null) {
                        model.addRow(new Object[]{
                            p.getId(),
                            p.getName(),
                            "BLOCKED_SUSPENDED", 
                            (simulationEngine.getCurrentCycle() - p.getCreationTime()) + " cycles"
                        });
                    }
                }
            }
        }
    }
    
    private void updateMemoryUsage() {
        if (memoryManager == null) return;
        
        int inRAM = memoryManager.getRAMUsage();
        int max = memoryManager.getMaxRAMCapacity();
        
        if (max == 0) return;
        int usage = (inRAM * 100) / max;
        
        // Actualizar barra de progreso
        JProgressBar memoryBar = findMemoryProgressBar();
        if (memoryBar != null) {
            memoryBar.setValue(usage);
        }
        
        memoryUsageLabel.setText(usage + "% (" + inRAM + "/" + max + " procesos)");
    }
    
    private void updateStatistics() {
        if (statisticsTracker != null) {
            // Actualizar con estadísticas reales si están disponibles
            // Por ahora, valores de ejemplo
            successRateLabel.setText("Success Rate: 95%");
            throughputLabel.setText("Throughput: 2.5 processes/cycle");
            cpuUsageLabel.setText("CPU Usage: 78%");
        }
    }
    
    private void startSimulation() {
        if (simulationEngine == null) return;
        
        simulationRunning = true;
        simulationTimer.start();
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        simulationEngine.start();
        logEvent("Simulation started");
    }
    
    private void pauseSimulation() {
        simulationRunning = false;
        simulationTimer.stop();
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        if (simulationEngine != null) {
            simulationEngine.pause();
        }
        logEvent("Simulation paused");
    }
    
    private void resetSimulation() {
        simulationRunning = false;
        simulationTimer.stop();
        
        // Resetear SimulationEngine
        if (simulationEngine != null) {
            simulationEngine.stop();
        }
        
        // Crear nueva instancia
        initManagers();
        
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        
        // Limpiar GUI
        clearAllTables();
        logArea.setText("");
        clockLabel.setText("MISSION CLOCK: Cycle 0");
        
        // Actualizar displays
        updateAllDisplays();
        
        logEvent("Simulation reset");
    }
    
    private void generateRandomProcesses() {
        if (simulationEngine != null) {
            simulationEngine.generate20Processes();
            logEvent("Generating 20 random processes...");
            updateAllDisplays();
        }
    }
    
    private void addEmergencyProcess() {
        if (simulationEngine != null) {
            simulationEngine.addEmergencyProcess();
            logEvent("EMERGENCY: Adding emergency process (Micro-meteorite impact)");
            updateAllDisplays();
        }
    }
    
    private void changeAlgorithm() {
        String algorithm = (String) algorithmComboBox.getSelectedItem();
        
        if (simulationEngine != null && algorithm != null) {
            simulationEngine.changeAlgorithm(algorithm);
            logEvent("Algorithm changed to: " + algorithm);
        }
        
        // Mostrar quantum solo para Round Robin
        quantumSpinner.setVisible(algorithm != null && algorithm.equals("Round Robin"));
    }
    
    private void clearAllTables() {
        ((DefaultTableModel) readyQueueTable.getModel()).setRowCount(0);
        ((DefaultTableModel) blockedQueueTable.getModel()).setRowCount(0);
        ((DefaultTableModel) suspendedQueueTable.getModel()).setRowCount(0);
        
        // Resetear tabla de proceso en ejecución
        DefaultTableModel runningModel = (DefaultTableModel) runningProcessTable.getModel();
        runningModel.setValueAt("None", 0, 1);
        runningModel.setValueAt("Idle", 1, 1);
        runningModel.setValueAt("IDLE", 2, 1);
        runningModel.setValueAt("0/0", 3, 1);
        runningModel.setValueAt("∞", 4, 1);
        runningModel.setValueAt("0/0", 5, 1);
        runningModel.setValueAt("-", 6, 1);
    }
    
    private void logEvent(String message) {
        int currentCycle = simulationEngine != null ? simulationEngine.getCurrentCycle() : 0;
        logArea.append("[" + currentCycle + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    private void showPerformanceCharts() {
        JOptionPane.showMessageDialog(this,
            "Performance charts feature will be implemented with JFreeChart\n" +
            "Showing: CPU Utilization, Deadline Success Rate, Throughput",
            "Performance Charts",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private JProgressBar findMemoryProgressBar() {
        // Buscar la barra de progreso en el panel de memoria
        for (Component comp : getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                for (Component subComp : panel.getComponents()) {
                    if (subComp instanceof JProgressBar) {
                        return (JProgressBar) subComp;
                    }
                }
            }
        }
        return null;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}