
import ij.IJ;
import ij.gui.ImageWindow;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import mmcorej.CMMCore;
import mmcorej.CharVector;
import mmcorej.DeviceType;
import mmcorej.StrVector;
import org.micromanager.MMStudio;
import org.micromanager.acquisition.AcquisitionEngine;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.ColorEditor;
import org.micromanager.utils.ColorRenderer;
import org.micromanager.utils.FileDialogs;
import org.micromanager.utils.JavaUtils;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author Hugo Pereira
 */
public class JDialFinal extends javax.swing.JFrame {
    private final MMStudio gui_;
    private final mmcorej.CMMCore mmc_;
    private final boolean  DEBUG = true;
    private acquisition ACQR_;
    private RotationControl2 RC_;
    private ShutterControl ShC_;
    private AcquisitionEngine acq_;
    private Final plugin_;
    //private final Preferences prefs_;
    private String DIALOG_POSITION = "dialogPosition";     
    private Color[] colors={Color.green,Color.yellow,Color.orange,Color.red, Color.magenta, Color.cyan};
    String[] Excitation_filters=new String[4];
    String[] Emission_filters={ "535/70", "580/25", "620/90", "640/25", "SP 700", "700/75" };
    String[] Intensity={"0","10","20","30","40","50","60","70","80","90","100"};
    String[] Angles;
    String Channels[]=new String [6];
    private String StateDev_;
    private String ShutterDev_;
    private Preferences prefs_;    
    
    private FilterControl FC_;
    private String StateDev_FW_;
    private String ShutterDev_FW_;
    
    private String Master = Global.Master;  

    private int frameXPos_ = 100;
    private int frameYPos_ = 100;

    private static final String FRAMEXPOS = "FRAMEXPOS";
    private static final String FRAMEYPOS = "FRAMEYPOS";
   
    public void setUpComboColumn(JTable table,
                                 TableColumn sportColumn,
                                JComboBox combobox){

        //Set up the editor for the sport cells.
        sportColumn.setCellEditor(new DefaultCellEditor(combobox));
 
        //Set up tool tips for the sport cells.
        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        sportColumn.setCellRenderer(renderer);
    }

    class MyTableModel extends AbstractTableModel {
        private String[] columnNames = {"Name",
                                        "Excitation",
                                        "Emission",
                                        "Exposure",
                                        "Intensity",
                                        "Color",
                                        "ON/OFF"};
        private Object[][] data = {
            {"GFP", Excitation_filters[0],
             Emission_filters[0], new Integer(100),Intensity[0], colors[0], new Boolean(false)},
            {"RFP", Excitation_filters[1],
             Emission_filters[1], new Integer(100),Intensity[1], colors[1], new Boolean(false)},
            {"YFP", Excitation_filters[0],
             Emission_filters[2], new Integer(100),Intensity[2], colors[2], new Boolean(false)},
            {"mCherry", Excitation_filters[1],
             Emission_filters[3], new Integer(100),Intensity[3], colors[3], new Boolean(false)},
            {"FarRed", Excitation_filters[0],
             Emission_filters[4], new Integer(100),Intensity[4], colors[4], new Boolean(false)},
            {"FarRed", Excitation_filters[0],
             Emission_filters[5], new Integer(100),Intensity[5], colors[5], new Boolean(false)}
        };
        

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            if (col < 1) {
                return false;
            } else {
                return true;
            }
        }

        public void setValueAt(Object value, int row, int col) {
            if (DEBUG) {
                System.out.println("Setting value at " + row + "," + col
                                   + " to " + value
                                   + " (an instance of "
                                   + value.getClass() + ")");
            }

            data[row][col] = value;
            fireTableCellUpdated(row, col);

            if (DEBUG) {
                System.out.println("New value of data:");
                printDebugData();
            }
        }

