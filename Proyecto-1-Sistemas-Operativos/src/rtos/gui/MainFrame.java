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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import rtos.interrupt.InterruptHandler;
import rtos.scheduler.SchedulerManager;
import rtos.simulation.SimulationEngine;
import rtos.model.Process;
import rtos.model.ProcessState;
import rtos.memory.MemoryManager;
import rtos.statistics.StatisticsTracker;
import rtos.structures.LinkedList;
import rtos.interrupt.InterruptHandler;
/*
*luisf
*/
public class MainFrame extends JFrame {
    // Managers
    private SimulationEngine simulationEngine;
    private SchedulerManager schedulerManager;
    private MemoryManager memoryManager;
    private StatisticsTracker statisticsTracker;
    private InterruptHandler interruptHandler;
    // Componentes de la GUI
    private JLabel clockLabel;
    private JLabel cpuModeLabel;
    private JLabel memoryUsageLabel;
    private JLabel successRateLabel;
    private JLabel throughputLabel;
    private JLabel cpuUsageLabel;
    private JLabel avgWaitingLabel;
    private JLabel deadlineMissLabel;
    private JLabel processCountLabel;
    private PerformanceChartPanel miniChartPanel;
    
    // Tablas para mostrar procesos
    private JTable readyQueueTable;
    private JTable blockedQueueTable;
    private JTable suspendedQueueTable;
    private JTable runningProcessTable;
    private JTable terminatedQueueTable;
    
    // Área de log
    private JTextArea logArea;
    private JProgressBar memoryProgressBar;
    
    // Controles
    private JComboBox<String> algorithmComboBox;
    private JSpinner quantumSpinner;
    private JSpinner cycleDurationSpinner;
    private JButton generateProcessesButton;
    private JButton addEmergencyButton;
    private JButton startButton;
    private JButton pauseButton;
    private JButton tickButton;
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
        interruptHandler = simulationEngine.getInterruptHandler();
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

        // Indicador de modo CPU (KERNEL/USER/IDLE)
        cpuModeLabel = new JLabel("CPU MODE: IDLE");
        cpuModeLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        controlPanel.add(cpuModeLabel);
        
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

        // Duración de ciclo (ms)
        cycleDurationSpinner = new JSpinner(new SpinnerNumberModel(
            simulationEngine.getCycleDurationMs(), 50, 5000, 50));
        controlPanel.add(new JLabel("Cycle (ms):"));
        controlPanel.add(cycleDurationSpinner);
        
        topPanel.add(controlPanel, BorderLayout.CENTER);
        
        // Botones de control
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        startButton = new JButton("▶ Start");
        pauseButton = new JButton("⏸ Pause");
        pauseButton.setEnabled(false);
        tickButton = new JButton("⏭ Tick");
        tickButton.setEnabled(false);
        resetButton = new JButton("⏹ Reset");
        generateProcessesButton = new JButton("Generate 20 Processes");
        addEmergencyButton = new JButton("Add Emergency Process");
        
        buttonPanel.add(startButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tickButton);
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
        
        // Cola de procesos terminados
        JPanel terminatedPanel = createQueuePanel("TERMINATED PROCESSES",
            new String[]{"ID", "Name", "Completion", "Deadline"},
            terminatedQueueTable = new JTable());
        terminatedPanel.setPreferredSize(new Dimension(420, 0));
        bottomPanel.add(terminatedPanel, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // ========== CONFIGURAR LISTENERS ==========
        setupEventListeners();
        updateCycleDuration();
        
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
        model.addRow(new Object[]{"CPU Mode", "IDLE"});
        
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
        memoryProgressBar = new JProgressBar(0, 100);
        memoryProgressBar.setValue(0);
        memoryProgressBar.setStringPainted(true);
        memoryProgressBar.setForeground(new Color(0, 150, 0));
        
        memoryUsageLabel = new JLabel("0% (0/10 processes)", SwingConstants.CENTER);
        memoryUsageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(memoryProgressBar, BorderLayout.CENTER);
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
        
        JPanel statsPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        
        successRateLabel = createStatLabel("Success Rate: 100%", Color.GREEN);
        throughputLabel = createStatLabel("Throughput: 0 processes/cycle", Color.BLUE);
        cpuUsageLabel = createStatLabel("CPU Usage: 0%", Color.ORANGE);
        avgWaitingLabel = createStatLabel("Avg Waiting: 0.00 cycles", Color.MAGENTA);
        deadlineMissLabel = createStatLabel("Deadline Misses: 0", Color.RED);
        processCountLabel = createStatLabel("Total Processes: 0", Color.BLACK);
        
        statsPanel.add(successRateLabel);
        statsPanel.add(throughputLabel);
        statsPanel.add(cpuUsageLabel);
        statsPanel.add(avgWaitingLabel);
        statsPanel.add(deadlineMissLabel);
        statsPanel.add(processCountLabel);
        
        panel.add(statsPanel, BorderLayout.NORTH);

        miniChartPanel = new PerformanceChartPanel();
        miniChartPanel.setPreferredSize(new Dimension(320, 140));
        panel.add(miniChartPanel, BorderLayout.CENTER);
        
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
        tickButton.addActionListener((ActionEvent e) -> stepSimulationOnce());
        resetButton.addActionListener((ActionEvent e) -> resetSimulation());
        generateProcessesButton.addActionListener((ActionEvent e) -> generateRandomProcesses());
        addEmergencyButton.addActionListener((ActionEvent e) -> addEmergencyProcess());
        algorithmComboBox.addActionListener((ActionEvent e) -> changeAlgorithm());
        quantumSpinner.addChangeListener(e -> updateRoundRobinQuantum());
        cycleDurationSpinner.addChangeListener(e -> updateCycleDuration());
    }
    