        private void printDebugData() {
            int numRows = getRowCount();
            int numCols = getColumnCount();

            for (int i=0; i < numRows; i++) {
                System.out.print("    row " + i + ":");
                for (int j=0; j < numCols; j++) {
                    System.out.print("  " + data[i][j]);
                }
                System.out.println();
            }
            System.out.println("--------------------------");
        }
    }

    /**
     * Creates new form NewJDialog
     */
    public JDialFinal(Final plugin,ScriptInterface gui, AcquisitionEngine acq) {
        setTitle("OPenT v1.170927");
        gui_ = (MMStudio)gui;
        mmc_ = gui_.getMMCore();
        acq_ = acq;
        FC_ = new FilterControl(mmc_, StateDev_FW_, ShutterDev_FW_, ShutterDev_);
        RC_ = new RotationControl2(mmc_, StateDev_, this);
        ShC_ = new ShutterControl(mmc_, StateDev_, ShutterDev_, this);
        initComponents();
        setLocation(frameXPos_, frameYPos_);
        ACQR_ = new acquisition(mmc_,this, acq_, gui_, ShC_, FC_, RC_);
        prefs_ = Preferences.userNodeForPackage(this.getClass());
        plugin_ = plugin;
        setBackground(gui_.getBackgroundColor());
        
        try{
        mmc_.setProperty("SRotation", "CommandTerminator", "\r");
        mmc_.setProperty("SRotation", "ResponseTerminator", "\r");
        }catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("Error in Initialize");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fc = new javax.swing.JFileChooser();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        Acquisition = new javax.swing.JPanel();
        RotationControlPanel = new javax.swing.JPanel();
        RotationControlLabel = new javax.swing.JLabel();
        AngleLabel = new javax.swing.JLabel();
        LeftButton = new javax.swing.JButton();
        AnglePositionField = new javax.swing.JTextField();
        RightButton = new javax.swing.JButton();
        StepAngleLabel = new javax.swing.JLabel();
        DelayLabel = new javax.swing.JLabel();
        StepAngleComboBox = new javax.swing.JComboBox();
        DelayField = new javax.swing.JTextField();
        MakeZeroButton = new javax.swing.JButton();
        LineCheckBox = new javax.swing.JCheckBox();
        RotationPanel = new javax.swing.JPanel();
        RotationSampleLabel = new javax.swing.JLabel();
        RotationLabel = new javax.swing.JLabel();
        ShutterRotationCheckBox = new javax.swing.JCheckBox();
        RotationComboBox = new javax.swing.JComboBox();
        RotationMotorComboBox = new javax.swing.JComboBox();
        TimeLapsePanel = new javax.swing.JPanel();
        TimeLapseCheckBox = new javax.swing.JCheckBox();
        NumberLabel = new javax.swing.JLabel();
        IntervalLabel = new javax.swing.JLabel();
        TimeFrames = new javax.swing.JSpinner();
        IntervalTime = new javax.swing.JTextField();
        IntervalUnits = new javax.swing.JComboBox();
        AcquireButton = new javax.swing.JButton();
        ShutterButton = new javax.swing.JButton();
        CheckAlignButton = new javax.swing.JButton();
        Ilumination = new javax.swing.JPanel();
        Excitation_label = new javax.swing.JLabel();
        Emission_label = new javax.swing.JLabel();
        try{
            String current= System.getProperty("user.dir");
            File file = new File(current + "\\mmplugins\\Config_EX_file_OPT.txt");
            //create FileReader object from File object
            FileReader fr = new FileReader(file);
            //create BufferedReader object from FileReader to read file line by line
            BufferedReader reader = new BufferedReader(fr);
            //// READ EXCITATION FILTERS ////
            String line = reader.readLine();
            for(int nn=0;nn<4;nn++){
                line = reader.readLine();
                Excitation_filters[nn]=line;
            }
            //// READ EMISSION FILTERS ////
            line = reader.readLine();
            for(int nn=0;nn<6;nn++){
                line = reader.readLine();
                Emission_filters[nn]=line;
            }
            line= reader.readLine();
            line= reader.readLine();
            for(int nn=0;nn<6;nn++){
                line = reader.readLine();
                Channels[nn]=line;
            }

        }catch (FileNotFoundException e) {
            MessageTextArea.setText("Error in Config_Ex_file");
        } catch (IOException e) {
            MessageTextArea.setText("Error in Config_Ex_file");
        }

        String[] CH0=Channels[0].split(":");
        String[] CH1=Channels[1].split(":");
        String[] CH2=Channels[2].split(":");
        String[] CH3=Channels[3].split(":");
        String[] CH4=Channels[4].split(":");
        String[] CH5=Channels[5].split(":");
        Excitation_ComboBox = new javax.swing.JComboBox(Excitation_filters);
        Emission_ComboBox = new javax.swing.JComboBox(Emission_filters);
        Intensity_label = new javax.swing.JLabel();
        Intensity_ComboBox = new javax.swing.JComboBox(Intensity);
        ChannelControlPanel = new javax.swing.JPanel();
        Channel_CheckBox = new javax.swing.JCheckBox();
        ChannelScrollPane = new javax.swing.JScrollPane();
        Channeltable = new JTable(new MyTableModel());
        SaveImagesPanel = new javax.swing.JPanel();
        SaveCheckBox = new javax.swing.JCheckBox();
        RootLabel = new javax.swing.JLabel();
        FileLabel = new javax.swing.JLabel();
        RootDirectoryField = new javax.swing.JTextField();
        FileNameField = new javax.swing.JTextField();
        DirectoryButton = new javax.swing.JButton();
        Message_Pannel = new javax.swing.JPanel();
        MessageLabel = new javax.swing.JLabel();
        Message_scroll_pane = new javax.swing.JScrollPane();
        MessageTextArea = new javax.swing.JTextArea();
        Settings = new javax.swing.JPanel();
        LedPanel = new javax.swing.JPanel();
        LedLabel = new javax.swing.JLabel();
        LedOnOffLabel = new javax.swing.JLabel();
        LedIntLabel = new javax.swing.JLabel();
        LedCheckBox1 = new javax.swing.JCheckBox();
        LedCheckBox2 = new javax.swing.JCheckBox();
        LedCheckBox3 = new javax.swing.JCheckBox();
        LedSlider1 = new javax.swing.JSlider();
        LedCheckBox4 = new javax.swing.JCheckBox();
        LedSlider2 = new javax.swing.JSlider();
        LedSlider3 = new javax.swing.JSlider();
        LedSlider4 = new javax.swing.JSlider();
        MotorPanel = new javax.swing.JPanel();
        MotorLabel = new javax.swing.JLabel();
        MotorOnOffLabel = new javax.swing.JLabel();
        MotorCheckBox1 = new javax.swing.JCheckBox();
        MotorCheckBox2 = new javax.swing.JCheckBox();
        MotorCheckBox3 = new javax.swing.JCheckBox();
        MotorCheckBox4 = new javax.swing.JCheckBox();
        StepsLabel = new javax.swing.JLabel();
        StepsComboBox1 = new javax.swing.JComboBox();
        StepsComboBox2 = new javax.swing.JComboBox();
        StepsComboBox3 = new javax.swing.JComboBox();
        StepsComboBox4 = new javax.swing.JComboBox();
        StepTypeLabel = new javax.swing.JLabel();
        StepTypeComboBox1 = new javax.swing.JComboBox();
        StepTypeComboBox2 = new javax.swing.JComboBox();
        StepTypeComboBox3 = new javax.swing.JComboBox();
        StepTypeComboBox4 = new javax.swing.JComboBox();
        VelocityLabel = new javax.swing.JLabel();
        MotorSlider1 = new javax.swing.JSlider();
        MotorSlider2 = new javax.swing.JSlider();
        MotorSlider3 = new javax.swing.JSlider();
        MotorSlider4 = new javax.swing.JSlider();
        jScrollPane1 = new javax.swing.JScrollPane();
        SettingsTextArea = new javax.swing.JTextArea();
        HelpButton = new javax.swing.JButton();
        LedButton = new javax.swing.JButton();
        MotorButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(new java.awt.Dimension(600, 413));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                onWindowClosing(evt);
            }
        });

        RotationControlPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        RotationControlLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        RotationControlLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        RotationControlLabel.setText("Rotation Control");
        RotationControlLabel.setToolTipText("<html>Manual rotation control:<br>use it to rotate the sample before starting acquisition!<html>");
        RotationControlLabel.setPreferredSize(new java.awt.Dimension(94, 14));

        AngleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        AngleLabel.setText("Angle");
        AngleLabel.setPreferredSize(new java.awt.Dimension(40, 20));

        LeftButton.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        LeftButton.setText("<");
        LeftButton.setToolTipText("Rotate to the left");
        LeftButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LeftButtonActionPerformed(evt);
            }
        });

        AnglePositionField.setText("180.00");
        AnglePositionField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AnglePositionFieldActionPerformed(evt);
            }
        });

        RightButton.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        RightButton.setText(">");
        RightButton.setToolTipText("Rotate to the right");
        RightButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RightButtonActionPerformed(evt);
            }
        });

        StepAngleLabel.setText("Angle Step");

        DelayLabel.setText("Delay (ms)");
        DelayLabel.setPreferredSize(new java.awt.Dimension(52, 20));

        StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0","45", "90", "180","360","18","9","1.8","0.9","0.45","0.225","0.1125" }));
        StepAngleComboBox.setToolTipText("Angle rotation value");
        StepAngleComboBox.setPreferredSize(new java.awt.Dimension(52, 20));
        StepAngleComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepAngleComboBoxActionPerformed(evt);
            }
        });

        DelayField.setText("125");
        DelayField.setToolTipText("Delay between channels");

        MakeZeroButton.setText("Make Zero");
        MakeZeroButton.setToolTipText("Reset the Angle value");
        MakeZeroButton.setPreferredSize(new java.awt.Dimension(84, 23));
        MakeZeroButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MakeZeroButtonActionPerformed(evt);
            }
        });

        LineCheckBox.setText("Line");
        LineCheckBox.setToolTipText("Makes a vertical line in the middle of the image");
        LineCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        LineCheckBox.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        LineCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LineCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout RotationControlPanelLayout = new javax.swing.GroupLayout(RotationControlPanel);
        RotationControlPanel.setLayout(RotationControlPanelLayout);
        RotationControlPanelLayout.setHorizontalGroup(
            RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RotationControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(RotationControlLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(AngleLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RotationControlPanelLayout.createSequentialGroup()
                        .addComponent(LeftButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 21, Short.MAX_VALUE)
                        .addComponent(AnglePositionField, javax.swing.GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                        .addComponent(RightButton))
                    .addComponent(MakeZeroButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(RotationControlPanelLayout.createSequentialGroup()
                        .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(DelayLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(StepAngleComboBox, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(StepAngleLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(56, 56, 56)
                        .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(DelayField, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
                            .addComponent(LineCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        RotationControlPanelLayout.setVerticalGroup(
            RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RotationControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(RotationControlLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(AngleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(AnglePositionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(LeftButton)
                    .addComponent(RightButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(MakeZeroButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16)
                .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(RotationControlPanelLayout.createSequentialGroup()
                        .addComponent(StepAngleLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(StepAngleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(DelayLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(DelayField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(LineCheckBox))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        RotationPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        RotationSampleLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        RotationSampleLabel.setText("Rotation");
        RotationSampleLabel.setToolTipText("Rotation settings for acquisition");

        RotationLabel.setText("Rotation Angle");

        ShutterRotationCheckBox.setText("Shutter?");
        ShutterRotationCheckBox.setToolTipText("Close shutter after acquiring a full rotation");
        ShutterRotationCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShutterRotationCheckBoxActionPerformed(evt);
            }
        });

        if("200".equals(StepsComboBox1.getSelectedItem())){
            if("FULL".equals(StepTypeComboBox1.getSelectedItem())){
                Angles = new String[]{"45", "90", "180","360","18","9","1.8"};
            }
            else if("HALF".equals(StepTypeComboBox1.getSelectedItem())){
                Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9"};
            }
            else if("QUARTER".equals(StepTypeComboBox1.getSelectedItem())){
                Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45"};
            }
            else if("EIGHTH".equals(StepTypeComboBox1.getSelectedItem())){
                Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45","0.225"};
            }
        }
        else if("400".equals(StepsComboBox1.getSelectedItem())){
            if("FULL".equals(StepTypeComboBox1.getSelectedItem())){
                Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9"};
            }
            else if("HALF".equals(StepTypeComboBox1.getSelectedItem())){
                Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45"};
            }
            else if("QUARTER".equals(StepTypeComboBox1.getSelectedItem())){
                Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45","0.225"};
            }
            else if("EIGHTH".equals(StepTypeComboBox1.getSelectedItem())){
                Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45","0.225","0.1125"};
            }
        }
        else{
            Angles = new String[]{"1.8","9","18","45", "90", "180","360"};
        }
        RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
        RotationComboBox.setToolTipText("Angle desired for acquisition");
        RotationComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RotationComboBoxActionPerformed(evt);
            }
        });

        StrVector zDrives2 = mmc_.getLoadedDevicesOfType(DeviceType.StageDevice);
        //zDrives2.add("Arduino_SR");
        zDrives2.add("Motor_1");
        zDrives2.add("Motor_2");
        zDrives2.add("Motor_3");
        zDrives2.add("Motor_4");
        String[] fd3=zDrives2.toArray();
        RotationMotorComboBox.setModel(new javax.swing.DefaultComboBoxModel(fd3));
        RotationMotorComboBox.setToolTipText("Rotation motor selected");
        RotationMotorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RotationMotorComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout RotationPanelLayout = new javax.swing.GroupLayout(RotationPanel);
        RotationPanel.setLayout(RotationPanelLayout);
        RotationPanelLayout.setHorizontalGroup(
            RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RotationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(RotationSampleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(RotationPanelLayout.createSequentialGroup()
                        .addGroup(RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(ShutterRotationCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(RotationLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(RotationComboBox, 0, 56, Short.MAX_VALUE)
                            .addComponent(RotationMotorComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        RotationPanelLayout.setVerticalGroup(
            RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RotationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(RotationSampleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(RotationLabel)
                    .addComponent(RotationComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ShutterRotationCheckBox)
                    .addComponent(RotationMotorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        TimeLapsePanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        TimeLapsePanel.setToolTipText("");

        TimeLapseCheckBox.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        TimeLapseCheckBox.setText("Time Lapse");
        TimeLapseCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TimeLapseCheckBoxActionPerformed(evt);
            }
        });

        NumberLabel.setText("Number");

        IntervalLabel.setText("Interval");

        TimeFrames.setToolTipText("Define number of timepoints");
        TimeFrames.setEnabled(false);

        IntervalTime.setText("100");
        IntervalTime.setToolTipText("Define time between time points");
        IntervalTime.setEnabled(false);
        IntervalTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IntervalTimeActionPerformed(evt);
            }
        });

        IntervalUnits.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ms", "s", "min" }));
        IntervalUnits.setToolTipText("Define time measurent");
        IntervalUnits.setEnabled(false);

        javax.swing.GroupLayout TimeLapsePanelLayout = new javax.swing.GroupLayout(TimeLapsePanel);
        TimeLapsePanel.setLayout(TimeLapsePanelLayout);
        TimeLapsePanelLayout.setHorizontalGroup(
            TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TimeLapsePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(TimeLapsePanelLayout.createSequentialGroup()
                        .addGroup(TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(TimeLapseCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE)
                            .addGroup(TimeLapsePanelLayout.createSequentialGroup()
                                .addComponent(IntervalLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(IntervalTime)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(IntervalUnits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(TimeLapsePanelLayout.createSequentialGroup()
                        .addComponent(NumberLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(TimeFrames)))
                .addContainerGap())
        );
        TimeLapsePanelLayout.setVerticalGroup(
            TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TimeLapsePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(TimeLapseCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(NumberLabel)
                    .addComponent(TimeFrames, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(IntervalLabel)
                        .addComponent(IntervalTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(IntervalUnits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        AcquireButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        AcquireButton.setText("Acquire!");
        AcquireButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AcquireButtonActionPerformed(evt);
            }
        });

        ShutterButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        ShutterButton.setText("Shutter");
        ShutterButton.setToolTipText("Turn on or off the LED");
        ShutterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShutterButtonActionPerformed(evt);
            }
        });

        CheckAlignButton.setText("Check Alignment");
        CheckAlignButton.setToolTipText("Take 2 images (0º and 180º) in order to check sample alignment");
        CheckAlignButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CheckAlignButtonActionPerformed(evt);
            }
        });

        Excitation_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        Excitation_label.setText("Excitation");

        Emission_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        Emission_label.setText("Emission");

        /*
        Excitation_ComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "473", "Item 2", "Item 3", "Item 4" }));
        */
        Excitation_ComboBox.setToolTipText("Set excitation LED");
        Excitation_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Excitation_ComboBoxActionPerformed(evt);
            }
        });

        /*
        Emission_ComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "535/70", "Item 2", "Item 3", "Item 4" }));
        */
        Emission_ComboBox.setToolTipText("Set emission filter");
        Emission_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Emission_ComboBoxActionPerformed(evt);
            }
        });

        Intensity_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        Intensity_label.setText("Intensity");

        /*
        Intensity_ComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "100", "Item 2", "Item 3", "Item 4" }));
        */
        Intensity_ComboBox.setToolTipText("Set intensity values for current LED");
        Intensity_ComboBox.setMinimumSize(new java.awt.Dimension(59, 20));
        Intensity_ComboBox.setPreferredSize(new java.awt.Dimension(59, 20));
        Intensity_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Intensity_ComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout IluminationLayout = new javax.swing.GroupLayout(Ilumination);
        Ilumination.setLayout(IluminationLayout);
        IluminationLayout.setHorizontalGroup(
            IluminationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(IluminationLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(IluminationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Excitation_ComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Excitation_label, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Intensity_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(IluminationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Emission_label, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(IluminationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(Emission_ComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Intensity_ComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(36, 36, 36))
        );
        IluminationLayout.setVerticalGroup(
            IluminationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(IluminationLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(IluminationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Excitation_label)
                    .addComponent(Emission_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(IluminationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Excitation_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Emission_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(IluminationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(Intensity_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Intensity_label, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        //JComponent ChannelTable = new Mytable();
        ChannelControlPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        ChannelControlPanel.setEnabled(false);
        //ChannelTable.setOpaque(true);
        //ChannelControlPanel.add(ChannelTable);

        Channel_CheckBox.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        Channel_CheckBox.setText("Channels");
        Channel_CheckBox.setToolTipText("Check for multiple channel acquisiton");
        Channel_CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Channel_CheckBoxActionPerformed(evt);
            }
        });

        Channeltable.setDefaultRenderer(Color.class,
            new ColorRenderer(true));
        Channeltable.setDefaultEditor(Color.class,
            new ColorEditor((AbstractTableModel) Channeltable.getModel(), 5));
        //Fiddle with the Sport column's cell editors/renderers.
        JComboBox ExcitationComboBoxT = new JComboBox(Excitation_filters);
        JComboBox EmissionComboBoxT = new JComboBox(Emission_filters);
        JComboBox IntensityComboBoxT = new JComboBox(Intensity);
        setUpComboColumn(Channeltable, Channeltable.getColumnModel().getColumn(1), ExcitationComboBoxT);
        setUpComboColumn(Channeltable, Channeltable.getColumnModel().getColumn(2), EmissionComboBoxT);
        setUpComboColumn(Channeltable, Channeltable.getColumnModel().getColumn(4), IntensityComboBoxT);
        Channeltable.setEnabled(false);
        ChannelScrollPane.setViewportView(Channeltable);

        javax.swing.GroupLayout ChannelControlPanelLayout = new javax.swing.GroupLayout(ChannelControlPanel);
        ChannelControlPanel.setLayout(ChannelControlPanelLayout);
        ChannelControlPanelLayout.setHorizontalGroup(
            ChannelControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ChannelControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Channel_CheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ChannelScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        ChannelControlPanelLayout.setVerticalGroup(
            ChannelControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ChannelControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Channel_CheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ChannelControlPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(ChannelScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        SaveImagesPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        SaveCheckBox.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        SaveCheckBox.setSelected(true);
        SaveCheckBox.setText("Save Images");
        SaveCheckBox.setToolTipText("");
        SaveCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveCheckBoxActionPerformed(evt);
            }
        });

        RootLabel.setText("Root Directory");

        FileLabel.setText("File name");

        String userdir = System.getProperty("user.home");
        RootDirectoryField.setText(userdir);
        RootDirectoryField.setPreferredSize(new java.awt.Dimension(120, 20));
        RootDirectoryField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RootDirectoryFieldActionPerformed(evt);
            }
        });

        FileNameField.setText("Sample");
        FileNameField.setPreferredSize(new java.awt.Dimension(120, 20));
        FileNameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileNameFieldActionPerformed(evt);
            }
        });

        DirectoryButton.setText("...");
        DirectoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DirectoryButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout SaveImagesPanelLayout = new javax.swing.GroupLayout(SaveImagesPanel);
        SaveImagesPanel.setLayout(SaveImagesPanelLayout);
        SaveImagesPanelLayout.setHorizontalGroup(
            SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SaveImagesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SaveCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(SaveImagesPanelLayout.createSequentialGroup()
                        .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(RootLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(FileLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(FileNameField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(RootDirectoryField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(DirectoryButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        SaveImagesPanelLayout.setVerticalGroup(
            SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SaveImagesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(SaveCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(RootDirectoryField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(DirectoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(RootLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(FileNameField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(FileLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(17, 17, 17))
        );

        Message_Pannel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        MessageLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        MessageLabel.setText("Messages");

        MessageTextArea.setColumns(20);
        MessageTextArea.setRows(5);
        Message_scroll_pane.setViewportView(MessageTextArea);

        javax.swing.GroupLayout Message_PannelLayout = new javax.swing.GroupLayout(Message_Pannel);
        Message_Pannel.setLayout(Message_PannelLayout);
        Message_PannelLayout.setHorizontalGroup(
            Message_PannelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Message_PannelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Message_PannelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(Message_PannelLayout.createSequentialGroup()
                        .addComponent(MessageLabel)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(Message_scroll_pane)))
        );
        Message_PannelLayout.setVerticalGroup(
            Message_PannelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Message_PannelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(MessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Message_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout AcquisitionLayout = new javax.swing.GroupLayout(Acquisition);
        Acquisition.setLayout(AcquisitionLayout);
        AcquisitionLayout.setHorizontalGroup(
            AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AcquisitionLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ChannelControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(AcquisitionLayout.createSequentialGroup()
                        .addComponent(RotationControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(TimeLapsePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(RotationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CheckAlignButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(ShutterButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(AcquireButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(Ilumination, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(AcquisitionLayout.createSequentialGroup()
                        .addComponent(SaveImagesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Message_Pannel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        AcquisitionLayout.setVerticalGroup(
            AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AcquisitionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(AcquisitionLayout.createSequentialGroup()
                        .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(RotationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(AcquisitionLayout.createSequentialGroup()
                                .addComponent(AcquireButton, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(ShutterButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CheckAlignButton)))
                        .addGap(7, 7, 7)
                        .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(TimeLapsePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(Ilumination, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(RotationControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ChannelControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SaveImagesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Message_Pannel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Acquisition", Acquisition);

        LedPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        LedLabel.setText("LED");

        LedOnOffLabel.setText("ON/OFF");

        LedIntLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LedIntLabel.setText("Intensity");

        LedCheckBox1.setText("1           ");
        LedCheckBox1.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        LedCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LedCheckBox1ActionPerformed(evt);
            }
        });

        LedCheckBox2.setText("2           ");
        LedCheckBox2.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        LedCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LedCheckBox2ActionPerformed(evt);
            }
        });

        LedCheckBox3.setText("3           ");
        LedCheckBox3.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        LedCheckBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LedCheckBox3ActionPerformed(evt);
            }
        });

        LedSlider1.setMajorTickSpacing(10);
        LedSlider1.setMinorTickSpacing(10);
        LedSlider1.setValue(0);
        LedSlider1.setEnabled(false);
        LedSlider1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                LedSlider1StateChanged(evt);
            }
        });

        LedCheckBox4.setText("4           ");
        LedCheckBox4.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        LedCheckBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LedCheckBox4ActionPerformed(evt);
            }
        });

        LedSlider2.setMajorTickSpacing(10);
        LedSlider2.setMinorTickSpacing(10);
        LedSlider2.setValue(0);
        LedSlider2.setEnabled(false);
        LedSlider2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                LedSlider2StateChanged(evt);
            }
        });

        LedSlider3.setMajorTickSpacing(10);
        LedSlider3.setMinorTickSpacing(10);
        LedSlider3.setValue(0);
        LedSlider3.setEnabled(false);
        LedSlider3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                LedSlider3StateChanged(evt);
            }
        });

        LedSlider4.setMajorTickSpacing(10);
        LedSlider4.setMinorTickSpacing(10);
        LedSlider4.setValue(0);
        LedSlider4.setEnabled(false);
        LedSlider4.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                LedSlider4StateChanged(evt);
            }
        });

        javax.swing.GroupLayout LedPanelLayout = new javax.swing.GroupLayout(LedPanel);
        LedPanel.setLayout(LedPanelLayout);
        LedPanelLayout.setHorizontalGroup(
            LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(LedPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(LedPanelLayout.createSequentialGroup()
                        .addComponent(LedLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(LedOnOffLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(LedCheckBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedCheckBox2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedCheckBox3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedCheckBox4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(LedSlider4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(LedSlider3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(LedSlider1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(LedIntLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE)
                    .addComponent(LedSlider2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );
        LedPanelLayout.setVerticalGroup(
            LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LedPanelLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(LedPanelLayout.createSequentialGroup()
                        .addGroup(LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(LedLabel)
                            .addComponent(LedOnOffLabel)
                            .addComponent(LedIntLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(LedCheckBox1))
                    .addComponent(LedSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(LedSlider2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedCheckBox2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(LedSlider3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedCheckBox3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(LedSlider4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedCheckBox4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        MotorPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        MotorLabel.setText("MOTOR");

        MotorOnOffLabel.setText("ON/OFF");

        MotorCheckBox1.setText("1           ");
        MotorCheckBox1.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        MotorCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MotorCheckBox1ActionPerformed(evt);
            }
        });

        MotorCheckBox2.setText("2           ");
        MotorCheckBox2.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        MotorCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MotorCheckBox2ActionPerformed(evt);
            }
        });

        MotorCheckBox3.setText("3           ");
        MotorCheckBox3.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        MotorCheckBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MotorCheckBox3ActionPerformed(evt);
            }
        });

        MotorCheckBox4.setText("4           ");
        MotorCheckBox4.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        MotorCheckBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MotorCheckBox4ActionPerformed(evt);
            }
        });

        StepsLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        StepsLabel.setText("STEPS");

        StepsComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "200", "400" }));
        StepsComboBox1.setEnabled(false);
        StepsComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepsComboBox1ActionPerformed(evt);
            }
        });

        StepsComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "200", "400" }));
        StepsComboBox2.setEnabled(false);
        StepsComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepsComboBox2ActionPerformed(evt);
            }
        });

        StepsComboBox3.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "200", "400" }));
        StepsComboBox3.setEnabled(false);
        StepsComboBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepsComboBox3ActionPerformed(evt);
            }
        });

        StepsComboBox4.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "200", "400" }));
        StepsComboBox4.setEnabled(false);
        StepsComboBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepsComboBox4ActionPerformed(evt);
            }
        });

        StepTypeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        StepTypeLabel.setText("STEPTYPE");

        StepTypeComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "FULL", "HALF", "QUARTER", "EIGHTH" }));
        StepTypeComboBox1.setEnabled(false);
        StepTypeComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepTypeComboBox1ActionPerformed(evt);
            }
        });

        StepTypeComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "FULL", "HALF", "QUARTER", "EIGHTH" }));
        StepTypeComboBox2.setEnabled(false);
        StepTypeComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepTypeComboBox2ActionPerformed(evt);
            }
        });

        StepTypeComboBox3.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "FULL", "HALF", "QUARTER", "EIGHTH" }));
        StepTypeComboBox3.setEnabled(false);
        StepTypeComboBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepTypeComboBox3ActionPerformed(evt);
            }
        });

        StepTypeComboBox4.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "FULL", "HALF", "QUARTER", "EIGHTH" }));
        StepTypeComboBox4.setEnabled(false);
        StepTypeComboBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepTypeComboBox4ActionPerformed(evt);
            }
        });

        VelocityLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        VelocityLabel.setText("VELOCITY");

        MotorSlider1.setMajorTickSpacing(10);
        MotorSlider1.setMaximum(99);
        MotorSlider1.setMinorTickSpacing(10);
        MotorSlider1.setPaintTicks(true);
        MotorSlider1.setEnabled(false);
        MotorSlider1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                MotorSlider1StateChanged(evt);
            }
        });

        MotorSlider2.setMajorTickSpacing(10);
        MotorSlider2.setMaximum(99);
        MotorSlider2.setMinorTickSpacing(10);
        MotorSlider2.setPaintTicks(true);
        MotorSlider2.setEnabled(false);
        MotorSlider2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                MotorSlider2StateChanged(evt);
            }
        });

        MotorSlider3.setMajorTickSpacing(10);
        MotorSlider3.setMaximum(99);
        MotorSlider3.setMinorTickSpacing(10);
        MotorSlider3.setPaintTicks(true);
        MotorSlider3.setEnabled(false);
        MotorSlider3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                MotorSlider3StateChanged(evt);
            }
        });

        MotorSlider4.setMajorTickSpacing(10);
        MotorSlider4.setMaximum(99);
        MotorSlider4.setMinorTickSpacing(10);
        MotorSlider4.setPaintTicks(true);
        MotorSlider4.setEnabled(false);
        MotorSlider4.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                MotorSlider4StateChanged(evt);
            }
        });

        javax.swing.GroupLayout MotorPanelLayout = new javax.swing.GroupLayout(MotorPanel);
        MotorPanel.setLayout(MotorPanelLayout);
        MotorPanelLayout.setHorizontalGroup(
            MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MotorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(MotorPanelLayout.createSequentialGroup()
                        .addComponent(MotorLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(MotorOnOffLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(MotorCheckBox4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(MotorCheckBox2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(MotorCheckBox1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(MotorCheckBox3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 11, Short.MAX_VALUE)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(StepsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepsComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepsComboBox3, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(MotorPanelLayout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(StepsComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(StepsComboBox2, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 11, Short.MAX_VALUE)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(StepTypeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepTypeComboBox1, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepTypeComboBox3, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepTypeComboBox4, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepTypeComboBox2, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 19, Short.MAX_VALUE)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(MotorSlider3, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
                    .addComponent(MotorSlider2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(MotorSlider4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(MotorSlider1, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
                    .addComponent(VelocityLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(11, Short.MAX_VALUE))
        );
        MotorPanelLayout.setVerticalGroup(
            MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MotorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(MotorLabel)
                    .addComponent(MotorOnOffLabel)
                    .addComponent(StepTypeLabel)
                    .addComponent(VelocityLabel)
                    .addComponent(StepsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(MotorCheckBox1)
                        .addComponent(StepsComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(StepTypeComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(MotorSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(MotorCheckBox2)
                        .addComponent(StepsComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(StepTypeComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(MotorSlider2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(MotorCheckBox3)
                        .addComponent(StepsComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(StepTypeComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(MotorSlider3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(MotorCheckBox4)
                        .addComponent(StepsComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(StepTypeComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(MotorSlider4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        SettingsTextArea.setColumns(20);
        SettingsTextArea.setRows(5);
        jScrollPane1.setViewportView(SettingsTextArea);

        HelpButton.setText("HELP");
        HelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                HelpButtonActionPerformed(evt);
            }
        });

        LedButton.setText("LED INFO");
        LedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LedButtonActionPerformed(evt);
            }
        });

        MotorButton.setText("MOTOR");
        MotorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MotorButtonActionPerformed(evt);
            }
        });

        jButton1.setText("SEQUENCE");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("RESET");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout SettingsLayout = new javax.swing.GroupLayout(Settings);
        Settings.setLayout(SettingsLayout);
        SettingsLayout.setHorizontalGroup(
            SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(SettingsLayout.createSequentialGroup()
                        .addGroup(SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(LedButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(MotorButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(HelpButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1))
                    .addGroup(SettingsLayout.createSequentialGroup()
                        .addComponent(LedPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(MotorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        SettingsLayout.setVerticalGroup(
            SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(MotorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(SettingsLayout.createSequentialGroup()
                        .addComponent(HelpButton)
                        .addGap(18, 18, 18)
                        .addComponent(LedButton)
                        .addGap(18, 18, 18)
                        .addComponent(MotorButton)
                        .addGap(18, 18, 18)
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 88, Short.MAX_VALUE)
                        .addComponent(jButton2)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Settings", Settings);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addGap(6, 6, 6))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void IntervalTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IntervalTimeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_IntervalTimeActionPerformed

    private void ShutterRotationCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShutterRotationCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ShutterRotationCheckBoxActionPerformed

    private void RotationMotorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RotationMotorComboBoxActionPerformed
        // TODO add your handling code here:
        try{
            String Rotationdevice=(String) RotationMotorComboBox.getSelectedItem();
        if      ("Motor_1".equals(RotationMotorComboBox.getSelectedItem()) && MotorCheckBox1.isSelected()){
            mmc_.setProperty("SRotation", "Command", "MOTORROTATION=0;");
            MessageTextArea.setText(Rotationdevice); 
        }
        else if ("Motor_2".equals(RotationMotorComboBox.getSelectedItem()) && MotorCheckBox2.isSelected()){
            mmc_.setProperty("SRotation", "Command", "MOTORROTATION=1;");
            MessageTextArea.setText(Rotationdevice); 
        }
        else if ("Motor_3".equals(RotationMotorComboBox.getSelectedItem()) && MotorCheckBox3.isSelected()){
            mmc_.setProperty("SRotation", "Command", "MOTORROTATION=2;");
            MessageTextArea.setText(Rotationdevice); 
        }
        else if ("Motor_4".equals(RotationMotorComboBox.getSelectedItem()) && MotorCheckBox4.isSelected()){
            mmc_.setProperty("SRotation", "Command", "MOTORROTATION=3;");        
            MessageTextArea.setText(Rotationdevice); 
        }
        else{
            MessageTextArea.setText("Verify if the "+RotationMotorComboBox.getSelectedItem()+" is enable");
        }
        }      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error in Set Rotation Motor");
        }


        mmc_.sleep(500);
    }//GEN-LAST:event_RotationMotorComboBoxActionPerformed

    private void RootDirectoryFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RootDirectoryFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_RootDirectoryFieldActionPerformed

    private void FileNameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileNameFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_FileNameFieldActionPerformed

    private void SaveCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveCheckBoxActionPerformed
        // TODO add your handling code here:
        if (SaveCheckBox.isSelected()){  
                RootDirectoryField.setEnabled(true);
                RootLabel.setEnabled(true);
                DirectoryButton.setEnabled(true);
                FileNameField.setEnabled(true);
                FileLabel.setEnabled(true);
            }
            else{
                RootDirectoryField.setEnabled(false);
                RootLabel.setEnabled(false);
                DirectoryButton.setEnabled(false);
                FileNameField.setEnabled(false);
                FileLabel.setEnabled(false);
            }
    }//GEN-LAST:event_SaveCheckBoxActionPerformed

    private void AcquireButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AcquireButtonActionPerformed
        // TODO add your handling code here:
        Thread thread = new Thread(){
            public void run(){      
                ACQR_.Configure();
                try {
                    mmc_.setProperty("SRotation", "CommandTerminator", "\r");
                    mmc_.setProperty("SRotation", "ResponseTerminator", "\r");
                } catch (Exception ex) {
                    Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                    MessageTextArea.setText("Error in OPT Trigger");
                }
   //////////////// to verify the number of LED's ON 
                int led1 = LedCheckBox1.isSelected() ? 1:0;
                int led2 = LedCheckBox2.isSelected() ? 1:0;
                int led3 = LedCheckBox3.isSelected() ? 1:0;
                int led4 = LedCheckBox4.isSelected() ? 1:0;
                int ledt = led1+led2+led3+led4;
    
   ////////////////  ACQUISITION IN OPT Mode (Slices are angles)  //////////////////   
   
                try{
                    ACQR_.Configure();
                    if(ledt == ACQR_.channels){
                        ACQR_.OPT_Trig();
                    }
                    else{
                        MessageTextArea.setText("Check if LED CheckBox is equal to channels");
                    }
                } catch (Exception ex) {
                    Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                    MessageTextArea.setText("Error in OPT_trig Mode");
               }
            }
            
        };
        thread.start();
    }//GEN-LAST:event_AcquireButtonActionPerformed

    private void ShutterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShutterButtonActionPerformed
        // TODO add your handling code here:
                        
        if (Global.shutter==0){
           ShC_.openshutter();
           Global.shutter=1;
           //MessageTextArea.setText("LED ON");
        }
        else if (Global.shutter==1){
           ShC_.closeshutter();
           Global.shutter=0;  
           //MessageTextArea.setText("LED OFF");
        }   
    }//GEN-LAST:event_ShutterButtonActionPerformed

    private void CheckAlignButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CheckAlignButtonActionPerformed
        // TODO add your handling code here:
        Thread thread = new Thread(){
            public void run(){
                ACQR_.Configure();
                ACQR_.CheckAlignement();
                mmc_.sleep(100);
                IJ.run("Flip Horizontally", "slice");
            }
        };
                thread.start();
    }//GEN-LAST:event_CheckAlignButtonActionPerformed

    private void DirectoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DirectoryButtonActionPerformed
        // TODO add your handling code here:
        File f = FileDialogs.openDir(this, "Directory to save to",
                new FileDialogs.FileType("SaveDir", "Save Directory",
                        "D:\\Data", true, ""));
        RootDirectoryField.setText(f.getAbsolutePath());

        acq_.setSaveFiles(rootPaneCheckingEnabled);
    }//GEN-LAST:event_DirectoryButtonActionPerformed

    private void TimeLapseCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TimeLapseCheckBoxActionPerformed
        // TODO add your handling code here:
        if (TimeLapseCheckBox.isSelected()){
            TimeFrames.setEnabled(true);
            IntervalTime.setEnabled(true);
            IntervalUnits.setEnabled(true);
            NumberLabel.setEnabled(true);
            IntervalLabel.setEnabled(true);
        }
        else{
            TimeFrames.setEnabled(false);
            IntervalTime.setEnabled(false);
            IntervalUnits.setEnabled(false);
            NumberLabel.setEnabled(false);
            IntervalLabel.setEnabled(false); 
        }
    }//GEN-LAST:event_TimeLapseCheckBoxActionPerformed

    private void RotationComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RotationComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_RotationComboBoxActionPerformed

    private void Intensity_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Intensity_ComboBoxActionPerformed
        // TODO add your handling code here:
        int Current_E=Excitation_ComboBox.getSelectedIndex();
        String Current_Ex= Integer.toString(Current_E);
        String Current_Item=Intensity_ComboBox.getItemAt(Intensity_ComboBox.getSelectedIndex());
        String CI=FC_.ChangeIntensity(Current_Ex, Current_Item);
        Global.shutter = 1;
        
        if(Current_E == 0){
            if(LedCheckBox1.isSelected()){
                LedSlider1.setValue(Integer.parseInt(Current_Item));
                MessageTextArea.setText("The Current Intensity is:  "+ CI);
            }
            else{
                MessageTextArea.setText("Turn ON LED 1");
            }
        }
        else if(Current_E == 1){
            if(LedCheckBox2.isSelected()){
                LedSlider2.setValue(Integer.parseInt(Current_Item));
                MessageTextArea.setText("The Current Intensity is:  "+ CI);
            }
            else{
                MessageTextArea.setText("Turn ON LED 2");
            }
        }
        else if(Current_E == 2){
            if(LedCheckBox3.isSelected()){
                LedSlider3.setValue(Integer.parseInt(Current_Item));
                MessageTextArea.setText("The Current Intensity is:  "+ CI);
            }
            else{
                MessageTextArea.setText("Turn ON LED 3");
            }
        }
        else if(Current_E == 3){
            if(LedCheckBox4.isSelected()){
                LedSlider4.setValue(Integer.parseInt(Current_Item));
                MessageTextArea.setText("The Current Intensity is:  "+ CI);
            }
            else{
                MessageTextArea.setText("Turn ON LED 4");
            }
        }
        
    }//GEN-LAST:event_Intensity_ComboBoxActionPerformed

    private void Emission_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Emission_ComboBoxActionPerformed
        // TODO add your handling code here:
        int Current_Item=Emission_ComboBox.getSelectedIndex();
        String CI=FC_.ChangeEmissionFilter(Current_Item);
        String CIN = Emission_ComboBox.getItemAt(Current_Item);
        MessageTextArea.setText("The Current Emission Filter is:  "+ CIN);
    }//GEN-LAST:event_Emission_ComboBoxActionPerformed

    private void Excitation_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Excitation_ComboBoxActionPerformed
        // TODO add your handling code here:
        int Current_Item=Excitation_ComboBox.getSelectedIndex();
        String CI=FC_.ChangeExcitationFilter(Current_Item);
        String CIN = Excitation_ComboBox.getItemAt(Current_Item);
        MessageTextArea.setText("The Current Excitation Filter is:  "+ CIN);
    }//GEN-LAST:event_Excitation_ComboBoxActionPerformed

    private void Channel_CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Channel_CheckBoxActionPerformed
        // TODO add your handling code here:
        if (Channel_CheckBox.isSelected()){
            Channeltable.setEnabled(true);
            ChannelScrollPane.setEnabled(true);
            ChannelControlPanel.setEnabled(true);
        }
        else{
            Channeltable.setEnabled(false);
            ChannelScrollPane.setEnabled(false);
            ChannelControlPanel.setEnabled(false);
        }
    }//GEN-LAST:event_Channel_CheckBoxActionPerformed

    private void LineCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LineCheckBoxActionPerformed
        // TODO add your handling code here:
        if (LineCheckBox.isSelected()){
            IJ.makeLine(mmc_.getImageWidth()/2, 0, mmc_.getImageWidth()/2, mmc_.getImageHeight());
        }
        else {
            IJ.makeLine(0, 0, 0, 0);
        }
    }//GEN-LAST:event_LineCheckBoxActionPerformed

    private void MakeZeroButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MakeZeroButtonActionPerformed
        // TODO add your handling code here:
        AnglePositionField.setText("0");
    }//GEN-LAST:event_MakeZeroButtonActionPerformed

    private void StepAngleComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepAngleComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_StepAngleComboBoxActionPerformed

    private void RightButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RightButtonActionPerformed
        // TODO add your handling code here:
        String Step;
        String Position;
        
        Step=(String)StepAngleComboBox.getSelectedItem();
        Position=AnglePositionField.getText();
        Position=RC_.rightMove(Step,Position);
        AnglePositionField.setText(Position);


    }//GEN-LAST:event_RightButtonActionPerformed

    private void AnglePositionFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AnglePositionFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_AnglePositionFieldActionPerformed

    private void LeftButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LeftButtonActionPerformed
        // TODO add your handling code here:
        String Step;
        String Position;
        
        Step=(String)StepAngleComboBox.getSelectedItem();
        Position=AnglePositionField.getText();
        Position=RC_.leftMove(Step,Position);
        AnglePositionField.setText(Position);
    }//GEN-LAST:event_LeftButtonActionPerformed

    private void onWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_onWindowClosing
        // TODO add your handling code here:
        prefs_.putInt(FRAMEXPOS, (int) getLocation().getX());
        prefs_.putInt(FRAMEYPOS, (int) getLocation().getY());
        JavaUtils.putObjectInPrefs(prefs_, DIALOG_POSITION, this.getLocation());
        acq_.shutdown();
        plugin_.dispose();
    }//GEN-LAST:event_onWindowClosing

    private void LedCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LedCheckBox1ActionPerformed
        // TODO add your handling code here:
        try{
        if(LedCheckBox1.isSelected()){
            mmc_.setProperty("SRotation", "Command", "LED0ENABLE=1;");
            LedSlider1.setEnabled(true);
        }
        else{
            mmc_.setProperty("SRotation", "Command", "LED0ENABLE=0;");
            LedSlider1.setEnabled(false);
        }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Enable 1");
        }

    }//GEN-LAST:event_LedCheckBox1ActionPerformed

    private void LedCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LedCheckBox2ActionPerformed
        // TODO add your handling code here:
        try{
        if(LedCheckBox2.isSelected()){
            mmc_.setProperty("SRotation", "Command", "LED1ENABLE=1;");
            LedSlider2.setEnabled(true);
        }
        else{
            mmc_.setProperty("SRotation", "Command", "LED1ENABLE=0;");
            LedSlider2.setEnabled(false);
        }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Enable 2");
        }
    }//GEN-LAST:event_LedCheckBox2ActionPerformed

    private void LedCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LedCheckBox3ActionPerformed
        // TODO add your handling code here:
    try{
        if(LedCheckBox3.isSelected()){
            mmc_.setProperty("SRotation", "Command", "LED2ENABLE=1;");
            LedSlider3.setEnabled(true);
        }
        else{
            mmc_.setProperty("SRotation", "Command", "LED2ENABLE=0;");
            LedSlider3.setEnabled(false);
        }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Enable 3");
        }
    }//GEN-LAST:event_LedCheckBox3ActionPerformed

    private void LedCheckBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LedCheckBox4ActionPerformed
        // TODO add your handling code here:
    try{
        if(LedCheckBox4.isSelected()){
            mmc_.setProperty("SRotation", "Command", "LED3ENABLE=1;");
            LedSlider4.setEnabled(true);
        }
        else{
            mmc_.setProperty("SRotation", "Command", "LED3ENABLE=0;");
            LedSlider4.setEnabled(false);
        }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Enable 4");
        }
    }//GEN-LAST:event_LedCheckBox4ActionPerformed

    private void LedSlider1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LedSlider1StateChanged
        // TODO add your handling code here:
        int value = LedSlider1.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "LED0INTENSITY="+value+";");
            mmc_.sleep(100);
            mmc_.setProperty("SRotation", "Command", "LED0STATE=1;");
            SettingsTextArea.setText("Led 1 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Intensity 1");
        }
    }//GEN-LAST:event_LedSlider1StateChanged

    private void LedSlider2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LedSlider2StateChanged
        // TODO add your handling code here:
        int value = LedSlider2.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "LED1INTENSITY="+value+";");
            mmc_.sleep(100);
            mmc_.setProperty("SRotation", "Command", "LED1STATE=1;");
            SettingsTextArea.setText("Led 2 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Intensity 2");
        }
    }//GEN-LAST:event_LedSlider2StateChanged

    private void LedSlider3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LedSlider3StateChanged
        // TODO add your handling code here:
        int value = LedSlider3.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "LED2INTENSITY="+value+";");
            mmc_.sleep(100);
            mmc_.setProperty("SRotation", "Command", "LED2STATE=1;");
            SettingsTextArea.setText("Led 3 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Intensity 3");
        }
    }//GEN-LAST:event_LedSlider3StateChanged

    private void LedSlider4StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LedSlider4StateChanged
        // TODO add your handling code here:
        int value = LedSlider4.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "LED3INTENSITY="+value+";");
            mmc_.sleep(100);
            mmc_.setProperty("SRotation", "Command", "LED3STATE=1;");
            SettingsTextArea.setText("Led 4 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Intensity 4");
        }
    }//GEN-LAST:event_LedSlider4StateChanged

    private void MotorCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MotorCheckBox1ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox1.getSelectedItem();
        String StepType=Integer.toString(StepTypeComboBox1.getSelectedIndex());
        try{
        if(MotorCheckBox1.isSelected()){
            mmc_.setProperty("SRotation", "Command", "MOTOR0ENABLE=1;");
            mmc_.setProperty("SRotation", "Command", "MOTOR0STEPS="+Steps+";");
            mmc_.setProperty("SRotation", "Command", "MOTOR0STEPTYPE="+StepType+";");
            MotorSlider1.setEnabled(true);
            StepsComboBox1.setEnabled(true);
            StepTypeComboBox1.setEnabled(true);
        }
        else{
            mmc_.setProperty("SRotation", "Command", "MOTOR0ENABLE=0;");
            MotorSlider1.setEnabled(false);
            StepsComboBox1.setEnabled(false);
            StepTypeComboBox1.setEnabled(false);
        }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Enable 1");
        }
    }//GEN-LAST:event_MotorCheckBox1ActionPerformed

    private void MotorCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MotorCheckBox2ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox1.getSelectedItem();
        String StepType=Integer.toString(StepTypeComboBox1.getSelectedIndex());
        try{
        if(MotorCheckBox2.isSelected()){
            mmc_.setProperty("SRotation", "Command", "MOTOR1ENABLE=1;");
            mmc_.setProperty("SRotation", "Command", "MOTOR1STEPS="+Steps+";");
            mmc_.setProperty("SRotation", "Command", "MOTOR0STEPTYPE="+StepType+";");
            MotorSlider2.setEnabled(true);
            StepsComboBox2.setEnabled(true);
            StepTypeComboBox2.setEnabled(true);
        }
        else{
            mmc_.setProperty("SRotation", "Command", "MOTOR1ENABLE=0;");
            MotorSlider2.setEnabled(false);
            StepsComboBox2.setEnabled(false);
            StepTypeComboBox2.setEnabled(false);
        }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Enable 2");
        }
    }//GEN-LAST:event_MotorCheckBox2ActionPerformed

    private void MotorCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MotorCheckBox3ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox1.getSelectedItem();
        String StepType=Integer.toString(StepTypeComboBox1.getSelectedIndex());
        try{
        if(MotorCheckBox3.isSelected()){
            mmc_.setProperty("SRotation", "Command", "MOTOR2ENABLE=1;");
            mmc_.setProperty("SRotation", "Command", "MOTOR2STEPS="+Steps+";");
            mmc_.setProperty("SRotation", "Command", "MOTOR0STEPTYPE="+StepType+";");
            MotorSlider3.setEnabled(true);
            StepsComboBox3.setEnabled(true);
            StepTypeComboBox3.setEnabled(true);
        }
        else{
            mmc_.setProperty("SRotation", "Command", "MOTOR2ENABLE=0;");
            MotorSlider3.setEnabled(false);
            StepsComboBox3.setEnabled(false);
            StepTypeComboBox3.setEnabled(false);
        }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Enable 3");
        }
    }//GEN-LAST:event_MotorCheckBox3ActionPerformed

    private void MotorCheckBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MotorCheckBox4ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox1.getSelectedItem();
        String StepType=Integer.toString(StepTypeComboBox1.getSelectedIndex());
        try{
        if(MotorCheckBox4.isSelected()){
            mmc_.setProperty("SRotation", "Command", "MOTOR3ENABLE=1;");
            mmc_.setProperty("SRotation", "Command", "MOTOR3STEPS="+Steps+";");
            mmc_.setProperty("SRotation", "Command", "MOTOR0STEPTYPE="+StepType+";");
            MotorSlider4.setEnabled(true);
            StepsComboBox4.setEnabled(true);
            StepTypeComboBox4.setEnabled(true);
        }
        else{
            mmc_.setProperty("SRotation", "Command", "MOTOR3ENABLE=0;");
            MotorSlider4.setEnabled(false);
            StepsComboBox4.setEnabled(false);
            StepTypeComboBox4.setEnabled(false);
        }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Enable 4");
        }
    }//GEN-LAST:event_MotorCheckBox4ActionPerformed

    private void StepsComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepsComboBox1ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox1.getSelectedItem();
        try{
            mmc_.setProperty("SRotation", "Command", "MOTOR0STEPS="+Steps+";");
            if("Motor_1".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox1.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"1.8","18","9","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45","90","180","360"};
                    }
                }
                else if("400".equals(StepsComboBox1.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.1125","0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                }
                else{
                    Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45","0.225", "0.1125" };
                }
                mmc_.sleep(100);
                RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }

      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Steps 1");
        }
    }//GEN-LAST:event_StepsComboBox1ActionPerformed

    private void StepsComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepsComboBox2ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox2.getSelectedItem();
        try{
            mmc_.setProperty("SRotation", "Command", "MOTOR1STEPS="+Steps+";");
            if("Motor_2".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox2.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"1.8","18","9","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45","90","180","360"};
                    }
                }
                else if("400".equals(StepsComboBox2.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.1125","0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                }
                else{
                    Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45","0.225", "0.1125" };
                }
                mmc_.sleep(100);
                RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Steps 2");
        }
    }//GEN-LAST:event_StepsComboBox2ActionPerformed

    private void StepsComboBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepsComboBox3ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox3.getSelectedItem();
        try{
            mmc_.setProperty("SRotation", "Command", "MOTOR2STEPS="+Steps+";");
            if("Motor_3".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox3.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"1.8","18","9","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45","90","180","360"};
                    }
                }
                else if("400".equals(StepsComboBox3.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.1125","0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                }
                else{
                    Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45","0.225", "0.1125" };
                }
                mmc_.sleep(100);
                RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Steps 3");
        }
    }//GEN-LAST:event_StepsComboBox3ActionPerformed

    private void StepsComboBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepsComboBox4ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox4.getSelectedItem();
        try{
            mmc_.setProperty("SRotation", "Command", "MOTOR3STEPS="+Steps+";");
            if("Motor_4".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox4.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"1.8","18","9","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45","90","180","360"};
                    }
                }
                else if("400".equals(StepsComboBox4.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.1125","0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                }
                else{
                    Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45","0.225", "0.1125" };
                }
                mmc_.sleep(100);
                RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Steps 4");
        }
    }//GEN-LAST:event_StepsComboBox4ActionPerformed

    private void StepTypeComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepTypeComboBox1ActionPerformed
        // TODO add your handling code here:
        String StepType=(String)StepTypeComboBox1.getSelectedItem();
        try{
            if(StepType.equalsIgnoreCase("FULL")){
                mmc_.setProperty("SRotation", "Command", "MOTOR0STEPTYPE=0;");
            }
            if(StepType.equalsIgnoreCase("HALF")){
                mmc_.setProperty("SRotation", "Command", "MOTOR0STEPTYPE=1;");
            }
            if(StepType.equalsIgnoreCase("QUARTER")){
                mmc_.setProperty("SRotation", "Command", "MOTOR0STEPTYPE=2;");
            }
            if(StepType.equalsIgnoreCase("EIGHTH")){
                mmc_.setProperty("SRotation", "Command", "MOTOR0STEPTYPE=3;");
            }
            /*if(StepType.equalsIgnoreCase("SIXTEEN")){
                mmc_.setProperty("SRotation", "Command", "MOTOR0STEPTYPE=4;");
            }*/
            if("Motor_1".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox1.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"1.8","18","9","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45","90","180","360"};
                    }
                }
                else if("400".equals(StepsComboBox1.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox1.getSelectedItem())){
                       Angles = new String[]{"0.1125","0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                }
                else{
                    Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45","0.225", "0.1125" };
                }
                RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
            catch (Exception ex) {
                  Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
                  SettingsTextArea.setText("Error in Motor Step Type 1");
        }
    }//GEN-LAST:event_StepTypeComboBox1ActionPerformed

    private void StepTypeComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepTypeComboBox2ActionPerformed
        // TODO add your handling code here:
        String StepType=(String)StepTypeComboBox2.getSelectedItem();
        try{
            if(StepType.equalsIgnoreCase("FULL")){
                mmc_.setProperty("SRotation", "Command", "MOTOR1STEPTYPE=0;");
            }
            if(StepType.equalsIgnoreCase("HALF")){
                mmc_.setProperty("SRotation", "Command", "MOTOR1STEPTYPE=1;");
            }
            if(StepType.equalsIgnoreCase("QUARTER")){
                mmc_.setProperty("SRotation", "Command", "MOTOR1STEPTYPE=2;");
            }
            if(StepType.equalsIgnoreCase("EIGHTH")){
                mmc_.setProperty("SRotation", "Command", "MOTOR1STEPTYPE=3;");
            }
            /*if(StepType.equalsIgnoreCase("SIXTEEN")){
                mmc_.setProperty("SRotation", "Command", "MOTOR1STEPTYPE=4;");
            }*/
            if("Motor_2".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox2.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"1.8","18","9","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45","90","180","360"};
                    }
                }
                else if("400".equals(StepsComboBox2.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox2.getSelectedItem())){
                       Angles = new String[]{"0.1125","0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                }
                else{
                    Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45","0.225", "0.1125" };
                }
                RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Step Type 2");
        }
    }//GEN-LAST:event_StepTypeComboBox2ActionPerformed

    private void StepTypeComboBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepTypeComboBox3ActionPerformed
        // TODO add your handling code here:
                String StepType=(String)StepTypeComboBox3.getSelectedItem();
        try{
            if(StepType.equalsIgnoreCase("FULL")){
                mmc_.setProperty("SRotation", "Command", "MOTOR2STEPTYPE=0;");
            }
            if(StepType.equalsIgnoreCase("HALF")){
                mmc_.setProperty("SRotation", "Command", "MOTOR2STEPTYPE=1;");
            }
            if(StepType.equalsIgnoreCase("QUARTER")){
                mmc_.setProperty("SRotation", "Command", "MOTOR2STEPTYPE=2;");
            }
            if(StepType.equalsIgnoreCase("EIGHTH")){
                mmc_.setProperty("SRotation", "Command", "MOTOR2STEPTYPE=3;");
            }
            /*if(StepType.equalsIgnoreCase("SIXTEEN")){
                mmc_.setProperty("SRotation", "Command", "MOTOR2STEPTYPE=4;");
            }*/
            if("Motor_3".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox3.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"1.8","18","9","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45","90","180","360"};
                    }
                }
                else if("400".equals(StepsComboBox3.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox3.getSelectedItem())){
                       Angles = new String[]{"0.1125","0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                }
                else{
                    Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45","0.225", "0.1125" };
                }
                RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Step Type 3");
        }
    }//GEN-LAST:event_StepTypeComboBox3ActionPerformed

    private void StepTypeComboBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepTypeComboBox4ActionPerformed
        // TODO add your handling code here:
                String StepType=(String)StepTypeComboBox4.getSelectedItem();
        try{
            if(StepType.equalsIgnoreCase("FULL")){
                mmc_.setProperty("SRotation", "Command", "MOTOR3STEPTYPE=0;");
            }
            if(StepType.equalsIgnoreCase("HALF")){
                mmc_.setProperty("SRotation", "Command", "MOTOR3STEPTYPE=1;");
            }
            if(StepType.equalsIgnoreCase("QUARTER")){
                mmc_.setProperty("SRotation", "Command", "MOTOR3STEPTYPE=2;");
            }
            if(StepType.equalsIgnoreCase("EIGHTH")){
                mmc_.setProperty("SRotation", "Command", "MOTOR3STEPTYPE=3;");
            }
            /*if(StepType.equalsIgnoreCase("SIXTEEN")){
                mmc_.setProperty("SRotation", "Command", "MOTOR3STEPTYPE=4;");
            }*/
            if("Motor_4".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox4.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"1.8","18","9","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45","90","180","360"};
                    }
                }
                else if("400".equals(StepsComboBox4.getSelectedItem())){
                    if("FULL".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("HALF".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("QUARTER".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                    else if("EIGHTH".equals(StepTypeComboBox4.getSelectedItem())){
                       Angles = new String[]{"0.1125","0.225","0.45","0.9","1.8","9","18","45", "90", "180","360"};
                    }
                }
                else{
                    Angles = new String[]{"45", "90", "180","360","18","9","1.8","0.9","0.45","0.225", "0.1125" };
                }
                RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Step Type 4");
        }
    }//GEN-LAST:event_StepTypeComboBox4ActionPerformed

    private void MotorSlider1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_MotorSlider1StateChanged
        // TODO add your handling code here:
        int value = MotorSlider1.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "MOTOR0VELOCITY="+value+";");
            SettingsTextArea.setText("Motor Velocity "+value);
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Velocity 1");
        }
    }//GEN-LAST:event_MotorSlider1StateChanged

    private void MotorSlider2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_MotorSlider2StateChanged
        // TODO add your handling code here:
        int value = MotorSlider2.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "MOTOR1VELOCITY="+value+";");
            SettingsTextArea.setText("Motor Velocity "+value);
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Velocity 2");
        }
    }//GEN-LAST:event_MotorSlider2StateChanged

    private void MotorSlider3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_MotorSlider3StateChanged
        // TODO add your handling code here:
        int value = MotorSlider3.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "MOTOR2VELOCITY="+value+";");
            SettingsTextArea.setText("Motor Velocity "+value);
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Velocity 3");
        }
    }//GEN-LAST:event_MotorSlider3StateChanged

    private void MotorSlider4StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_MotorSlider4StateChanged
        // TODO add your handling code here:
        int value = MotorSlider4.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "MOTOR3VELOCITY="+value+";");
            SettingsTextArea.setText("Motor Velocity "+value);
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Velocity 4");
        }
    }//GEN-LAST:event_MotorSlider4StateChanged

    private void readBuffer(){
        String str1 = "BLANK";
        String str2 = "";
        String str3 = "";
        try{
            String port = mmc_.getProperty("SRotation", "ShowPort");
            CharVector answer = mmc_.readFromSerialPort(port);
            CharVector answer2 = mmc_.readFromSerialPort(port);
            CharVector answer3 = mmc_.readFromSerialPort(port);

            if (answer.capacity() > 0) {
               for(int i=0; i<answer.capacity(); i++){
                  if(str1 == "BLANK") {
                     str1 = "" + answer.get(i);
                  }   // you have to add the "" otherwise adds decimal value
                  else {
                     str1 = "" + str1 +(char)answer.get(i);
                  }
              }// end loop

            }////////end if statement
            if (answer2.capacity() > 0) {
               for(int i=0; i<answer2.capacity(); i++){
                  if(str2 == "") {
                     str2 = "" + answer2.get(i);
                  }   // you have to add the "" otherwise adds decimal value
                  else {
                     str2 = "" + str2 +(char)answer2.get(i);
                  }
              }// end loop

            }////////end if statement
            if (answer3.capacity() > 0) {
               for(int i=0; i<answer3.capacity(); i++){
                  if(str3 == "") {
                     str3 = "" + answer3.get(i);
                  }   // you have to add the "" otherwise adds decimal value
                  else {
                     str3 = "" + str3 +(char)answer3.get(i);
                  }
              }// end loop

            }////////end if statement
                          SettingsTextArea.setText(str1+str2+str3);
        }  catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in readBuffer");  
        }

    }
    private void HelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_HelpButtonActionPerformed
        // TODO add your handling code here:
        try{
            mmc_.setProperty("SRotation", "Command", "HELP;");
            
            mmc_.sleep(3000);
            readBuffer();
           //SettingsTextArea.setText(Integer.toString(Math.round(answer.capacity()))+"___"+Integer.toString(Math.round(answer2.capacity())));
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error at Serial Communication");
        }
    }//GEN-LAST:event_HelpButtonActionPerformed

    private void LedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LedButtonActionPerformed
        // TODO add your handling code here:
        try{
            mmc_.setProperty("SRotation", "Command", "LEDINFO;");
            mmc_.sleep(2000);
            readBuffer();
//            String value = mmc_.getProperty("SRotation", "Response");
//            SettingsTextArea.setText(value);
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in LED INFO");
        }
    }//GEN-LAST:event_LedButtonActionPerformed

    private void MotorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MotorButtonActionPerformed
        // TODO add your handling code here:
        try{
            mmc_.setProperty("SRotation", "Command", "MOTORINFO;");
            mmc_.sleep(2000);
            readBuffer();
//            String value = mmc_.getProperty("SRotation", "Response");
//            SettingsTextArea.setText(value);
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in MOTOR INFO");
        }
    }//GEN-LAST:event_MotorButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        try{
            mmc_.setProperty("SRotation", "Command", "SEQUENCEINFO;");
            mmc_.sleep(2000);
            readBuffer();
//            String value = mmc_.getProperty("SRotation", "Response");
//            SettingsTextArea.setText(value);
        }catch (Exception ex) {
            Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in SEQUENCE INFO");
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        try{
            mmc_.setProperty(Master, "AnswerTimeout", "5000");
            mmc_.sleep(100);
            mmc_.setProperty(Master, "CommandTerminator", "\r");
            mmc_.sleep(100);
            mmc_.setProperty(Master, "ResponseTerminator", "\r");

            LedCheckBox1.setSelected(false);
            LedCheckBox2.setSelected(false);
            LedCheckBox3.setSelected(false);
            LedCheckBox4.setSelected(false);
            LedSlider1.setEnabled(false);
            LedSlider2.setEnabled(false);
            LedSlider3.setEnabled(false);
            LedSlider4.setEnabled(false);
            LedSlider1.setValue(0);
            LedSlider2.setValue(0);
            LedSlider3.setValue(0);
            LedSlider4.setValue(0);
            MotorCheckBox1.setSelected(false);
            MotorCheckBox2.setSelected(false);
            MotorCheckBox3.setSelected(false);
            MotorCheckBox4.setSelected(false);
            StepsComboBox1.setEnabled(false);
            StepsComboBox2.setEnabled(false);
            StepsComboBox3.setEnabled(false);
            StepsComboBox4.setEnabled(false);
            StepTypeComboBox1.setEnabled(false);
            StepTypeComboBox2.setEnabled(false);
            StepTypeComboBox3.setEnabled(false);
            StepTypeComboBox4.setEnabled(false);
            MotorSlider1.setEnabled(false);
            MotorSlider2.setEnabled(false);
            MotorSlider3.setEnabled(false);
            MotorSlider4.setEnabled(false);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    public void setPlugin(Final plugin) {
      plugin_ = plugin;
    }
    /**
     * @param args the command line arguments
     */
    //public static void main(String args[]) {
    //    /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
    //    try {
    //        for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
    //            if ("Nimbus".equals(info.getName())) {
    //                javax.swing.UIManager.setLookAndFeel(info.getClassName());
    //                break;
    //            }
    //        }
    //    } catch (ClassNotFoundException ex) {
    //        java.util.logging.Logger.getLogger(JDialFinal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    //    } catch (InstantiationException ex) {
    //        java.util.logging.Logger.getLogger(JDialFinal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    //    } catch (IllegalAccessException ex) {
    //        java.util.logging.Logger.getLogger(JDialFinal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    //    } catch (javax.swing.UnsupportedLookAndFeelException ex) {
    //       java.util.logging.Logger.getLogger(JDialFinal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    //    }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
    //    java.awt.EventQueue.invokeLater(new Runnable() {
    //        public void run() {
    //            JDialFinal dialog = new JDialFinal(new javax.swing.JFrame(), true);
    //            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
    //                @Override
    //                public void windowClosing(java.awt.event.WindowEvent e) {
    //                    System.exit(0);
    //                }
    //            });
    //            dialog.setVisible(true);
    //        }
    //    });
    //}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JButton AcquireButton;
    private javax.swing.JPanel Acquisition;
    public javax.swing.JLabel AngleLabel;
    public javax.swing.JTextField AnglePositionField;
    public javax.swing.JPanel ChannelControlPanel;
    public javax.swing.JScrollPane ChannelScrollPane;
    public javax.swing.JCheckBox Channel_CheckBox;
    public javax.swing.JTable Channeltable;
    public javax.swing.JButton CheckAlignButton;
    public javax.swing.JTextField DelayField;
    public javax.swing.JLabel DelayLabel;
    public javax.swing.JButton DirectoryButton;
    public javax.swing.JComboBox<String> Emission_ComboBox;
    public javax.swing.JLabel Emission_label;
    public javax.swing.JComboBox<String> Excitation_ComboBox;
    public javax.swing.JLabel Excitation_label;
    public javax.swing.JLabel FileLabel;
    public javax.swing.JTextField FileNameField;
    public javax.swing.JButton HelpButton;
    public javax.swing.JPanel Ilumination;
    public javax.swing.JComboBox<String> Intensity_ComboBox;
    public javax.swing.JLabel Intensity_label;
    public javax.swing.JLabel IntervalLabel;
    public javax.swing.JTextField IntervalTime;
    public javax.swing.JComboBox<String> IntervalUnits;
    public javax.swing.JButton LedButton;
    public javax.swing.JCheckBox LedCheckBox1;
    public javax.swing.JCheckBox LedCheckBox2;
    public javax.swing.JCheckBox LedCheckBox3;
    public javax.swing.JCheckBox LedCheckBox4;
    public javax.swing.JLabel LedIntLabel;
    public javax.swing.JLabel LedLabel;
    public javax.swing.JLabel LedOnOffLabel;
    public javax.swing.JPanel LedPanel;
    public javax.swing.JSlider LedSlider1;
    public javax.swing.JSlider LedSlider2;
    public javax.swing.JSlider LedSlider3;
    public javax.swing.JSlider LedSlider4;
    public javax.swing.JButton LeftButton;
    public javax.swing.JCheckBox LineCheckBox;
    public javax.swing.JButton MakeZeroButton;
    public javax.swing.JLabel MessageLabel;
    public javax.swing.JTextArea MessageTextArea;
    public javax.swing.JPanel Message_Pannel;
    public javax.swing.JScrollPane Message_scroll_pane;
    private javax.swing.JButton MotorButton;
    public javax.swing.JCheckBox MotorCheckBox1;
    public javax.swing.JCheckBox MotorCheckBox2;
    public javax.swing.JCheckBox MotorCheckBox3;
    public javax.swing.JCheckBox MotorCheckBox4;
    public javax.swing.JLabel MotorLabel;
    public javax.swing.JLabel MotorOnOffLabel;
    public javax.swing.JPanel MotorPanel;
    public javax.swing.JSlider MotorSlider1;
    public javax.swing.JSlider MotorSlider2;
    public javax.swing.JSlider MotorSlider3;
    public javax.swing.JSlider MotorSlider4;
    public javax.swing.JLabel NumberLabel;
    public javax.swing.JButton RightButton;
    public javax.swing.JTextField RootDirectoryField;
    public javax.swing.JLabel RootLabel;
    public javax.swing.JComboBox<String> RotationComboBox;
    public javax.swing.JLabel RotationControlLabel;
    public javax.swing.JPanel RotationControlPanel;
    public javax.swing.JLabel RotationLabel;
    public javax.swing.JComboBox<String> RotationMotorComboBox;
    public javax.swing.JPanel RotationPanel;
    public javax.swing.JLabel RotationSampleLabel;
    public javax.swing.JCheckBox SaveCheckBox;
    public javax.swing.JPanel SaveImagesPanel;
    public javax.swing.JPanel Settings;
    public javax.swing.JTextArea SettingsTextArea;
    public javax.swing.JButton ShutterButton;
    public javax.swing.JCheckBox ShutterRotationCheckBox;
    public javax.swing.JComboBox<String> StepAngleComboBox;
    public javax.swing.JLabel StepAngleLabel;
    public javax.swing.JComboBox StepTypeComboBox1;
    public javax.swing.JComboBox StepTypeComboBox2;
    public javax.swing.JComboBox StepTypeComboBox3;
    public javax.swing.JComboBox StepTypeComboBox4;
    public javax.swing.JLabel StepTypeLabel;
    public javax.swing.JComboBox StepsComboBox1;
    public javax.swing.JComboBox StepsComboBox2;
    public javax.swing.JComboBox StepsComboBox3;
    public javax.swing.JComboBox StepsComboBox4;
    public javax.swing.JLabel StepsLabel;
    public javax.swing.JSpinner TimeFrames;
    public javax.swing.JCheckBox TimeLapseCheckBox;
    public javax.swing.JPanel TimeLapsePanel;
    public javax.swing.JLabel VelocityLabel;
    private javax.swing.JFileChooser fc;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    public javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration//GEN-END:variables
}