    private void setupSimulationTimer() {
        simulationTimer = new Timer(200, (ActionEvent e) -> {
            if (simulationRunning && simulationEngine != null) {
                updateAllDisplays();
            }
        });
    }
    
    private void updateAllDisplays() {
        if (simulationEngine == null) return;
        
        // Actualizar reloj
        int currentCycle = simulationEngine.getCurrentCycle();
        clockLabel.setText("MISSION CLOCK: Cycle " + currentCycle);
        cpuModeLabel.setText("CPU MODE: " + simulationEngine.getCpuModeLabel());
        
        // Actualizar todas las tablas
        updateReadyQueueTable();
        updateRunningProcessTable();
        updateBlockedQueueTable();
        updateSuspendedQueueTable();
        updateTerminatedQueueTable();
        updateMemoryUsage();
        updateStatistics();
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
                model.setValueAt(current.getProgramCounter() + "/" +
                               current.getMemoryAddressRegister(), 3, 1);
                model.setValueAt(current.getRemainingDeadline() + " cycles", 4, 1);
                model.setValueAt(current.getExecutedInstructions() + "/" + 
                               current.getTotalInstructions(), 5, 1);
                model.setValueAt(current.getPriority(), 6, 1);
                model.setValueAt(simulationEngine.getCpuModeLabel(), 7, 1);
            } else {
                // CPU idle
                model.setValueAt("None", 0, 1);
                model.setValueAt("Idle", 1, 1);
                model.setValueAt("IDLE", 2, 1);
                model.setValueAt("0/0", 3, 1);
                model.setValueAt("∞", 4, 1);
                model.setValueAt("0/0", 5, 1);
                model.setValueAt("-", 6, 1);
                model.setValueAt("IDLE", 7, 1);
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

    private void updateTerminatedQueueTable() {
        if (terminatedQueueTable == null || simulationEngine == null) return;

        DefaultTableModel model = (DefaultTableModel) terminatedQueueTable.getModel();
        model.setRowCount(0);

        LinkedList<Process> terminatedQueue = simulationEngine.getTerminatedQueue();
        if (terminatedQueue == null) return;

        for (int i = 0; i < terminatedQueue.size(); i++) {
            Process p = terminatedQueue.get(i);
            if (p == null) continue;

            String deadlineStatus = p.isDeadlineMissed() ? "MISSED" : "ON TIME";
            model.addRow(new Object[]{
                p.getId(),
                p.getName(),
                p.getCompletionTime(),
                deadlineStatus
            });
        }
    }
    
    private void updateMemoryUsage() {
        if (memoryManager == null) return;
        
        int inRAM = memoryManager.getRAMUsage();
        int max = memoryManager.getMaxRAMCapacity();
        
        if (max == 0) return;
        int usage = (inRAM * 100) / max;
        
        // Actualizar barra de progreso
        if (memoryProgressBar != null) {
            memoryProgressBar.setValue(usage);
        }
        
        memoryUsageLabel.setText(usage + "% (" + inRAM + "/" + max + " procesos)");
    }
    
    private void updateStatistics() {
        if (statisticsTracker != null) {
            // Usar los métodos CORRECTOS de StatisticsTracker
            double successRate = statisticsTracker.getSuccessRate();
            double throughput = statisticsTracker.getThroughput();
            int cpuUsage = statisticsTracker.getCPUUtilization(); // ¡IMPORTANTE: getCPUUtilization()!
            double avgWaiting = statisticsTracker.getAverageWaitingTime();
            int deadlineMisses = statisticsTracker.getTotalDeadlinesMissed();
            int totalProcesses = statisticsTracker.getTotalProcessesCreated();

            successRateLabel.setText(String.format("Success Rate: %.1f%%", successRate));
            throughputLabel.setText(String.format("Throughput: %.2f processes/cycle", throughput));
            cpuUsageLabel.setText(String.format("CPU Usage: %d%%", cpuUsage));
            avgWaitingLabel.setText(String.format("Avg Waiting: %.2f cycles", avgWaiting));
            deadlineMissLabel.setText("Deadline Misses: " + deadlineMisses);
            processCountLabel.setText("Total Processes: " + totalProcesses);

            if (miniChartPanel != null) {
                miniChartPanel.updateData(
                    statisticsTracker.getCPUUtilizationHistory(),
                    statisticsTracker.getSuccessRateHistory()
                );
            }

            // También puedes usar el reporte corto para debugging
            System.out.println("Stats: " + statisticsTracker.generateShortReport());
        }

        // Actualizar estadísticas de interrupciones
        if (interruptHandler != null) {
            updateInterruptStats();
        }
    }

    private void updateInterruptStats() {
        // Crear estadísticas más completas de interrupciones
        String interruptStats = String.format(
            "Interrupts: %d pending, %d processed",
            interruptHandler.getPendingInterruptCount(),
            interruptHandler.getTotalProcessedInterrupts() // Necesitarías un método para esto
        );

        // Buscar la etiqueta y actualizarla
        findAndUpdateLabel("Interrupts:", interruptStats);
    }

    private void findAndUpdateLabel(String containsText, String newText) {
        for (Component comp : getComponents()) {
            if (comp instanceof JPanel) {
                findLabelInPanel((JPanel) comp, containsText, newText);
            }
        }
    }

    private void findLabelInPanel(JPanel panel, String containsText, String newText) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getText().contains(containsText)) {
                    label.setText(newText);
                    return;
                }
            } else if (comp instanceof JPanel) {
                findLabelInPanel((JPanel) comp, containsText, newText);
            }
        }
    }
    
    private void startSimulation() {
        if (simulationEngine == null) return;
        
        updateCycleDuration();
        simulationEngine.start();
        simulationRunning = true;
        simulationTimer.start();
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        tickButton.setEnabled(false);
        logEvent("Simulation started");
    }
    
    private void pauseSimulation() {
        simulationRunning = false;
        simulationTimer.stop();
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        tickButton.setEnabled(true);
        if (simulationEngine != null) {
            simulationEngine.pause();
        }
        logEvent("Simulation paused");
    }

    private void stepSimulationOnce() {
        if (simulationEngine == null) return;
        if (simulationRunning) return;
        if (!simulationEngine.isPaused()) return;

        simulationEngine.stepOneCycle();
        updateAllDisplays();
        logEvent("Manual tick executed");
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
        cycleDurationSpinner.setValue(simulationEngine.getCycleDurationMs());
        updateCycleDuration();
        
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        tickButton.setEnabled(false);
        
        // Limpiar GUI
        clearAllTables();
        logArea.setText("");
        clockLabel.setText("MISSION CLOCK: Cycle 0");
        cpuModeLabel.setText("CPU MODE: IDLE");
        
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
        updateRoundRobinQuantum();
    }

    private void updateRoundRobinQuantum() {
        if (simulationEngine == null) return;
        String algorithm = (String) algorithmComboBox.getSelectedItem();
        if (!"Round Robin".equals(algorithm)) return;

        int quantum = (Integer) quantumSpinner.getValue();
        simulationEngine.setRoundRobinQuantum(quantum);
    }

    private void updateCycleDuration() {
        if (simulationEngine == null) return;
        int cycleMs = (Integer) cycleDurationSpinner.getValue();
        simulationEngine.setCycleDurationMs(cycleMs);

        int refreshMs = Math.max(50, Math.min(500, cycleMs / 2));
        if (simulationTimer != null) {
            simulationTimer.setDelay(refreshMs);
            simulationTimer.setInitialDelay(refreshMs);
        }
    }
    
    private void clearAllTables() {
        ((DefaultTableModel) readyQueueTable.getModel()).setRowCount(0);
        ((DefaultTableModel) blockedQueueTable.getModel()).setRowCount(0);
        ((DefaultTableModel) suspendedQueueTable.getModel()).setRowCount(0);
        ((DefaultTableModel) terminatedQueueTable.getModel()).setRowCount(0);
        
        // Resetear tabla de proceso en ejecución
        DefaultTableModel runningModel = (DefaultTableModel) runningProcessTable.getModel();
        runningModel.setValueAt("None", 0, 1);
        runningModel.setValueAt("Idle", 1, 1);
        runningModel.setValueAt("IDLE", 2, 1);
        runningModel.setValueAt("0/0", 3, 1);
        runningModel.setValueAt("∞", 4, 1);
        runningModel.setValueAt("0/0", 5, 1);
        runningModel.setValueAt("-", 6, 1);
        runningModel.setValueAt("IDLE", 7, 1);
    }
    
    private void logEvent(String message) {
        int currentCycle = simulationEngine != null ? simulationEngine.getCurrentCycle() : 0;
        logArea.append("[" + currentCycle + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    private void showPerformanceCharts() {
        if (statisticsTracker == null) return;

        JDialog dialog = new JDialog(this, "Performance Charts", false);
        dialog.setLayout(new BorderLayout(8, 8));

        JLabel header = new JLabel(
            "CPU Utilization (naranja) vs Mission Success Rate (verde)",
            SwingConstants.CENTER
        );
        header.setFont(new Font("Arial", Font.BOLD, 13));

        PerformanceChartPanel bigChart = new PerformanceChartPanel();
        bigChart.setPreferredSize(new Dimension(760, 360));
        bigChart.updateData(
            statisticsTracker.getCPUUtilizationHistory(),
            statisticsTracker.getSuccessRateHistory()
        );

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(bigChart, BorderLayout.CENTER);

        Timer chartTimer = new Timer(500, e -> bigChart.updateData(
            statisticsTracker.getCPUUtilizationHistory(),
            statisticsTracker.getSuccessRateHistory()
        ));
        chartTimer.start();

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                chartTimer.stop();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                chartTimer.stop();
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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

    private static class PerformanceChartPanel extends JPanel {
        private int[] cpuData = new int[0];
        private int[] successData = new int[0];

        public void updateData(LinkedList<Integer> cpuHistory, LinkedList<Integer> successHistory) {
            int cpuSize = cpuHistory != null ? cpuHistory.size() : 0;
            int successSize = successHistory != null ? successHistory.size() : 0;
            int size = Math.max(cpuSize, successSize);

            if (size <= 0) {
                cpuData = new int[0];
                successData = new int[0];
                repaint();
                return;
            }

            cpuData = new int[size];
            successData = new int[size];

            for (int i = 0; i < size; i++) {
                int cpu = (cpuHistory != null && i < cpuHistory.size()) ? cpuHistory.get(i) : 0;
                int success = (successHistory != null && i < successHistory.size()) ? successHistory.get(i) : 100;

                cpuData[i] = Math.max(0, Math.min(100, cpu));
                successData[i] = Math.max(0, Math.min(100, success));
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int left = 40;
            int right = 16;
            int top = 20;
            int bottom = 28;

            g2.setColor(new Color(245, 245, 245));
            g2.fillRect(0, 0, w, h);

            int plotW = Math.max(10, w - left - right);
            int plotH = Math.max(10, h - top - bottom);

            // Ejes y grilla
            g2.setColor(new Color(220, 220, 220));
            for (int p = 0; p <= 5; p++) {
                int y = top + (plotH * p / 5);
                g2.drawLine(left, y, left + plotW, y);
            }

            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(left, top, plotW, plotH);

            // Etiquetas de escala Y
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            for (int p = 0; p <= 5; p++) {
                int value = 100 - (p * 20);
                int y = top + (plotH * p / 5);
                g2.drawString(value + "%", 6, y + 4);
            }

            drawSeries(g2, cpuData, new Color(255, 140, 0), left, top, plotW, plotH);
            drawSeries(g2, successData, new Color(0, 150, 0), left, top, plotW, plotH);

            // Leyenda
            g2.setColor(new Color(255, 140, 0));
            g2.fillRect(left, h - 16, 10, 10);
            g2.setColor(Color.BLACK);
            g2.drawString("CPU", left + 14, h - 8);

            g2.setColor(new Color(0, 150, 0));
            g2.fillRect(left + 60, h - 16, 10, 10);
            g2.setColor(Color.BLACK);
            g2.drawString("Success", left + 74, h - 8);

            g2.dispose();
        }

        private void drawSeries(Graphics2D g2, int[] series, Color color,
                                int left, int top, int plotW, int plotH) {
            if (series == null || series.length == 0) return;

            g2.setColor(color);
            int prevX = left;
            int prevY = top + plotH - (series[0] * plotH / 100);

            if (series.length == 1) {
                g2.fillOval(prevX - 2, prevY - 2, 4, 4);
                return;
            }

            for (int i = 1; i < series.length; i++) {
                int x = left + (plotW * i / (series.length - 1));
                int y = top + plotH - (series[i] * plotH / 100);
                g2.drawLine(prevX, prevY, x, y);
                prevX = x;
                prevY = y;
            }
        }
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
