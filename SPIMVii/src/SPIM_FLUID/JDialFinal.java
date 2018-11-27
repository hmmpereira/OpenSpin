package SPIM_FLUID;


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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
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
import org.micromanager.api.PositionList;
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
    private SPIMIIcontrolsPlugin plugin_;
    
    //private final MMStudio mgui_;
    //private final CMMCore mmc_;
    //private final Preferences prefs_;
    private String DIALOG_POSITION = "dialogPosition";     
    //private MMStudio gui_;
    private Color[] colors={Color.green,Color.yellow,Color.orange,Color.red, Color.magenta, Color.cyan};
    String[] Excitation_filters= { "473", "561", "633", "Bright"};//new String[4];
    String[] Emission_filters={ "535/70", "580/25", "620/90", "640/25", "SP 700", "700/75" };
    String[] Intensity={"0","10","20","30","40","50","60","70","80","90","100"};
    String[] Angles;
    public String Channels[]=new String [6];
    private String StateDev_;
    private String ShutterDev_;
    private Preferences prefs_;    
    
    private FilterControl FC_;
    private String StateDev_FW_;
    private String ShutterDev_FW_;

    private int frameXPos_ = 100;
    private int frameYPos_ = 100;

    private static final String FRAMEXPOS = "FRAMEXPOS";
    private static final String FRAMEYPOS = "FRAMEYPOS";
    
    private String Master = Global.Master;  
    private String Galvo1 = Global.Galvo1;
    private String Galvo2 = Global.Galvo2;
    
    NumberFormat formatter = new DecimalFormat("#.0",new DecimalFormatSymbols(Locale.US));
    NumberFormat timeformat = new DecimalFormat("#.00",new DecimalFormatSymbols(Locale.US));
   
   public void setUpComboColumn(JTable table,
                                 TableColumn sportColumn,
                                JComboBox combobox){

        //Set up the editor for the sport cells.
        sportColumn.setCellEditor(new DefaultCellEditor(combobox));
 
        //Set up tool tips for the sport cells.
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
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
            {"Ch1", Excitation_filters[0], Emission_filters[0], new Integer(100),Intensity[0], colors[0], new Boolean(false)},
            {"Ch2", Excitation_filters[1], Emission_filters[1], new Integer(100),Intensity[1], colors[1], new Boolean(false)},
            {"Ch3", Excitation_filters[2], Emission_filters[2], new Integer(100),Intensity[2], colors[2], new Boolean(false)},
            {"Ch3", Excitation_filters[0], Emission_filters[3], new Integer(100),Intensity[3], colors[3], new Boolean(false)},
            {"Ch4", Excitation_filters[1], Emission_filters[4], new Integer(100),Intensity[4], colors[4], new Boolean(false)},
            {"Ch5", Excitation_filters[2], Emission_filters[5], new Integer(100),Intensity[5], colors[5], new Boolean(false)}
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
    public JDialFinal(SPIMIIcontrolsPlugin plugin, ScriptInterface gui, AcquisitionEngine acq) {
        setTitle("SPIMvII");
        gui_ = (MMStudio)gui;
        mmc_ = gui_.getMMCore();
        acq_ = acq;
        plugin_ = plugin;

                /// DEVICE DEFINITION  /////  
        String StateDev_FW   = "Arduino_FW-Switch";
        String ShutterDev_FW = "Arduino_FW-Shutter"; 

        String ShutterDev    = mmc_.getShutterDevice();
        String StateDev      = "Arduino_SR-Switch";
    
    
//    try {
//            mmc_.setProperty(StateDev,"State","32");     
//            mmc_.setProperty(ShutterDev,"OnOff","1");
//            mmc_.setProperty(ShutterDev,"OnOff","0");
//       } catch (Exception ex) {
//            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
//        }   
    
        ShC_ = new ShutterControl(mmc_, StateDev, ShutterDev, this);
        FC_ = new FilterControl(mmc_, StateDev_FW, ShutterDev_FW);
        RC_ = new RotationControl2(mmc_, StateDev, this);
        setLocation(frameXPos_, frameYPos_);
        prefs_ = Preferences.userNodeForPackage(this.getClass());
        ACQR_ = new acquisition(mmc_,this, acq_, gui_, ShC_, FC_, RC_);
        initComponents();
        setBackground(gui_.getBackgroundColor());
        
        try{
            mmc_.setProperty(Master, "AnswerTimeout", "5000");
            mmc_.sleep(100);
            mmc_.setProperty(Master, "CommandTerminator", "\r");
            mmc_.sleep(100);
            mmc_.setProperty(Master, "ResponseTerminator", "\r");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo1, "CommandTerminator", "\r");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo1, "ResponseTerminator", "\r");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "CommandTerminator", "\r");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "ResponseTerminator", "\r");
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
        String focusdevice = " ";
        String xyStage = " ";
        try {
            focusdevice=mmc_.getFocusDevice();
            xyStage = mmc_.getXYStageDevice();
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
        }
        RotationControlPanel = new javax.swing.JPanel();
        RotationControlLabel = new javax.swing.JLabel();
        AngleLabel = new javax.swing.JLabel();
        LeftButton = new javax.swing.JButton();
        AnglePositionField = new javax.swing.JTextField();
        RightButton = new javax.swing.JButton();
        StepAngleLabel = new javax.swing.JLabel();
        DelayLabel = new javax.swing.JLabel();
        DelayField = new javax.swing.JTextField();
        MakeZeroButton = new javax.swing.JButton();
        LineCheckBox = new javax.swing.JCheckBox();
        StepAngleComboBox = new javax.swing.JComboBox();
        RotationPanel = new javax.swing.JPanel();
        RotationLabel = new javax.swing.JLabel();
        ShutterRotationCheckBox = new javax.swing.JCheckBox();
        RotationComboBox = new javax.swing.JComboBox();
        RotationMotorComboBox = new javax.swing.JComboBox();
        RotationSampleCheckBox = new javax.swing.JCheckBox();
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
        ChannelControlPanel = new javax.swing.JPanel();
        Channel_CheckBox = new javax.swing.JCheckBox();
        ChannelScrollPane = new javax.swing.JScrollPane();
        Channeltable = new JTable(new MyTableModel());
        jPanel1 = new javax.swing.JPanel();
        Excitation_label = new javax.swing.JLabel();
        Emission_label = new javax.swing.JLabel();
        try{
            String current= System.getProperty("user.dir");
            File file = new File(current + "\\mmplugins\\Config_EX_file.txt");
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
        SaveImagesPanel = new javax.swing.JPanel();
        SaveCheckBox = new javax.swing.JCheckBox();
        RootLabel = new javax.swing.JLabel();
        FileLabel = new javax.swing.JLabel();
        RootDirectoryField = new javax.swing.JTextField();
        FileNameField = new javax.swing.JTextField();
        DirectoryButton = new javax.swing.JButton();
        DataSetSizeField = new javax.swing.JTextField();
        Message_Pannel = new javax.swing.JPanel();
        MessageLabel = new javax.swing.JLabel();
        Message_scroll_pane = new javax.swing.JScrollPane();
        MessageTextArea = new javax.swing.JTextArea();
        XYPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        XYListButton = new javax.swing.JButton();
        XYStageControl = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        YUPUPButton = new javax.swing.JButton();
        YUPButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        YDownDownButton = new javax.swing.JButton();
        YDownButton = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        XLeftLeftButton = new javax.swing.JButton();
        XLeftButton = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        XYUpUpField = new javax.swing.JTextField();
        XYUpField = new javax.swing.JTextField();
        jPanel9 = new javax.swing.JPanel();
        XRightButton = new javax.swing.JButton();
        XRightRightButton = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        XMotorComboBox = new javax.swing.JComboBox();
        XPositionField = new javax.swing.JTextField();
        XMotorLabel = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        YMotorLabel = new javax.swing.JLabel();
        YMotorComboBox = new javax.swing.JComboBox();
        YPositionField = new javax.swing.JTextField();
        jPanel12 = new javax.swing.JPanel();
        CameraPanel = new javax.swing.JPanel();
        FirstCamComboBox = new javax.swing.JComboBox();
        SecondCamComboBox = new javax.swing.JComboBox();
        CameraControlLabel = new javax.swing.JLabel();
        FirstCamLabel = new javax.swing.JLabel();
        SecondCamLabel = new javax.swing.JLabel();
        ModePanel = new javax.swing.JPanel();
        ModeComboBox = new javax.swing.JComboBox();
        ModeLabel = new javax.swing.JLabel();
        PumpsPanel = new javax.swing.JPanel();
        PumpsLabel = new javax.swing.JLabel();
        VolumeTextField = new javax.swing.JTextField();
        SpeedTextField = new javax.swing.JTextField();
        DelayTextField = new javax.swing.JTextField();
        PumpsComboBox = new javax.swing.JComboBox();
        StartPjButton = new javax.swing.JButton();
        StageControlPanel = new javax.swing.JPanel();
        UPUPButton = new javax.swing.JButton();
        UPButton = new javax.swing.JButton();
        DownButton = new javax.swing.JButton();
        DownDownButton = new javax.swing.JButton();
        PositionField = new javax.swing.JTextField();
        PositionLabel = new javax.swing.JLabel();
        UpUpField = new javax.swing.JTextField();
        UpField = new javax.swing.JTextField();
        StageControlLabel = new javax.swing.JLabel();
        String StackPosition="ERROR";
        try {
            double pos=mmc_.getPosition(focusdevice);
            NumberFormat formatter = new DecimalFormat("#.0",new DecimalFormatSymbols(Locale.US));
            StackPosition=formatter.format(pos);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
        }
        StageMotorComboBox = new javax.swing.JComboBox();
        ZMotorLabel = new javax.swing.JLabel();
        StackPanel = new javax.swing.JPanel();
        StackCheckBox = new javax.swing.JCheckBox();
        StartField = new javax.swing.JTextField();
        SetMiddleButton = new javax.swing.JButton();
        EndField = new javax.swing.JTextField();
        SetStartButton = new javax.swing.JButton();
        StepField = new javax.swing.JTextField();
        StartLabel = new javax.swing.JLabel();
        EndLabel = new javax.swing.JLabel();
        StepLabel = new javax.swing.JLabel();
        ShutterStackCheckBox = new javax.swing.JCheckBox();
        int ts = 0;
        NumberofSlicesField = new javax.swing.JTextField();
        TimeLabel = new javax.swing.JLabel();
        TotalTimeField = new javax.swing.JTextField();
        MiddleLabel1 = new javax.swing.JLabel();
        MiddleField1 = new javax.swing.JTextField();
        SetEndButton = new javax.swing.JButton();
        SetSliceButton = new javax.swing.JButton();
        GalvoControlPanel1 = new javax.swing.JPanel();
        GalvoCheckBox1 = new javax.swing.JCheckBox();
        GAmpTextField1 = new javax.swing.JTextField();
        GAmpLabel1 = new javax.swing.JLabel();
        GFreqTextField1 = new javax.swing.JTextField();
        GFreqLabel1 = new javax.swing.JLabel();
        GMoveButton1 = new javax.swing.JButton();
        GSetButton1 = new javax.swing.JButton();
        GAmpLabel2 = new javax.swing.JLabel();
        G1RunTime = new javax.swing.JTextField();
        GAmpTextField2 = new javax.swing.JTextField();
        G1Run = new javax.swing.JButton();
        GalvoControlPanel2 = new javax.swing.JPanel();
        GalvoCheckBox2 = new javax.swing.JCheckBox();
        GAmpTextField3 = new javax.swing.JTextField();
        GAmpLabel3 = new javax.swing.JLabel();
        GFreqTextField3 = new javax.swing.JTextField();
        GFreqLabel3 = new javax.swing.JLabel();
        GMoveButton2 = new javax.swing.JButton();
        GSetButton2 = new javax.swing.JButton();
        GAmpLabel4 = new javax.swing.JLabel();
        G2RunTime = new javax.swing.JTextField();
        GAmpTextField4 = new javax.swing.JTextField();
        G2Run = new javax.swing.JButton();
        Settings = new javax.swing.JPanel();
        LaserPanel = new javax.swing.JPanel();
        LedLabel = new javax.swing.JLabel();
        LedOnOffLabel = new javax.swing.JLabel();
        LedIntLabel = new javax.swing.JLabel();
        LaserCheckBox1 = new javax.swing.JCheckBox();
        LaserCheckBox2 = new javax.swing.JCheckBox();
        LaserCheckBox3 = new javax.swing.JCheckBox();
        LaserSlider1 = new javax.swing.JSlider();
        LaserCheckBox4 = new javax.swing.JCheckBox();
        LaserSlider2 = new javax.swing.JSlider();
        LaserSlider3 = new javax.swing.JSlider();
        LaserSlider4 = new javax.swing.JSlider();
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
        StepTypeComboBox3 = new javax.swing.JComboBox();
        StepTypeComboBox2 = new javax.swing.JComboBox();
        StepTypeComboBox4 = new javax.swing.JComboBox();
        VelocityLabel = new javax.swing.JLabel();
        MotorSlider1 = new javax.swing.JSlider();
        MotorSlider2 = new javax.swing.JSlider();
        MotorSlider3 = new javax.swing.JSlider();
        MotorSlider4 = new javax.swing.JSlider();
        FunctionLabel = new javax.swing.JLabel();
        FunctionComboBox1 = new javax.swing.JComboBox();
        FunctionComboBox2 = new javax.swing.JComboBox();
        FunctionComboBox3 = new javax.swing.JComboBox();
        FunctionComboBox4 = new javax.swing.JComboBox();
        RestoreButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        SettingsTextArea = new javax.swing.JTextArea();
        HelpButton = new javax.swing.JButton();
        LaserButton = new javax.swing.JButton();
        MotorButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        FilterButton = new javax.swing.JButton();
        GalvoButton = new javax.swing.JButton();
        StageButton = new javax.swing.JButton();
        LedPanel = new javax.swing.JPanel();
        LedLabel1 = new javax.swing.JLabel();
        LedOnOffLabel1 = new javax.swing.JLabel();
        LedIntLabel1 = new javax.swing.JLabel();
        LedCheckBox1 = new javax.swing.JCheckBox();
        LedCheckBox2 = new javax.swing.JCheckBox();
        LedCheckBox3 = new javax.swing.JCheckBox();
        LedSlider1 = new javax.swing.JSlider();
        LedCheckBox4 = new javax.swing.JCheckBox();
        LedSlider2 = new javax.swing.JSlider();
        LedSlider3 = new javax.swing.JSlider();
        LedSlider4 = new javax.swing.JSlider();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setSize(new java.awt.Dimension(600, 413));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                onWindowClosing(evt);
            }
        });

        StrVector zDrives = mmc_.getLoadedDevicesOfType(DeviceType.StageDevice);
        String[] fd2=zDrives.toArray();
        StageMotorComboBox.setModel(new javax.swing.DefaultComboBoxModel(fd2));

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

        AnglePositionField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
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

        DelayField.setText("10");
        DelayField.setToolTipText("Delay between Slices");

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
        LineCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LineCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        LineCheckBox.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        LineCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LineCheckBoxActionPerformed(evt);
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
        StepAngleComboBox.setMaximumRowCount(3);
        StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1.8", "9", "18", "45", "90", "180", "360" }));
        StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
        StepAngleComboBox.setAutoscrolls(true);
        StepAngleComboBox.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        javax.swing.GroupLayout RotationControlPanelLayout = new javax.swing.GroupLayout(RotationControlPanel);
        RotationControlPanel.setLayout(RotationControlPanelLayout);
        RotationControlPanelLayout.setHorizontalGroup(
            RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RotationControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(MakeZeroButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(RotationControlLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, RotationControlPanelLayout.createSequentialGroup()
                        .addComponent(LeftButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(AngleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(AnglePositionField))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(RightButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, RotationControlPanelLayout.createSequentialGroup()
                        .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(DelayLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE)
                            .addComponent(StepAngleComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(StepAngleLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(DelayField)
                            .addComponent(LineCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE))))
                .addContainerGap())
        );
        RotationControlPanelLayout.setVerticalGroup(
            RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RotationControlPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(RotationControlLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(AngleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(LeftButton)
                        .addComponent(RightButton))
                    .addComponent(AnglePositionField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(MakeZeroButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(LineCheckBox)
                    .addGroup(RotationControlPanelLayout.createSequentialGroup()
                        .addComponent(StepAngleLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(StepAngleComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(6, 6, 6)
                .addGroup(RotationControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(DelayLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(DelayField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        RotationPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        RotationLabel.setText("Rotation Angle");

        ShutterRotationCheckBox.setText("Shutter?");
        ShutterRotationCheckBox.setToolTipText("Close shutter after acquiring a full rotation");
        ShutterRotationCheckBox.setEnabled(false);
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
        RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "45", "90", "180", "360", "18", "9", "1.8" }));
        RotationComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
        RotationComboBox.setToolTipText("Angle desired for acquisition");
        RotationComboBox.setEnabled(false);
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
        RotationMotorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        RotationMotorComboBox.setModel(new javax.swing.DefaultComboBoxModel(fd3));
        RotationMotorComboBox.setToolTipText("Rotation motor selected");
        RotationMotorComboBox.setEnabled(false);
        RotationMotorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RotationMotorComboBoxActionPerformed(evt);
            }
        });

        RotationSampleCheckBox.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        RotationSampleCheckBox.setText("Rotation");
        RotationSampleCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RotationSampleCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout RotationPanelLayout = new javax.swing.GroupLayout(RotationPanel);
        RotationPanel.setLayout(RotationPanelLayout);
        RotationPanelLayout.setHorizontalGroup(
            RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RotationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(RotationSampleCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(RotationPanelLayout.createSequentialGroup()
                        .addGroup(RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(ShutterRotationCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(RotationLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(RotationComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(RotationMotorComboBox, 0, 94, Short.MAX_VALUE))))
                .addContainerGap())
        );
        RotationPanelLayout.setVerticalGroup(
            RotationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RotationPanelLayout.createSequentialGroup()
                .addComponent(RotationSampleCheckBox)
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
                        .addComponent(IntervalLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(IntervalTime, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(IntervalUnits, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(TimeLapsePanelLayout.createSequentialGroup()
                        .addComponent(NumberLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(TimeFrames))
                    .addComponent(TimeLapseCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addGroup(TimeLapsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(IntervalTime)
                    .addComponent(IntervalUnits)
                    .addComponent(IntervalLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(29, 29, 29))
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
        ShutterButton.setToolTipText("Turn On or Off the Laser");
        ShutterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShutterButtonActionPerformed(evt);
            }
        });

        CheckAlignButton.setText("Check Alignment");
        CheckAlignButton.setToolTipText("Align Lasers at Center of FoV");
        CheckAlignButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CheckAlignButtonActionPerformed(evt);
            }
        });

        //JComponent ChannelTable = new Mytable();
        ChannelControlPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        ChannelControlPanel.setEnabled(false);
        //ChannelTable.setOpaque(true);
        //ChannelControlPanel.add(ChannelTable);

        Channel_CheckBox.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        Channel_CheckBox.setText("Channels");
        Channel_CheckBox.setToolTipText("Check for multiple channel acquisiton");
        Channel_CheckBox.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        Channel_CheckBox.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        Channel_CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Channel_CheckBoxActionPerformed(evt);
            }
        });

        Channeltable.setDefaultRenderer(Color.class, new ColorRenderer(true));
        Channeltable.setDefaultEditor(Color.class, new ColorEditor((AbstractTableModel) Channeltable.getModel(), 5));
        //Fiddle with the column's cell editors/renderers.
        JComboBox ExcitationComboBoxT = new JComboBox(Excitation_filters);
        JComboBox EmissionComboBoxT = new JComboBox(Emission_filters);
        JComboBox IntensityComboBoxT = new JComboBox(Intensity);
        setUpComboColumn(Channeltable, Channeltable.getColumnModel().getColumn(1), ExcitationComboBoxT);
        setUpComboColumn(Channeltable, Channeltable.getColumnModel().getColumn(2), EmissionComboBoxT);
        setUpComboColumn(Channeltable, Channeltable.getColumnModel().getColumn(4), IntensityComboBoxT);
        Channeltable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        Channeltable.setEnabled(false);
        ChannelScrollPane.setViewportView(Channeltable);

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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(Excitation_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Excitation_ComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Intensity_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(Intensity_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Emission_label)
                            .addComponent(Emission_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Excitation_label)
                    .addComponent(Emission_label))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Excitation_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Emission_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Intensity_label)
                    .addComponent(Intensity_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout ChannelControlPanelLayout = new javax.swing.GroupLayout(ChannelControlPanel);
        ChannelControlPanel.setLayout(ChannelControlPanelLayout);
        ChannelControlPanelLayout.setHorizontalGroup(
            ChannelControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ChannelControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Channel_CheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ChannelScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        ChannelControlPanelLayout.setVerticalGroup(
            ChannelControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ChannelControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ChannelControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Channel_CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(ChannelControlPanelLayout.createSequentialGroup()
                        .addComponent(ChannelScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
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

        DataSetSizeField.setEditable(false);
        DataSetSizeField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        DataSetSizeField.setText("DataSet Size");
        DataSetSizeField.setPreferredSize(new java.awt.Dimension(120, 20));
        DataSetSizeField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DataSetSizeFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout SaveImagesPanelLayout = new javax.swing.GroupLayout(SaveImagesPanel);
        SaveImagesPanel.setLayout(SaveImagesPanelLayout);
        SaveImagesPanelLayout.setHorizontalGroup(
            SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SaveImagesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(SaveImagesPanelLayout.createSequentialGroup()
                        .addComponent(SaveCheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(DataSetSizeField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(SaveImagesPanelLayout.createSequentialGroup()
                        .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(RootLabel)
                            .addComponent(FileLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(FileNameField, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
                            .addComponent(RootDirectoryField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(DirectoryButton)))
                .addContainerGap())
        );
        SaveImagesPanelLayout.setVerticalGroup(
            SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SaveImagesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(SaveCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(DataSetSizeField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(RootLabel)
                    .addComponent(RootDirectoryField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(DirectoryButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(SaveImagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(FileNameField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(FileLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                    .addComponent(Message_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 297, Short.MAX_VALUE)))
        );
        Message_PannelLayout.setVerticalGroup(
            Message_PannelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Message_PannelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(MessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Message_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
        );

        XYPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        XYListButton.setText("XY List");
        XYListButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                XYListButtonActionPerformed(evt);
            }
        });

        XYStageControl.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        XYStageControl.setText("XYStage Control");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(XYListButton)
                .addGap(28, 28, 28)
                .addComponent(XYStageControl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(XYListButton)
                    .addComponent(XYStageControl))
                .addGap(2, 2, 2))
        );

        YUPUPButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        YUPUPButton.setText("UP");
        YUPUPButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                YUPUPButtonActionPerformed(evt);
            }
        });

        YUPButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        YUPButton.setText("˄");
        YUPButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                YUPButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(YUPUPButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(YUPButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(YUPUPButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(YUPButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 105, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 61, Short.MAX_VALUE)
        );

        YDownDownButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        YDownDownButton.setText("D");
        YDownDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                YDownDownButtonActionPerformed(evt);
            }
        });

        YDownButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        YDownButton.setText("˅");
        YDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                YDownButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(YDownButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(YDownDownButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addComponent(YDownButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(YDownDownButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        XLeftLeftButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        XLeftLeftButton.setText("L");
        XLeftLeftButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                XLeftLeftButtonActionPerformed(evt);
            }
        });

        XLeftButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        XLeftButton.setText("<");
        XLeftButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                XLeftButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(XLeftLeftButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(XLeftButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(XLeftLeftButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(XLeftButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        XYUpUpField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        XYUpUpField.setText("100");
        XYUpUpField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                XYUpUpFieldActionPerformed(evt);
            }
        });

        XYUpField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        XYUpField.setText("10");
        XYUpField.setPreferredSize(new java.awt.Dimension(24, 20));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(XYUpUpField, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(XYUpField, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(38, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(XYUpUpField, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(XYUpField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel9.setPreferredSize(new java.awt.Dimension(110, 25));

        XRightButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        XRightButton.setText(">");
        XRightButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                XRightButtonActionPerformed(evt);
            }
        });

        XRightRightButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        XRightRightButton.setText("R");
        XRightRightButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                XRightRightButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(XRightButton, javax.swing.GroupLayout.DEFAULT_SIZE, 45, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(XRightRightButton, javax.swing.GroupLayout.DEFAULT_SIZE, 46, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(XRightRightButton)
            .addComponent(XRightButton)
        );

        XMotorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        XMotorComboBox.setModel(new javax.swing.DefaultComboBoxModel(fd2));
        for (int i=0; i<fd2.length; i++){
            if (XMotorComboBox.getItemAt(i).equals("StageX")){
                XMotorComboBox.setSelectedIndex(i);
            }
        }
        XMotorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                XMotorComboBoxActionPerformed(evt);
            }
        });

        XPositionField.setBackground(new java.awt.Color(230, 230, 230));
        XPositionField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        XPositionField.setText("0");
        XPositionField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                XPositionFieldActionPerformed(evt);
            }
        });

        XMotorLabel.setText("X Motor");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(XPositionField)
                    .addComponent(XMotorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(XMotorComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addComponent(XMotorLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(XMotorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(XPositionField))
        );

        YMotorLabel.setText("Y Motor");

        YMotorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        YMotorComboBox.setModel(new javax.swing.DefaultComboBoxModel(fd2));
        for (int i=0; i<fd2.length; i++){
            if (YMotorComboBox.getItemAt(i).equals("StageY")){
                YMotorComboBox.setSelectedIndex(i);
            }
        }
        YMotorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                YMotorComboBoxActionPerformed(evt);
            }
        });

        YPositionField.setBackground(new java.awt.Color(230, 230, 230));
        YPositionField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        YPositionField.setText("0");
        YPositionField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                YPositionFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(YMotorLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(YMotorComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(YPositionField, javax.swing.GroupLayout.Alignment.TRAILING)))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addComponent(YMotorLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(YMotorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(YPositionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout XYPanelLayout = new javax.swing.GroupLayout(XYPanel);
        XYPanel.setLayout(XYPanelLayout);
        XYPanelLayout.setHorizontalGroup(
            XYPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(XYPanelLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(XYPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(XYPanelLayout.createSequentialGroup()
                        .addGroup(XYPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel10, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, XYPanelLayout.createSequentialGroup()
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(XYPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel12, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(10, 10, 10)
                        .addGroup(XYPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jPanel11, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel9, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE))))
                .addGap(16, 16, 16))
        );
        XYPanelLayout.setVerticalGroup(
            XYPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(XYPanelLayout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(XYPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(XYPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(XYPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(13, 13, 13))
        );

        CameraPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        CameraPanel.setToolTipText("");
        CameraPanel.setEnabled(false);

        FirstCamComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        FirstCamComboBox.setEnabled(false);
        String[] Camera_config=acq_.getCameraConfigs();
        FirstCamComboBox.setModel(new javax.swing.DefaultComboBoxModel(Camera_config));

        String Cam_name=mmc_.getCameraDevice();

        FirstCamComboBox.setSelectedItem(Cam_name);
        FirstCamComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FirstCamComboBoxActionPerformed(evt);
            }
        });

        SecondCamComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Single", "Dual" }));
        SecondCamComboBox.setEnabled(false);
        SecondCamComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SecondCamComboBoxActionPerformed(evt);
            }
        });

        CameraControlLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        CameraControlLabel.setText("Camera Control");
        CameraControlLabel.setEnabled(false);

        FirstCamLabel.setText("Camera");
        FirstCamLabel.setEnabled(false);

        SecondCamLabel.setText("Mode ");
        SecondCamLabel.setEnabled(false);

        javax.swing.GroupLayout CameraPanelLayout = new javax.swing.GroupLayout(CameraPanel);
        CameraPanel.setLayout(CameraPanelLayout);
        CameraPanelLayout.setHorizontalGroup(
            CameraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CameraPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(CameraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(CameraControlLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(CameraPanelLayout.createSequentialGroup()
                        .addGroup(CameraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(FirstCamLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(SecondCamLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(CameraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(SecondCamComboBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(FirstCamComboBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        CameraPanelLayout.setVerticalGroup(
            CameraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CameraPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(CameraControlLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(CameraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(FirstCamLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(FirstCamComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(CameraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(SecondCamLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SecondCamComboBox))
                .addContainerGap(26, Short.MAX_VALUE))
        );

        ModePanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        //String[] Modes = new String[]{"DSLM/SPIM", "OPT", "FLOW-SPIM", "VERTICAL-SPIM", "MULTIPOSITION", "DSLM_XYCZ", "Confocal_XYCZ", "SPIM_Trig"};
        ModeComboBox.setMaximumRowCount(6);
        ModeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "DSLM/SPIM", "SPIM_Trig", "MultiPos_Trig", "Confocal_Trig", "Test" }));
        ModeComboBox.setSelectedIndex(3);
        ModeComboBox.setAutoscrolls(true);
        ModeComboBox.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        ModeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ModeComboBoxActionPerformed(evt);
            }
        });

        ModeLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        ModeLabel.setText("Mode");

        javax.swing.GroupLayout ModePanelLayout = new javax.swing.GroupLayout(ModePanel);
        ModePanel.setLayout(ModePanelLayout);
        ModePanelLayout.setHorizontalGroup(
            ModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ModePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ModeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ModeComboBox, 0, 109, Short.MAX_VALUE)
                .addContainerGap())
        );
        ModePanelLayout.setVerticalGroup(
            ModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ModePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ModeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(ModePanelLayout.createSequentialGroup()
                        .addComponent(ModeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        PumpsPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        PumpsPanel.setEnabled(false);

        PumpsLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        PumpsLabel.setText("Pumps");
        PumpsLabel.setEnabled(false);

        VolumeTextField.setText("Volume");
        VolumeTextField.setEnabled(false);

        SpeedTextField.setText("Speed");
        SpeedTextField.setEnabled(false);

        DelayTextField.setText("Delay");
        DelayTextField.setEnabled(false);

        PumpsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0", "1" }));
        PumpsComboBox.setEnabled(false);
        PumpsComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                PumpsComboBoxItemStateChanged(evt);
            }
        });

        StartPjButton.setText("Start");
        StartPjButton.setEnabled(false);
        StartPjButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                StartPjButtonMouseClicked(evt);
            }
        });
        StartPjButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StartPjButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout PumpsPanelLayout = new javax.swing.GroupLayout(PumpsPanel);
        PumpsPanel.setLayout(PumpsPanelLayout);
        PumpsPanelLayout.setHorizontalGroup(
            PumpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PumpsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PumpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SpeedTextField)
                    .addComponent(PumpsComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(PumpsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(PumpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(PumpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(VolumeTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE)
                        .addComponent(DelayTextField))
                    .addComponent(StartPjButton))
                .addContainerGap())
        );
        PumpsPanelLayout.setVerticalGroup(
            PumpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PumpsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PumpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(PumpsLabel)
                    .addComponent(StartPjButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(PumpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(VolumeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(PumpsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(PumpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(SpeedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(DelayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        StageControlPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        UPUPButton.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        UPUPButton.setText("|<<");
        UPUPButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UPUPButtonActionPerformed(evt);
            }
        });

        UPButton.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        UPButton.setText("|<");
        UPButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UPButtonActionPerformed(evt);
            }
        });

        DownButton.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        DownButton.setText("|>");
        DownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DownButtonActionPerformed(evt);
            }
        });

        DownDownButton.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        DownDownButton.setText("|>>");
        DownDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DownDownButtonActionPerformed(evt);
            }
        });

        PositionField.setBackground(new java.awt.Color(230, 230, 230));
        PositionField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        PositionField.setText("0");
        PositionField.setText(StackPosition);
        PositionField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PositionFieldActionPerformed(evt);
            }
        });

        PositionLabel.setText("Position");

        UpUpField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        UpUpField.setText("100");

        UpField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        UpField.setText("10");

        StageControlLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        StageControlLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        StageControlLabel.setText("Z Motor");

        StageMotorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        StageMotorComboBox.setModel(new javax.swing.DefaultComboBoxModel(fd2));
        for (int i=0; i<fd2.length; i++){
            if (StageMotorComboBox.getItemAt(i).equals("StageZ")){
                StageMotorComboBox.setSelectedIndex(i);
            }
        }
        StageMotorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StageMotorComboBoxActionPerformed(evt);
            }
        });

        ZMotorLabel.setText("Z Motor");

        javax.swing.GroupLayout StageControlPanelLayout = new javax.swing.GroupLayout(StageControlPanel);
        StageControlPanel.setLayout(StageControlPanelLayout);
        StageControlPanelLayout.setHorizontalGroup(
            StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, StageControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(StageControlLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, StageControlPanelLayout.createSequentialGroup()
                        .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(ZMotorLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(PositionLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(UpField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(UpUpField)
                            .addComponent(StageMotorComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(DownButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(UPUPButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(UPButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(PositionField)
                            .addComponent(DownDownButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        StageControlPanelLayout.setVerticalGroup(
            StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StageControlPanelLayout.createSequentialGroup()
                .addComponent(StageControlLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(UPUPButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(UpUpField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(UPButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(UpField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(PositionField)
                    .addComponent(PositionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(DownButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ZMotorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(StageControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(DownDownButton)
                    .addComponent(StageMotorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        StackPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        StackCheckBox.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        StackCheckBox.setText("Stack of Images");
        StackCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                StackCheckBoxStateChanged(evt);
            }
        });
        StackCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StackCheckBoxActionPerformed(evt);
            }
        });

        StartField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        StartField.setText("0");
        StartField.setToolTipText("Starting position the stack in microns");
        StartField.setEnabled(false);
        StartField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StartFieldActionPerformed(evt);
            }
        });

        SetMiddleButton.setText("Go");
        SetMiddleButton.setEnabled(false);
        SetMiddleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetMiddleButtonActionPerformed(evt);
            }
        });

        EndField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        EndField.setText("10");
        EndField.setToolTipText("Finishing position of the stack in microns");
        EndField.setEnabled(false);

        SetStartButton.setText("Set");
        SetStartButton.setEnabled(false);
        SetStartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetStartButtonActionPerformed(evt);
            }
        });

        StepField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        StepField.setText("5");
        StepField.setToolTipText("Steps in micros between images of the stack");
        StepField.setEnabled(false);
        StepField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepFieldActionPerformed(evt);
            }
        });

        StartLabel.setText("Start (microns)");

        EndLabel.setText("End (microns)");

        StepLabel.setText("Steps (microns)");

        ShutterStackCheckBox.setText("Use Shutter");
        ShutterStackCheckBox.setEnabled(false);

        NumberofSlicesField.setEditable(false);
        NumberofSlicesField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        NumberofSlicesField.setText("22.000");
        NumberofSlicesField.setToolTipText("Total number of Slices");
        NumberofSlicesField.setEnabled(false);
        NumberofSlicesField.setText(String.valueOf(ts));
        NumberofSlicesField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NumberofSlicesFieldActionPerformed(evt);
            }
        });

        TimeLabel.setText("Slices | Time");

        TotalTimeField.setEditable(false);
        TotalTimeField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        TotalTimeField.setText("22.000");
        TotalTimeField.setToolTipText("Estimation of total acquisition time (s)");
        TotalTimeField.setEnabled(false);
        TotalTimeField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TotalTimeFieldActionPerformed(evt);
            }
        });

        MiddleLabel1.setText("Middle (microns)");

        MiddleField1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        MiddleField1.setText("0");
        MiddleField1.setToolTipText("Starting position the stack in microns");
        MiddleField1.setEnabled(false);
        MiddleField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MiddleField1ActionPerformed(evt);
            }
        });

        SetEndButton.setText("Set");
        SetEndButton.setEnabled(false);
        SetEndButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetEndButtonActionPerformed(evt);
            }
        });

        SetSliceButton.setText("Set");
        SetSliceButton.setEnabled(false);
        SetSliceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetSliceButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout StackPanelLayout = new javax.swing.GroupLayout(StackPanel);
        StackPanel.setLayout(StackPanelLayout);
        StackPanelLayout.setHorizontalGroup(
            StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StackPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(StackCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ShutterStackCheckBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(StackPanelLayout.createSequentialGroup()
                        .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(TimeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(StepLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(MiddleLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(EndLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(StartLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(NumberofSlicesField, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(StepField, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(EndField)
                            .addComponent(StartField, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(MiddleField1, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(SetMiddleButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(SetStartButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(SetEndButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(TotalTimeField)
                            .addComponent(SetSliceButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        StackPanelLayout.setVerticalGroup(
            StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StackPanelLayout.createSequentialGroup()
                .addComponent(StackCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(StartLabel)
                    .addComponent(SetStartButton)
                    .addComponent(StartField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(MiddleLabel1)
                    .addComponent(MiddleField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SetMiddleButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(EndLabel)
                    .addComponent(EndField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SetEndButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(StepLabel)
                        .addComponent(StepField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(SetSliceButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(StackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(TimeLabel)
                    .addComponent(NumberofSlicesField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(TotalTimeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ShutterStackCheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GalvoControlPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        GalvoControlPanel1.setToolTipText("<html> <p>Define the parameters for illumination 1 (Right Side). \n<p>For normal trigger mode use: \n<p>\t-100 %;\n<p>\t-10 ms; \n<p>\t-(Exposure time/10); \n<p>For confocal trigger mode use: \n<p>\t-100 %; \n<p>\t-100 ms (=Exposure time); \n<p>\t-1; ");

        GalvoCheckBox1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        GalvoCheckBox1.setText("Galvo 1");
        GalvoCheckBox1.setToolTipText("<html> <p>Define the parameters for illumination 1 (Right Side). \n<p>For normal trigger mode use: \n<p>\t-100 %;\n<p>\t-10 ms; \n<p>\t-(Exposure time/10); \n<p>For confocal trigger mode use: \n<p>\t-100 %; \n<p>\t-100 ms (=Exposure time); \n<p>\t-1; ");
        GalvoCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GalvoCheckBox1ActionPerformed(evt);
            }
        });

        GAmpTextField1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        GAmpTextField1.setText("100");
        GAmpTextField1.setToolTipText("Galvo 1 Amplitude 0-100");
        GAmpTextField1.setEnabled(false);
        GAmpTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GAmpTextField1ActionPerformed(evt);
            }
        });

        GAmpLabel1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        GAmpLabel1.setText("Amp.");
        GAmpLabel1.setEnabled(false);

        GFreqTextField1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        GFreqTextField1.setText("100");
        GFreqTextField1.setToolTipText("Galvo 1 Cicle's Period");
        GFreqTextField1.setEnabled(false);
        GFreqTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GFreqTextField1ActionPerformed(evt);
            }
        });

        GFreqLabel1.setText("Period (ms)");
        GFreqLabel1.setEnabled(false);

        GMoveButton1.setFont(new java.awt.Font("Tahoma", 1, 8)); // NOI18N
        GMoveButton1.setText("DEFINE");
        GMoveButton1.setToolTipText("Define Galvo 1 Preferences for acquisition");
        GMoveButton1.setEnabled(false);
        GMoveButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GMoveButton1ActionPerformed(evt);
            }
        });

        GSetButton1.setFont(new java.awt.Font("Tahoma", 1, 8)); // NOI18N
        GSetButton1.setText("SET");
        GSetButton1.setToolTipText("Set Galvo 1 at defined pos");
        GSetButton1.setEnabled(false);
        GSetButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GSetButton1ActionPerformed(evt);
            }
        });

        GAmpLabel2.setText("Cycles");
        GAmpLabel2.setEnabled(false);

        G1RunTime.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        G1RunTime.setText("60");
        G1RunTime.setToolTipText("Time in seconds");
        G1RunTime.setEnabled(false);

        GAmpTextField2.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        GAmpTextField2.setText("1");
        GAmpTextField2.setToolTipText("Number of scans that Galvo 1 will do");
        GAmpTextField2.setEnabled(false);

        G1Run.setFont(new java.awt.Font("Tahoma", 1, 8)); // NOI18N
        G1Run.setText("RUN");
        G1Run.setToolTipText("Moves galvo 2 for X seconds");
        G1Run.setEnabled(false);
        G1Run.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                G1RunActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout GalvoControlPanel1Layout = new javax.swing.GroupLayout(GalvoControlPanel1);
        GalvoControlPanel1.setLayout(GalvoControlPanel1Layout);
        GalvoControlPanel1Layout.setHorizontalGroup(
            GalvoControlPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(GalvoControlPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(GalvoControlPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(GalvoCheckBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(GalvoControlPanel1Layout.createSequentialGroup()
                        .addGroup(GalvoControlPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(GAmpLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(GMoveButton1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(GAmpLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(GFreqLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(G1Run, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(10, 10, 10)
                        .addGroup(GalvoControlPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(GFreqTextField1)
                            .addComponent(GSetButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                            .addComponent(GAmpTextField2)
                            .addComponent(G1RunTime)
                            .addComponent(GAmpTextField1))))
                .addContainerGap())
        );
        GalvoControlPanel1Layout.setVerticalGroup(
            GalvoControlPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(GalvoControlPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(GalvoCheckBox1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(GalvoControlPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(GAmpLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(GAmpTextField1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GalvoControlPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(GFreqLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(GFreqTextField1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GalvoControlPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(GAmpLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(GAmpTextField2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GalvoControlPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(G1RunTime)
                    .addComponent(G1Run, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GalvoControlPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(GMoveButton1)
                    .addComponent(GSetButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        GalvoControlPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        GalvoControlPanel2.setToolTipText("<html>\n<p>Define the parameters for illumination 2 (Left Side).\n<p>For normal trigger mode use:\n<p>\t-100 %;\n<p>\t-10 ms;\n<p>\t-(Exposure time/10);\n<p>For confocal trigger mode use:\n<p>\t-100 %;\n<p>\t-100 ms (=Exposure time);\n<p>\t-1;\n");

        GalvoCheckBox2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        GalvoCheckBox2.setText("Galvo 2");
        GalvoCheckBox2.setToolTipText("<html>\n<p>Define the parameters for illumination 2 (Left Side).\n<p>For normal trigger mode use:\n<p>\t-100 %;\n<p>\t-10 ms;\n<p>\t-(Exposure time/10);\n<p>For confocal trigger mode use:\n<p>\t-100 %;\n<p>\t-100 ms (=Exposure time);\n<p>\t-1;\n");
        GalvoCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GalvoCheckBox2ActionPerformed(evt);
            }
        });

        GAmpTextField3.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        GAmpTextField3.setText("100");
        GAmpTextField3.setToolTipText("Galvo 1 Amplitude 0-100");
        GAmpTextField3.setEnabled(false);
        GAmpTextField3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GAmpTextField3ActionPerformed(evt);
            }
        });

        GAmpLabel3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        GAmpLabel3.setText(" Amp.");
        GAmpLabel3.setEnabled(false);

        GFreqTextField3.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        GFreqTextField3.setText("100");
        GFreqTextField3.setToolTipText("Galvo 2 Cicle's Period");
        GFreqTextField3.setEnabled(false);
        GFreqTextField3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GFreqTextField3ActionPerformed(evt);
            }
        });

        GFreqLabel3.setText("Period (ms)");
        GFreqLabel3.setEnabled(false);

        GMoveButton2.setFont(new java.awt.Font("Tahoma", 1, 8)); // NOI18N
        GMoveButton2.setText("DEFINE");
        GMoveButton2.setToolTipText("Define Galvo 1 Preferences for acquisition");
        GMoveButton2.setEnabled(false);
        GMoveButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GMoveButton2ActionPerformed(evt);
            }
        });

        GSetButton2.setFont(new java.awt.Font("Tahoma", 1, 8)); // NOI18N
        GSetButton2.setText("SET");
        GSetButton2.setToolTipText("Set Galvo 2 at defined pos");
        GSetButton2.setEnabled(false);
        GSetButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GSetButton2ActionPerformed(evt);
            }
        });

        GAmpLabel4.setText("Cycles");
        GAmpLabel4.setEnabled(false);

        G2RunTime.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        G2RunTime.setText("60");
        G2RunTime.setToolTipText("Time in seconds");
        G2RunTime.setEnabled(false);
        G2RunTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                G2RunTimeActionPerformed(evt);
            }
        });

        GAmpTextField4.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        GAmpTextField4.setText("1");
        GAmpTextField4.setToolTipText("Number of scans that Galvo 2 will do");
        GAmpTextField4.setEnabled(false);

        G2Run.setFont(new java.awt.Font("Tahoma", 1, 8)); // NOI18N
        G2Run.setText("RUN");
        G2Run.setToolTipText("Moves galvo 2 for X seconds");
        G2Run.setEnabled(false);
        G2Run.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                G2RunActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout GalvoControlPanel2Layout = new javax.swing.GroupLayout(GalvoControlPanel2);
        GalvoControlPanel2.setLayout(GalvoControlPanel2Layout);
        GalvoControlPanel2Layout.setHorizontalGroup(
            GalvoControlPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(GalvoControlPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(GalvoControlPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(GalvoCheckBox2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(GalvoControlPanel2Layout.createSequentialGroup()
                        .addGroup(GalvoControlPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(GFreqLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(GAmpLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(GAmpLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(GMoveButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(G2Run, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
                        .addGroup(GalvoControlPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(GSetButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 55, Short.MAX_VALUE)
                            .addGroup(GalvoControlPanel2Layout.createSequentialGroup()
                                .addGroup(GalvoControlPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(GFreqTextField3, javax.swing.GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE)
                                    .addComponent(GAmpTextField4, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(GAmpTextField3)
                                    .addComponent(G2RunTime))
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        GalvoControlPanel2Layout.setVerticalGroup(
            GalvoControlPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(GalvoControlPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(GalvoCheckBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(GalvoControlPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(GAmpTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(GAmpLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GalvoControlPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(GFreqLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 19, Short.MAX_VALUE)
                    .addComponent(GFreqTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GalvoControlPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(GAmpTextField4, javax.swing.GroupLayout.DEFAULT_SIZE, 21, Short.MAX_VALUE)
                    .addComponent(GAmpLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GalvoControlPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(G2RunTime)
                    .addComponent(G2Run, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addGroup(GalvoControlPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(GMoveButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
                    .addComponent(GSetButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout AcquisitionLayout = new javax.swing.GroupLayout(Acquisition);
        Acquisition.setLayout(AcquisitionLayout);
        AcquisitionLayout.setHorizontalGroup(
            AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AcquisitionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(AcquisitionLayout.createSequentialGroup()
                        .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(CameraPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(PumpsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ModePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(RotationControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(XYPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(AcquisitionLayout.createSequentialGroup()
                        .addComponent(SaveImagesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Message_Pannel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(ChannelControlPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(AcquisitionLayout.createSequentialGroup()
                        .addComponent(StageControlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(StackPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(RotationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(TimeLapsePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(GalvoControlPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(CheckAlignButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ShutterButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(AcquireButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(GalvoControlPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        AcquisitionLayout.setVerticalGroup(
            AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AcquisitionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(AcquisitionLayout.createSequentialGroup()
                        .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, AcquisitionLayout.createSequentialGroup()
                                .addComponent(AcquireButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(ShutterButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CheckAlignButton, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(RotationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(6, 6, 6)
                        .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(GalvoControlPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(TimeLapsePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(GalvoControlPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(AcquisitionLayout.createSequentialGroup()
                        .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(StageControlPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(XYPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(StackPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(AcquisitionLayout.createSequentialGroup()
                                .addComponent(CameraPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(PumpsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(AcquisitionLayout.createSequentialGroup()
                                .addComponent(ChannelControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(AcquisitionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(Message_Pannel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(SaveImagesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(AcquisitionLayout.createSequentialGroup()
                                .addComponent(ModePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(RotationControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Acquisition", Acquisition);

        LaserPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        LedLabel.setText("Laser");

        LedOnOffLabel.setText("ON/OFF");

        LedIntLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LedIntLabel.setText("Intensity");

        LaserCheckBox1.setText("1           ");
        LaserCheckBox1.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        LaserCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LaserCheckBox1ActionPerformed(evt);
            }
        });

        LaserCheckBox2.setText("2           ");
        LaserCheckBox2.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        LaserCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LaserCheckBox2ActionPerformed(evt);
            }
        });

        LaserCheckBox3.setText("3           ");
        LaserCheckBox3.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        LaserCheckBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LaserCheckBox3ActionPerformed(evt);
            }
        });

        LaserSlider1.setMajorTickSpacing(10);
        LaserSlider1.setMinorTickSpacing(10);
        LaserSlider1.setValue(0);
        LaserSlider1.setEnabled(false);
        LaserSlider1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                LaserSlider1StateChanged(evt);
            }
        });

        LaserCheckBox4.setText("4           ");
        LaserCheckBox4.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        LaserCheckBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LaserCheckBox4ActionPerformed(evt);
            }
        });

        LaserSlider2.setMajorTickSpacing(10);
        LaserSlider2.setMinorTickSpacing(10);
        LaserSlider2.setValue(0);
        LaserSlider2.setEnabled(false);
        LaserSlider2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                LaserSlider2StateChanged(evt);
            }
        });

        LaserSlider3.setMajorTickSpacing(10);
        LaserSlider3.setMinorTickSpacing(10);
        LaserSlider3.setValue(0);
        LaserSlider3.setEnabled(false);
        LaserSlider3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                LaserSlider3StateChanged(evt);
            }
        });

        LaserSlider4.setMajorTickSpacing(10);
        LaserSlider4.setMinorTickSpacing(10);
        LaserSlider4.setValue(0);
        LaserSlider4.setEnabled(false);
        LaserSlider4.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                LaserSlider4StateChanged(evt);
            }
        });

        javax.swing.GroupLayout LaserPanelLayout = new javax.swing.GroupLayout(LaserPanel);
        LaserPanel.setLayout(LaserPanelLayout);
        LaserPanelLayout.setHorizontalGroup(
            LaserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(LaserPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(LaserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(LaserPanelLayout.createSequentialGroup()
                        .addComponent(LedLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(LedOnOffLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 41, Short.MAX_VALUE))
                    .addComponent(LaserCheckBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LaserCheckBox2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LaserCheckBox3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LaserCheckBox4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(LaserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(LaserSlider1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(LaserSlider2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(LaserSlider3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(LaserSlider4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(LedIntLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        LaserPanelLayout.setVerticalGroup(
            LaserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LaserPanelLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(LaserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LedLabel)
                    .addComponent(LedOnOffLabel)
                    .addComponent(LedIntLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(LaserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(LaserSlider1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LaserCheckBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(LaserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(LaserSlider2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LaserCheckBox2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(LaserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(LaserSlider3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LaserCheckBox3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(LaserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(LaserSlider4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LaserCheckBox4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
        StepTypeComboBox1.setSelectedIndex(3);
        StepTypeComboBox1.setEnabled(false);
        StepTypeComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepTypeComboBox1ActionPerformed(evt);
            }
        });

        StepTypeComboBox3.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "FULL", "HALF", "QUARTER", "EIGHTH" }));
        StepTypeComboBox3.setSelectedIndex(3);
        StepTypeComboBox3.setEnabled(false);
        StepTypeComboBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepTypeComboBox3ActionPerformed(evt);
            }
        });

        StepTypeComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "FULL", "HALF", "QUARTER", "EIGHTH" }));
        StepTypeComboBox2.setSelectedIndex(3);
        StepTypeComboBox2.setEnabled(false);
        StepTypeComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepTypeComboBox2ActionPerformed(evt);
            }
        });

        StepTypeComboBox4.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "FULL", "HALF", "QUARTER", "EIGHTH" }));
        StepTypeComboBox4.setSelectedIndex(3);
        StepTypeComboBox4.setEnabled(false);
        StepTypeComboBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StepTypeComboBox4ActionPerformed(evt);
            }
        });

        VelocityLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        VelocityLabel.setText("VELOCITY");
        VelocityLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        MotorSlider1.setMajorTickSpacing(10);
        MotorSlider1.setMinorTickSpacing(10);
        MotorSlider1.setPaintTicks(true);
        MotorSlider1.setEnabled(false);
        MotorSlider1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                MotorSlider1StateChanged(evt);
            }
        });

        MotorSlider2.setMajorTickSpacing(10);
        MotorSlider2.setMinorTickSpacing(10);
        MotorSlider2.setPaintTicks(true);
        MotorSlider2.setEnabled(false);
        MotorSlider2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                MotorSlider2StateChanged(evt);
            }
        });

        MotorSlider3.setMajorTickSpacing(10);
        MotorSlider3.setMinorTickSpacing(10);
        MotorSlider3.setPaintTicks(true);
        MotorSlider3.setEnabled(false);
        MotorSlider3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                MotorSlider3StateChanged(evt);
            }
        });

        MotorSlider4.setMajorTickSpacing(10);
        MotorSlider4.setMinorTickSpacing(10);
        MotorSlider4.setPaintTicks(true);
        MotorSlider4.setEnabled(false);
        MotorSlider4.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                MotorSlider4StateChanged(evt);
            }
        });

        FunctionLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        FunctionLabel.setText("FUNCTION");

        FunctionComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "EmissionW", "IntensityW", "SRotation", "ExcitationW" }));
        FunctionComboBox1.setEnabled(false);
        FunctionComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FunctionComboBox1ActionPerformed(evt);
            }
        });

        FunctionComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "EmissionW", "IntensityW", "SRotation", "ExcitationW" }));
        FunctionComboBox2.setSelectedIndex(1);
        FunctionComboBox2.setEnabled(false);
        FunctionComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FunctionComboBox2ActionPerformed(evt);
            }
        });

        FunctionComboBox3.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "EmissionW", "IntensityW", "SRotation", "ExcitationW" }));
        FunctionComboBox3.setSelectedIndex(2);
        FunctionComboBox3.setEnabled(false);
        FunctionComboBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FunctionComboBox3ActionPerformed(evt);
            }
        });

        FunctionComboBox4.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "EmissionW", "IntensityW", "SRotation", "ExcitationW" }));
        FunctionComboBox4.setSelectedIndex(3);
        FunctionComboBox4.setEnabled(false);
        FunctionComboBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FunctionComboBox4ActionPerformed(evt);
            }
        });

        RestoreButton.setText("RESET");
        RestoreButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RestoreButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout MotorPanelLayout = new javax.swing.GroupLayout(MotorPanel);
        MotorPanel.setLayout(MotorPanelLayout);
        MotorPanelLayout.setHorizontalGroup(
            MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MotorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(MotorCheckBox3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(MotorCheckBox2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(MotorCheckBox1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, MotorPanelLayout.createSequentialGroup()
                        .addComponent(MotorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(MotorOnOffLabel))
                    .addComponent(MotorCheckBox4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(StepsComboBox1, 0, 60, Short.MAX_VALUE)
                    .addComponent(StepsComboBox2, 0, 60, Short.MAX_VALUE)
                    .addComponent(StepsComboBox3, 0, 60, Short.MAX_VALUE)
                    .addComponent(StepsComboBox4, 0, 60, Short.MAX_VALUE)
                    .addComponent(StepsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(StepTypeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepTypeComboBox3, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepTypeComboBox2, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepTypeComboBox1, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepTypeComboBox4, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(MotorSlider2, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(MotorSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(MotorSlider3, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(MotorSlider4, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(VelocityLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(FunctionComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(FunctionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(FunctionComboBox2, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(FunctionComboBox3, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(FunctionComboBox4, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(178, 178, 178)
                .addComponent(RestoreButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        MotorPanelLayout.setVerticalGroup(
            MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MotorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(FunctionLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(MotorLabel)
                        .addComponent(MotorOnOffLabel)
                        .addComponent(StepsLabel)
                        .addComponent(StepTypeLabel)
                        .addComponent(VelocityLabel)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(MotorSlider1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(MotorCheckBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepsComboBox1)
                    .addComponent(StepTypeComboBox1)
                    .addComponent(FunctionComboBox1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(MotorSlider2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(MotorCheckBox2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepTypeComboBox2)
                    .addComponent(StepsComboBox2)
                    .addComponent(FunctionComboBox2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(MotorSlider3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(StepsComboBox3)
                    .addComponent(StepTypeComboBox3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(FunctionComboBox3)
                    .addComponent(MotorCheckBox3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(RestoreButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(MotorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(FunctionComboBox4, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(StepTypeComboBox4, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(MotorSlider4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(StepsComboBox4, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(MotorCheckBox4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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

        LaserButton.setText("LASER");
        LaserButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LaserButtonActionPerformed(evt);
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

        FilterButton.setText("FILTER");
        FilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FilterButtonActionPerformed(evt);
            }
        });

        GalvoButton.setText("GALVO");
        GalvoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GalvoButtonActionPerformed(evt);
            }
        });

        StageButton.setText("STAGE");
        StageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StageButtonActionPerformed(evt);
            }
        });

        LedPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        LedPanel.setEnabled(false);

        LedLabel1.setText("Led");

        LedOnOffLabel1.setText("ON/OFF");

        LedIntLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LedIntLabel1.setText("Intensity");

        LedCheckBox1.setText("1           ");
        LedCheckBox1.setEnabled(false);
        LedCheckBox1.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        LedCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LedCheckBox1ActionPerformed(evt);
            }
        });

        LedCheckBox2.setText("2           ");
        LedCheckBox2.setEnabled(false);
        LedCheckBox2.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        LedCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LedCheckBox2ActionPerformed(evt);
            }
        });

        LedCheckBox3.setText("3           ");
        LedCheckBox3.setEnabled(false);
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
        LedCheckBox4.setEnabled(false);
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
                .addContainerGap()
                .addGroup(LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(LedPanelLayout.createSequentialGroup()
                        .addComponent(LedLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(LedOnOffLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(LedCheckBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedCheckBox2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedCheckBox3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedCheckBox4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(LedSlider1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(LedSlider2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(LedSlider3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(LedSlider4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(LedIntLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        LedPanelLayout.setVerticalGroup(
            LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LedPanelLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LedLabel1)
                    .addComponent(LedOnOffLabel1)
                    .addComponent(LedIntLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(LedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(LedSlider1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedCheckBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

        javax.swing.GroupLayout SettingsLayout = new javax.swing.GroupLayout(Settings);
        Settings.setLayout(SettingsLayout);
        SettingsLayout.setHorizontalGroup(
            SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SettingsLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(SettingsLayout.createSequentialGroup()
                        .addGroup(SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(LaserButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(MotorButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(HelpButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(FilterButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(GalvoButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(StageButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 986, Short.MAX_VALUE))
                    .addGroup(SettingsLayout.createSequentialGroup()
                        .addComponent(LaserPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(MotorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(LedPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        SettingsLayout.setVerticalGroup(
            SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(MotorPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LaserPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LedPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(SettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(SettingsLayout.createSequentialGroup()
                        .addComponent(HelpButton)
                        .addGap(18, 18, 18)
                        .addComponent(LaserButton)
                        .addGap(18, 18, 18)
                        .addComponent(MotorButton)
                        .addGap(18, 18, 18)
                        .addComponent(FilterButton)
                        .addGap(18, 18, 18)
                        .addComponent(GalvoButton)
                        .addGap(18, 18, 18)
                        .addComponent(StageButton)
                        .addGap(18, 42, Short.MAX_VALUE)
                        .addComponent(jButton1))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Settings", Settings);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
            mmc_.setProperty(Master, "Command", "MOTORROTATION=0;");
            MessageTextArea.setText(Rotationdevice); 
        }
        else if ("Motor_2".equals(RotationMotorComboBox.getSelectedItem()) && MotorCheckBox2.isSelected()){
            mmc_.setProperty(Master, "Command", "MOTORROTATION=1;");
            MessageTextArea.setText(Rotationdevice); 
        }
        else if ("Motor_3".equals(RotationMotorComboBox.getSelectedItem()) && MotorCheckBox3.isSelected()){
            mmc_.setProperty(Master, "Command", "MOTORROTATION=2;");
            MessageTextArea.setText(Rotationdevice); 
        }
        else if ("Motor_4".equals(RotationMotorComboBox.getSelectedItem()) && MotorCheckBox4.isSelected()){
            mmc_.setProperty(Master, "Command", "MOTORROTATION=3;");        
            MessageTextArea.setText(Rotationdevice); 
        }
        else{
            MessageTextArea.setText("Verify if the "+RotationMotorComboBox.getSelectedItem()+" is enable");
        }
        }      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
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
                String Mode="NO MODE";                                       
                Mode=(String) ModeComboBox.getSelectedItem();
                MessageTextArea.setText(Mode);         
                String Cam_name=mmc_.getCameraDevice();
                String Cam_mode=(String) SecondCamComboBox.getSelectedItem();
                ACQR_.Configure();  
                try {
                    mmc_.setProperty(Master, "CommandTerminator", "\r");
                    mmc_.setProperty(Master, "ResponseTerminator", "\r");
                } catch (Exception ex) {
                    Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                    MessageTextArea.setText("Error in Define Terminators");
                }
                int laser1 = LaserCheckBox1.isSelected() ? 1:0;
                int laser2 = LaserCheckBox2.isSelected() ? 1:0;
                int laser3 = LaserCheckBox3.isSelected() ? 1:0;
                int laser4 = LaserCheckBox4.isSelected() ? 1:0;
                int lasert = laser1+laser2+laser3+laser4;

    ////////////////  ACQUISITION IN DSLM/SPIM Mode //////////////////                  
                if ("DSLM/SPIM".equals(Mode)& "Single".equals(Cam_mode)){
                    try {
                        ACQR_.DSLM();
                    } catch (Exception ex) {
                        Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                        MessageTextArea.setText("Error in DSLM/SPIM Mode");
                    }
                }//END IF
    
////////////////  ACQUISITION IN DSLM/SPIM Mode //////////////////                  
//    if ("DSLM/SPIM".equals(Mode)& "Dual".equals(Cam_mode)){
//            try {
//                ACQR_.DSLM_Dual();
//            } catch (Exception ex) {
//                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
//                MessageTextArea.setText("Error in DSLM_Dual Mode");
//            }
//        }//END IF
    
   ////////////////  ACQUISITION IN XYCZ (Channels are acquire first)  //////////////////   
//   if("DSLM_XYCZ".equals(Mode)){
//          try {
//                ACQR_.DSLM_XYCZ(); 
//                } catch (Exception ex) {
//                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
//                MessageTextArea.setText("Error in DSLM_XYCZ Mode");
//            }
//        }
//   
//   if("Confocal_XYCZ".equals(Mode)){
//          try {
//                ACQR_.Confocal_XYCZ(); 
//                } catch (Exception ex) {
//                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
//                MessageTextArea.setText("Error in Confocal_XYCZ Mode");
//            }
//        }
   
   ////////////////  ACQUISITION IN OPT Mode (Slices are angles)  //////////////////
//     if("OPT".equals(Mode)){
//          try {
//            ACQR_.Configure();
//            if(ledt == ACQR_.channels){
//                 ACQR_.OPT(); 
//            }
//            else{
//                MessageTextArea.setText("Check if LED CheckBox is equal to channels");
//             }
//                       } catch (Exception ex) {
//                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
//                MessageTextArea.setText("Error in OPT Mode");
//            }
//        }

    if("Test".equals(Mode)){
        try{
            GMoveButton1.doClick();
            mmc_.sleep(100);
            GMoveButton2.doClick();
            mmc_.sleep(100);
            ACQR_.Configure();
            if(lasert == ACQR_.channels){
            ACQR_.Test();
            }
            else{
                MessageTextArea.setText("Check if LED CheckBox is equal to channels");
            }
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error in Test Mode");
        }
    }
    if("SPIM_Trig".equals(Mode)){
        try{
            GMoveButton1.doClick();
            mmc_.sleep(100);
            GMoveButton2.doClick();
            mmc_.sleep(100);
            ACQR_.Configure();
            if(lasert == ACQR_.channels){
            ACQR_.SPIM_Trig();
            }
            else{
                MessageTextArea.setText("Check if LED CheckBox is equal to channels");
            }
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error in SPIM_trig Mode");
        }
    }
    
    if("MultiPos_Trig".equals(Mode)){
        try{
            GMoveButton1.doClick();
            mmc_.sleep(100);
            GMoveButton2.doClick();
            mmc_.sleep(100);
            ACQR_.Configure();
            if(lasert == ACQR_.channels){
            ACQR_.MultiPos_Trig();
            }
            else{
                MessageTextArea.setText("Check if LED CheckBox is equal to channels");
            }
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error in MultiPos_trig Mode");
        }
    }
    
    if("Confocal_Trig".equals(Mode)){
        try{
            GMoveButton1.doClick();
            mmc_.sleep(100);
            GMoveButton2.doClick();
            mmc_.sleep(100);
            ACQR_.Configure();
            if(lasert == ACQR_.channels){
            ACQR_.Confocal_Trig();
            }
            else{
                MessageTextArea.setText("Check if LED CheckBox is equal to channels");
            }
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error in Confocal_trig Mode");
        }
    }
//          if("MultiPosition".equals(Mode)& "Single".equals(Cam_mode)){
//          try {
//                    ACQR_.Configure();
//                    ACQR_.MultiPosition(); 
//                       } catch (Exception ex) {
//                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
//                MessageTextArea.setText("Error in MultiPosition Mode");
//            }
//        }//END ELSE
            }
        };
        thread.start();
    }//GEN-LAST:event_AcquireButtonActionPerformed

    private void ShutterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShutterButtonActionPerformed
        // TODO add your handling code here:
                        
        if (Global.shutter==0){
           ShC_.openshutter();
        }
        else if (Global.shutter==1){
           ShC_.closeshutter();            
        }   
    }//GEN-LAST:event_ShutterButtonActionPerformed

    private void CheckAlignButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CheckAlignButtonActionPerformed
        // TODO add your handling code here:
        try {
            mmc_.setProperty(Master, "Command", "GALVO0ENABLE=0;");
            mmc_.sleep(100);            
            mmc_.setProperty(Master, "Command", "GALVO1ENABLE=0;");

            mmc_.setProperty(Galvo1, "Command", "GOTO=50;");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "Command", "GOTO=50;");
            mmc_.sleep(100);

            mmc_.setProperty(Master, "Command", "GALVO0ENABLE=1;");
            mmc_.sleep(100);
            mmc_.setProperty(Master, "Command", "GALVO1ENABLE=1;");
            
            IJ.makeLine(0, mmc_.getImageHeight()/2, mmc_.getImageWidth(), mmc_.getImageHeight()/2);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("ERROR IN SET GALVO #1");
        }
    }//GEN-LAST:event_CheckAlignButtonActionPerformed

    private void DirectoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DirectoryButtonActionPerformed
        // TODO add your handling code here:
        File f = FileDialogs.openDir(this, "Directory to save to",
                new FileDialogs.FileType("SaveDir", "Save Directory",
                        "D:\\Data", true, ""));
        RootDirectoryField.setText(f.getAbsolutePath());
        
        /*String Rootname=acq_.getRootName();        
       // String Rootname=ss.root;
        RootDirectoryField.setText(Rootname);
        //int returnVal = fc.showDialog(this, "Run Application");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal=fc.showOpenDialog(this);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
                    //.getCurrentDirectory();
            Rootname=file.getPath();
            RootDirectoryField.setText(Rootname);
            acq_.setRootName(Rootname);
            //ss.root=Rootname;
        }*/
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
        int Current_E       = Excitation_ComboBox.getSelectedIndex();
        int Current_Index   = Intensity_ComboBox.getSelectedIndex();
        String Current_Item =(String)Intensity_ComboBox.getSelectedItem();
        int Current_Int     = (int) Integer.parseInt(Current_Item);
        
        if(Current_E == 0){
            
            String Cex = FC_.ChangeIntensity(Current_E, Current_Index);
            MessageTextArea.setText("The Current Intensity is:  "+ Current_Item);

        }
        else{
            try{
                String Cint = FC_.ChangeIntensity(Current_E, Current_Int);
                } catch (Exception ex) {
                    Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
                    MessageTextArea.setText("Error in Intensity Filter: "+Current_Item);
                }
            MessageTextArea.setText("The Current Intensity is:  "+ Current_Item); 
        }
        switch (Current_E){
            case 0:
                LaserSlider1.setValue(Current_Int);
                break;
            case 1:
                LaserSlider2.setValue(Current_Int);
                break;
            case 2:
                LaserSlider3.setValue(Current_Int);
                break;
            case 3:
                LaserSlider4.setValue(Current_Int);
                break;
            default :
                break;
        }

        
    }//GEN-LAST:event_Intensity_ComboBoxActionPerformed

    private void Emission_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Emission_ComboBoxActionPerformed
        // TODO add your handling code here:
        int Current_Item    =Emission_ComboBox.getSelectedIndex();
        String CI           =FC_.ChangeEmissionFilter(Current_Item);
        String CIN          =Emission_ComboBox.getItemAt(Current_Item);
        MessageTextArea.setText("The Current Emission Filter is:  "+ CIN);
    }//GEN-LAST:event_Emission_ComboBoxActionPerformed

    private void Excitation_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Excitation_ComboBoxActionPerformed
        // TODO add your handling code here:
        int Current_Item    =Excitation_ComboBox.getSelectedIndex();
        String CI           =FC_.ChangeExcitationLaser(Current_Item);
        String CIN          =Excitation_ComboBox.getItemAt(Current_Item);
        MessageTextArea.setText("The Current Excitation Filter is:  "+ CIN);
        Global.shutter = 1;
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

    private void LaserCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LaserCheckBox1ActionPerformed
        // TODO add your handling code here:
        try{
            if(LaserCheckBox1.isSelected()){
                mmc_.setProperty(Master, "Command", "LASER0ENABLE=1;");
                LaserSlider1.setEnabled(true);
            }
            else{
                mmc_.setProperty(Master, "Command", "LASER0ENABLE=0;");
                LaserSlider1.setEnabled(false);
            }
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Laser Enable 1");
        }

    }//GEN-LAST:event_LaserCheckBox1ActionPerformed

    private void LaserCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LaserCheckBox2ActionPerformed
        // TODO add your handling code here:
        try{
            if(LaserCheckBox2.isSelected()){
                mmc_.setProperty(Master, "Command", "LASER1ENABLE=1;");
                LaserSlider2.setEnabled(true);
            }
            else{
                mmc_.setProperty(Master, "Command", "LASER1ENABLE=0;");
                LaserSlider2.setEnabled(false);
            }
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Laser Enable 2");
        }
    }//GEN-LAST:event_LaserCheckBox2ActionPerformed

    private void LaserCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LaserCheckBox3ActionPerformed
        // TODO add your handling code here:
        try{
            if(LaserCheckBox3.isSelected()){
                mmc_.setProperty(Master, "Command", "LASER2ENABLE=1;");
                LaserSlider3.setEnabled(true);
            }
            else{
                mmc_.setProperty(Master, "Command", "LASER2ENABLE=0;");
                LaserSlider3.setEnabled(false);
            }
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Laser Enable 3");
        }
    }//GEN-LAST:event_LaserCheckBox3ActionPerformed

    private void LaserCheckBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LaserCheckBox4ActionPerformed
        // TODO add your handling code here:
        try{
            if(LaserCheckBox4.isSelected()){
                mmc_.setProperty(Master, "Command", "LASER3ENABLE=1;");
                LaserSlider4.setEnabled(true);
            }
            else{
                mmc_.setProperty(Master, "Command", "LASER3ENABLE=0;");
                LaserSlider4.setEnabled(false);
            }
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Laser Enable 4");
        }
    }//GEN-LAST:event_LaserCheckBox4ActionPerformed

    private void LaserSlider1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LaserSlider1StateChanged
        // TODO add your handling code here:
        int value = LaserSlider1.getValue();
        int val = Math.round(value/10);
        try{
            FC_.ChangeIntensity(0, val);
            SettingsTextArea.setText("Laser 1 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Laser Intensity 1");
        }
    }//GEN-LAST:event_LaserSlider1StateChanged

    private void LaserSlider2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LaserSlider2StateChanged
        // TODO add your handling code here:
        int value = LaserSlider2.getValue();
        try{
            FC_.ChangeIntensity(1, value);
            SettingsTextArea.setText("Laser 2 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Laser Intensity 2");
        }
    }//GEN-LAST:event_LaserSlider2StateChanged

    private void LaserSlider3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LaserSlider3StateChanged
        // TODO add your handling code here:
        int value = LaserSlider3.getValue();
        try{
            FC_.ChangeIntensity(2, value);
            SettingsTextArea.setText("Laser 3 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Laser Intensity 3");
        }
    }//GEN-LAST:event_LaserSlider3StateChanged

    private void LaserSlider4StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LaserSlider4StateChanged
        // TODO add your handling code here:
        int value = LaserSlider4.getValue();
        try{
            FC_.ChangeIntensity(3, value);
            SettingsTextArea.setText("Laser 4 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Laser Intensity 4");
        }
    }//GEN-LAST:event_LaserSlider4StateChanged

    private void MotorCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MotorCheckBox1ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox1.getSelectedItem();
        int StepType=(int)StepTypeComboBox1.getSelectedIndex();
        SettingsTextArea.setText("StepType: "+StepType);
        int FW = FunctionComboBox1.getSelectedIndex();
        try{
            if(MotorCheckBox1.isSelected()){
                mmc_.setProperty(Master, "Command", "MOTOR0ENABLE=1;");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "MOTOR0STEPS="+Steps+";");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "MOTOR0STEPTYPE="+StepType+";");
                MotorSlider1.setEnabled(true);
                StepsComboBox1.setEnabled(true);
                StepTypeComboBox1.setEnabled(true);
                FunctionComboBox1.setEnabled(true);
            }
            else{
                mmc_.setProperty(Master, "Command", "MOTOR0ENABLE=0;");
                MotorSlider1.setEnabled(false);
                StepsComboBox1.setEnabled(false);
                StepTypeComboBox1.setEnabled(false);
                FunctionComboBox1.setEnabled(false);
                mmc_.setProperty(Master, "Command", "FILTER"+FW+"ENABLE=0;");
            }
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Enable 1");
        }
    }//GEN-LAST:event_MotorCheckBox1ActionPerformed

    private void MotorCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MotorCheckBox2ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox2.getSelectedItem();
        int StepType=(int)StepTypeComboBox2.getSelectedIndex();
        int FW = FunctionComboBox2.getSelectedIndex();
        try{
            if(MotorCheckBox2.isSelected()){
                mmc_.setProperty(Master, "Command", "MOTOR1ENABLE=1;");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "MOTOR1STEPS="+Steps+";");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "MOTOR1STEPTYPE="+StepType+";");
                MotorSlider2.setEnabled(true);
                StepsComboBox2.setEnabled(true);
                StepTypeComboBox2.setEnabled(true);
                FunctionComboBox2.setEnabled(true);
            }
            else{
                mmc_.setProperty(Master, "Command", "MOTOR1ENABLE=0;");
                MotorSlider2.setEnabled(false);
                StepsComboBox2.setEnabled(false);
                StepTypeComboBox2.setEnabled(false);
                FunctionComboBox2.setEnabled(false);
                mmc_.setProperty(Master, "Command", "FILTER"+FW+"ENABLE=0;");
            }
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Enable 2");
        }
    }//GEN-LAST:event_MotorCheckBox2ActionPerformed

    private void MotorCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MotorCheckBox3ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox3.getSelectedItem();
        int StepType=(int)StepTypeComboBox3.getSelectedIndex();
        int FW = FunctionComboBox3.getSelectedIndex();
        try{
            if(MotorCheckBox3.isSelected()){
                mmc_.setProperty(Master, "Command", "MOTOR2ENABLE=1;");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "MOTOR2STEPS="+Steps+";");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "MOTOR2STEPTYPE="+StepType+";");
                MotorSlider3.setEnabled(true);
                StepsComboBox3.setEnabled(true);
                StepTypeComboBox3.setEnabled(true);
                FunctionComboBox3.setEnabled(true);
            }
            else{
                mmc_.setProperty(Master, "Command", "MOTOR2ENABLE=0;");
                MotorSlider3.setEnabled(false);
                StepsComboBox3.setEnabled(false);
                StepTypeComboBox3.setEnabled(false);
                FunctionComboBox3.setEnabled(false);
                mmc_.setProperty(Master, "Command", "FILTER"+FW+"ENABLE=0;");
            }
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Enable 3");
        }
    }//GEN-LAST:event_MotorCheckBox3ActionPerformed

    private void MotorCheckBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MotorCheckBox4ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox4.getSelectedItem();
        int StepType=(int)StepTypeComboBox4.getSelectedIndex();
        int FW = FunctionComboBox4.getSelectedIndex();
        try{
            if(MotorCheckBox4.isSelected()){
                mmc_.setProperty(Master, "Command", "MOTOR3ENABLE=1;");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "MOTOR3STEPS="+Steps+";");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "MOTOR3STEPTYPE="+StepType+";");
                MotorSlider4.setEnabled(true);
                StepsComboBox4.setEnabled(true);
                StepTypeComboBox4.setEnabled(true);
                FunctionComboBox4.setEnabled(true);
            }
            else{
                mmc_.setProperty(Master, "Command", "MOTOR3ENABLE=0;");
                MotorSlider4.setEnabled(false);
                StepsComboBox4.setEnabled(false);
                StepTypeComboBox4.setEnabled(false);
                FunctionComboBox4.setEnabled(false);
                mmc_.setProperty(Master, "Command", "FILTER"+FW+"ENABLE=0;");
            }
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Enable 4");
        }
    }//GEN-LAST:event_MotorCheckBox4ActionPerformed

    private void StepsComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepsComboBox1ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox1.getSelectedItem();
        try{
            mmc_.setProperty(Master, "Command", "MOTOR0STEPS="+Steps+";");
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
                StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }

      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Steps 1");
        }
    }//GEN-LAST:event_StepsComboBox1ActionPerformed

    private void StepsComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepsComboBox2ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox2.getSelectedItem();
        try{
            mmc_.setProperty(Master, "Command", "MOTOR1STEPS="+Steps+";");
            if("Motor_2".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox2.getSelectedItem())){
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
                else if("400".equals(StepsComboBox2.getSelectedItem())){
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
                StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Steps 2");
        }
    }//GEN-LAST:event_StepsComboBox2ActionPerformed

    private void StepsComboBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepsComboBox3ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox3.getSelectedItem();
        try{
            mmc_.setProperty(Master, "Command", "MOTOR2STEPS="+Steps+";");
            if("Motor_3".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox3.getSelectedItem())){
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
                else if("400".equals(StepsComboBox3.getSelectedItem())){
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
                StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Steps 3");
        }
    }//GEN-LAST:event_StepsComboBox3ActionPerformed

    private void StepsComboBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepsComboBox4ActionPerformed
        // TODO add your handling code here:
        String Steps=(String)StepsComboBox4.getSelectedItem();
        try{
            mmc_.setProperty(Master, "Command", "MOTOR3STEPS="+Steps+";");
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
                StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Steps 4");
        }
    }//GEN-LAST:event_StepsComboBox4ActionPerformed

    private void StepTypeComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepTypeComboBox1ActionPerformed
        // TODO add your handling code here:
        String StepType=(String)StepTypeComboBox1.getSelectedItem();
        try{
            if(StepType.equalsIgnoreCase("FULL")){
                mmc_.setProperty(Master, "Command", "MOTOR0STEPTYPE=0;");
            }
            if(StepType.equalsIgnoreCase("HALF")){
                mmc_.setProperty(Master, "Command", "MOTOR0STEPTYPE=1;");
            }
            if(StepType.equalsIgnoreCase("QUARTER")){
                mmc_.setProperty(Master, "Command", "MOTOR0STEPTYPE=2;");
            }
            if(StepType.equalsIgnoreCase("EIGHTH")){
                mmc_.setProperty(Master, "Command", "MOTOR0STEPTYPE=3;");
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
                StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
            catch (Exception ex) {
                  Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
                  SettingsTextArea.setText("Error in Motor Step Type 1");
        }
    }//GEN-LAST:event_StepTypeComboBox1ActionPerformed

    private void StepTypeComboBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepTypeComboBox3ActionPerformed
        // TODO add your handling code here:
        String StepType=(String)StepTypeComboBox3.getSelectedItem();
        try{
            if(StepType.equalsIgnoreCase("FULL")){
                mmc_.setProperty(Master, "Command", "MOTOR2STEPTYPE=0;");
            }
            if(StepType.equalsIgnoreCase("HALF")){
                mmc_.setProperty(Master, "Command", "MOTOR2STEPTYPE=1;");
            }
            if(StepType.equalsIgnoreCase("QUARTER")){
                mmc_.setProperty(Master, "Command", "MOTOR2STEPTYPE=2;");
            }
            if(StepType.equalsIgnoreCase("EIGHTH")){
                mmc_.setProperty(Master, "Command", "MOTOR2STEPTYPE=3;");
            }
            /*if(StepType.equalsIgnoreCase("SIXTEEN")){
                mmc_.setProperty("SRotation", "Command", "MOTOR1STEPTYPE=4;");
            }*/
            if("Motor_2".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox2.getSelectedItem())){
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
                else if("400".equals(StepsComboBox2.getSelectedItem())){
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
                StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Step Type 2");
        }
    }//GEN-LAST:event_StepTypeComboBox3ActionPerformed

    private void StepTypeComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepTypeComboBox2ActionPerformed
        // TODO add your handling code here:
                String StepType=(String)StepTypeComboBox2.getSelectedItem();
        try{
            if(StepType.equalsIgnoreCase("FULL")){
                mmc_.setProperty(Master, "Command", "MOTOR1STEPTYPE=0;");
            }
            if(StepType.equalsIgnoreCase("HALF")){
                mmc_.setProperty(Master, "Command", "MOTOR1STEPTYPE=1;");
            }
            if(StepType.equalsIgnoreCase("QUARTER")){
                mmc_.setProperty(Master, "Command", "MOTOR1STEPTYPE=2;");
            }
            if(StepType.equalsIgnoreCase("EIGHTH")){
                mmc_.setProperty(Master, "Command", "MOTOR1STEPTYPE=3;");
            }
            /*if(StepType.equalsIgnoreCase("SIXTEEN")){
                mmc_.setProperty("SRotation", "Command", "MOTOR2STEPTYPE=4;");
            }*/
            if("Motor_3".equals(RotationMotorComboBox.getSelectedItem())){
                if("200".equals(StepsComboBox3.getSelectedItem())){
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
                else if("400".equals(StepsComboBox3.getSelectedItem())){
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
                StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Step Type 3");
        }
    }//GEN-LAST:event_StepTypeComboBox2ActionPerformed

    private void StepTypeComboBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepTypeComboBox4ActionPerformed
        // TODO add your handling code here:
                String StepType=(String)StepTypeComboBox4.getSelectedItem();
        try{
            if(StepType.equalsIgnoreCase("FULL")){
                mmc_.setProperty(Master, "Command", "MOTOR3STEPTYPE=0;");
            }
            if(StepType.equalsIgnoreCase("HALF")){
                mmc_.setProperty(Master, "Command", "MOTOR3STEPTYPE=1;");
            }
            if(StepType.equalsIgnoreCase("QUARTER")){
                mmc_.setProperty(Master, "Command", "MOTOR3STEPTYPE=2;");
            }
            if(StepType.equalsIgnoreCase("EIGHTH")){
                mmc_.setProperty(Master, "Command", "MOTOR3STEPTYPE=3;");
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
                StepAngleComboBox.setModel(new javax.swing.DefaultComboBoxModel(Angles));
            }
        }
      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Step Type 4");
        }
    }//GEN-LAST:event_StepTypeComboBox4ActionPerformed

    private void MotorSlider1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_MotorSlider1StateChanged
        // TODO add your handling code here:
        int value = MotorSlider1.getValue();
        try{
            mmc_.setProperty(Master, "Command", "MOTOR0VELOCITY="+value+";");
            SettingsTextArea.setText("Motor Velocity "+value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Velocity 1");
        }
    }//GEN-LAST:event_MotorSlider1StateChanged

    private void MotorSlider2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_MotorSlider2StateChanged
        // TODO add your handling code here:
        int value = MotorSlider2.getValue();
        try{
            mmc_.setProperty(Master, "Command", "MOTOR1VELOCITY="+value+";");
            SettingsTextArea.setText("Motor Velocity "+value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Velocity 2");
        }
    }//GEN-LAST:event_MotorSlider2StateChanged

    private void MotorSlider3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_MotorSlider3StateChanged
        // TODO add your handling code here:
        int value = MotorSlider3.getValue();
        try{
            mmc_.setProperty(Master, "Command", "MOTOR2VELOCITY="+value+";");
            SettingsTextArea.setText("Motor Velocity "+value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Velocity 3");
        }
    }//GEN-LAST:event_MotorSlider3StateChanged

    private void MotorSlider4StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_MotorSlider4StateChanged
        // TODO add your handling code here:
        int value = MotorSlider4.getValue();
        try{
            mmc_.setProperty(Master, "Command", "MOTOR3VELOCITY="+value+";");
            SettingsTextArea.setText("Motor Velocity "+value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Motor Velocity 4");
        }
    }//GEN-LAST:event_MotorSlider4StateChanged

    private void readBuffer(){
        String str1 = "BLANK";
        String str2 = "";
        String str3 = "";
        try{
            String port = mmc_.getProperty(Master, "ShowPort");
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
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in readBuffer");  
        }

    }
    private void HelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_HelpButtonActionPerformed
        // TODO add your handling code here:
        SettingsTextArea.setText("Please wait 3 seconds");
        try{
            mmc_.setProperty(Master, "Command", "HELP;");
            
            mmc_.sleep(3000);
            readBuffer();
           //SettingsTextArea.setText(Integer.toString(Math.round(answer.capacity()))+"___"+Integer.toString(Math.round(answer2.capacity())));
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in HELP");
        }
    }//GEN-LAST:event_HelpButtonActionPerformed

    private void LaserButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LaserButtonActionPerformed
        // TODO add your handling code here:
        try{
            mmc_.setProperty(Master, "Command", "LASERINFO;");
            mmc_.sleep(2000);
            readBuffer();
//            String value = mmc_.getProperty("SRotation", "Response");
//            SettingsTextArea.setText(value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in LASER INFO");
        }
    }//GEN-LAST:event_LaserButtonActionPerformed

    private void MotorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MotorButtonActionPerformed
        // TODO add your handling code here:
        try{
            mmc_.setProperty(Master, "Command", "MOTORINFO;");
            mmc_.sleep(2000);
            readBuffer();
//            String value = mmc_.getProperty("SRotation", "Response");
//            SettingsTextArea.setText(value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in MOTOR INFO");
        }
    }//GEN-LAST:event_MotorButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        try{
            mmc_.setProperty(Master, "Command", "SEQUENCEINFO;");
            mmc_.sleep(2000);
            readBuffer();
//            String value = mmc_.getProperty("SRotation", "Response");
//            SettingsTextArea.setText(value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in SEQUENCE INFO");
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void ModeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ModeComboBoxActionPerformed
        // TODO add your handling code here:
        String Mode=(String) ModeComboBox.getSelectedItem();
        String expoSpim = "10";
        String expoConf = "100";
//        if(ACQR_.Exposure[0] != null){
//            expoSpim = Integer.toString(Integer.parseInt(ACQR_.Exposure[0])/10);
//            expoConf = ACQR_.Exposure[0];
//        }
        double ch[] = new double[6];
        
        try{

            String chs;

            if(Channel_CheckBox.isSelected()){
                
                for(int i = 0; i<6; i++){
                    if(Channeltable.getValueAt(i, 6).equals(true)){
                        chs = (String) Channeltable.getValueAt(i, 3).toString();
                        ch[i] = Double.parseDouble(chs);
                    } else{
                        ch[i] = 0;
                    };
                }    
            } 
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Setting Galvo Exposure \n at Selecting Mode");
        }
        
        expoSpim = Double.toString(ch[0]/10);
        expoConf = Double.toString(ch[0]);
        
        if("DSLM/SPIM".equals(Mode)){
            TimeLabel.setEnabled(true);
            NumberofSlicesField.setEnabled(true);
            StepLabel.setText("Steps (microns)");
            if (!StackCheckBox.isSelected()){
                StackCheckBox.doClick();
            }
            if (!GalvoCheckBox1.isSelected()){
                GalvoCheckBox1.doClick();
            }
            if (!GalvoCheckBox2.isSelected()){
                GalvoCheckBox2.doClick();
            }
            GAmpTextField1.setText("100");
            GFreqTextField1.setText(expoSpim);
            GAmpTextField2.setText("10");
            GAmpTextField3.setText("100");
            GFreqTextField3.setText(expoSpim);
            GAmpTextField4.setText("10");
        }
        /*else if ("FLOW-SPIM".equals(Mode)){
            TimeLabel.setEnabled(true);
            StepLabel.setText("Steps(#images)");
            StackCheckBox.setEnabled(true);
            GalvoCheckBox1.setEnabled(true);
            GalvoCheckBox2.setEnabled(true);
        }

        else if ("VERTICAL-SPIM".equals(Mode)){
            TimeLabel.setEnabled(true);
            StepLabel.setText("Steps (microns)");
            StackCheckBox.setEnabled(true);
            GalvoCheckBox1.setEnabled(true);
            GalvoCheckBox2.setEnabled(true);
        }
        else if("OPT".equals(Mode)){
            StackCheckBox.setSelected(false);
            GalvoCheckBox2.setSelected(false);
            GalvoCheckBox1.setSelected(false);
            StackCheckBox.setEnabled(false);
            GalvoCheckBox1.setEnabled(false);
            GalvoCheckBox2.setEnabled(false);

        }*/
        else if("MultiPos_Trig".equals(Mode)){
            TimeLabel.setEnabled(true);
            NumberofSlicesField.setEnabled(true);
            StepLabel.setText("Steps (microns)");
            if (!StackCheckBox.isSelected()){
                StackCheckBox.doClick();
            }
            if (!GalvoCheckBox1.isSelected()){
                GalvoCheckBox1.doClick();
            }
            if (!GalvoCheckBox2.isSelected()){
                GalvoCheckBox2.doClick();
            }
            GAmpTextField1.setText("100");
            GFreqTextField1.setText(expoSpim);
            GAmpTextField2.setText("10");
            GAmpTextField3.setText("100");
            GFreqTextField3.setText(expoSpim);
            GAmpTextField4.setText("10");
        }
        
        else if("DSLM_XYCZ".equals(Mode)){
            TimeLabel.setEnabled(true);
            NumberofSlicesField.setEnabled(true);
            StepLabel.setText("Steps (microns)");
            if (!StackCheckBox.isSelected()){
                StackCheckBox.doClick();
            }
            if (!GalvoCheckBox1.isSelected()){
                GalvoCheckBox1.doClick();
            }
            if (!GalvoCheckBox2.isSelected()){
                GalvoCheckBox2.doClick();
            }
            try {
                mmc_.setProperty(Master, "Command", "SEQUENCEMODE=2;");
                mmc_.sleep(100);
            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("Error in DSLM_XYCZ definition");
            }
        }
        else if("Confocal_Trig".equals(Mode)){
            TimeLabel.setEnabled(true);
            NumberofSlicesField.setEnabled(true);
            StepLabel.setText("Steps (microns)");
            if (!StackCheckBox.isSelected()){
                StackCheckBox.doClick();
            }
            if (!GalvoCheckBox1.isSelected()){
                GalvoCheckBox1.doClick();
            }
            if (!GalvoCheckBox2.isSelected()){
                GalvoCheckBox2.doClick();
            }
            GAmpTextField1.setText("100");
            GFreqTextField1.setText(expoConf);
            GAmpTextField2.setText("1");
            GAmpTextField3.setText("100");
            GFreqTextField3.setText(expoConf);
            GAmpTextField4.setText("1");
            try {
                mmc_.setProperty(Master, "Command", "SEQUENCEMODE=2;");
                mmc_.sleep(100);
            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("Error in Confocal_XYCZ definition");
            }
        }
        else if("SPIM_Trig".equals(Mode)){
            TimeLabel.setEnabled(true);
            NumberofSlicesField.setEnabled(true);
            StepLabel.setText("Steps (microns)");
            if (!StackCheckBox.isSelected()){
                StackCheckBox.doClick();
            }
            if (!GalvoCheckBox1.isSelected()){
                GalvoCheckBox1.doClick();
            }
            if (!GalvoCheckBox2.isSelected()){
                GalvoCheckBox2.doClick();
            }
            GAmpTextField1.setText("100");
            GFreqTextField1.setText(expoSpim);
            GAmpTextField2.setText("10");
            GAmpTextField3.setText("100");
            GFreqTextField3.setText(expoSpim);
            GAmpTextField4.setText("10");
            try {
                mmc_.setProperty(Master, "Command", "SEQUENCEMODE=2;");
                mmc_.sleep(100);
            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("Error in SPIM_Trig definition");
            }
        }
        
    }//GEN-LAST:event_ModeComboBoxActionPerformed

    private void UPUPButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UPUPButtonActionPerformed
        // TODO add your handling code here:
        try {
            String Step = UpUpField.getText();
            String focusdevice = (String) StageMotorComboBox.getSelectedItem();
            moveMotor(focusdevice, Step, true);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Fast Towards Detection ");
        }
    }//GEN-LAST:event_UPUPButtonActionPerformed

    private void DownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DownButtonActionPerformed
        // TODO add your handling code here:
        try{
            String Step=UpField.getText();
            String focusdevice=(String) StageMotorComboBox.getSelectedItem();
            moveMotor(focusdevice, Step, false);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Away From Detection ");
        }
    }//GEN-LAST:event_DownButtonActionPerformed

    private void DownDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DownDownButtonActionPerformed
        // TODO add your handling code here:
        try{
            String Step=UpUpField.getText();
            String focusdevice=(String) StageMotorComboBox.getSelectedItem();
            moveMotor(focusdevice, Step, false);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Fast Away From Detection ");
        }
    }//GEN-LAST:event_DownDownButtonActionPerformed

    private void PositionFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PositionFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_PositionFieldActionPerformed

    private void StageMotorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StageMotorComboBoxActionPerformed
        // TODO add your handling code here:
        String focusdevice=(String) StageMotorComboBox.getSelectedItem();

        try {
            String Position_new=formatter.format(mmc_.getPosition(focusdevice));
            PositionField.setText(Position_new);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Selecting Stage Motor");
        }
    }//GEN-LAST:event_StageMotorComboBoxActionPerformed

    private void StackCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StackCheckBoxActionPerformed
        // TODO add your handling code here:
        if (StackCheckBox.isSelected()){
            StartField.setEnabled(true);
            StackPanel.setEnabled(true);
            SetMiddleButton.setEnabled(true);
            MiddleField1.setEnabled(true);
            EndField.setEnabled(true);
            SetStartButton.setEnabled(true);
            StepField.setEnabled(true);
            StartLabel.setEnabled(true);
            MiddleLabel1.setEnabled(true);
            EndLabel.setEnabled(true);
            SetEndButton.setEnabled(true);
            StepLabel.setEnabled(true);
            ShutterStackCheckBox.setEnabled(true);
            TimeLabel.setEnabled(true);
            NumberofSlicesField.setEnabled(true);
            SetSliceButton.setEnabled(true);
            
            try {
                mmc_.setProperty(Master, "Command", "STAGE0ENABLE=1;");
            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("ERROR in Stage Enable");
            }

        }
        else{
            StartField.setEnabled(false);
            StackPanel.setEnabled(false);
            SetMiddleButton.setEnabled(false);
            MiddleField1.setEnabled(false);
            EndField.setEnabled(false);
            SetStartButton.setEnabled(false);
            StepField.setEnabled(false);
            StartLabel.setEnabled(false);
            MiddleLabel1.setEnabled(false);
            EndLabel.setEnabled(false);
            SetEndButton.setEnabled(false);
            StepLabel.setEnabled(false);
            ShutterStackCheckBox.setEnabled(false);
            TimeLabel.setEnabled(false);
            NumberofSlicesField.setEnabled(false);
            TotalTimeField.setEnabled(false);
            SetSliceButton.setEnabled(false);
            
            try {
                mmc_.setProperty(Master, "Command", "STAGE0ENABLE=0;");
            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("ERROR in Stage Disable");
            }
        }
    }//GEN-LAST:event_StackCheckBoxActionPerformed

    private void SetMiddleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SetMiddleButtonActionPerformed
        // TODO add your handling code here:
        String focusdevice=(String) StageMotorComboBox.getSelectedItem();
        try {
            double startpos = Double.parseDouble(StartField.getText());
            double endpos = Double.parseDouble(EndField.getText());
            double middlepos = (startpos+endpos)/2;
            String Position=formatter.format(middlepos);
            MiddleField1.setText(Position);
            PositionField.setText(Position);
            mmc_.setPosition(focusdevice,middlepos);

        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("ERROR in Middle Button");
        }
    }//GEN-LAST:event_SetMiddleButtonActionPerformed

    private void SetStartButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SetStartButtonActionPerformed
        // TODO add your handling code here:
        String focusdevice=(String) StageMotorComboBox.getSelectedItem();
        //String focusdevice=mmc_.getFocusDevice();

        try {
            double position = mmc_.getPosition(focusdevice);
            String Position=formatter.format(position);
            StartField.setText(Position);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Setting End Position");
        }
//            Number of Slices and acquisition estimation time fields ------------------------------------------
            setDataSize();
            //            MId position Text Field change -----------------------------
            setMiddlePos();
//            ---------------------------------------------------------------

/*----------Legacy - For vertival mode in SPIMFluid ----------*/
            String AMP1=GAmpTextField1.getText();
            String AMP2=GAmpTextField2.getText();
            Global.GampEND1=Double.parseDouble(AMP1);
            Global.GampEND2=Double.parseDouble(AMP2);

            String AMP3=GAmpTextField3.getText();
            String AMP4=GAmpTextField4.getText();
            Global.GampEND3=Double.parseDouble(AMP3);
            Global.GampEND4=Double.parseDouble(AMP4);
    }//GEN-LAST:event_SetStartButtonActionPerformed

    private void NumberofSlicesFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NumberofSlicesFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_NumberofSlicesFieldActionPerformed

    private void TotalTimeFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TotalTimeFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TotalTimeFieldActionPerformed

    private void GalvoCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GalvoCheckBox1ActionPerformed
        // TODO add your handling code here:
        if (GalvoCheckBox1.isSelected()){
            GAmpTextField1.setEnabled(true);
            GAmpTextField2.setEnabled(true);
            GAmpLabel1.setEnabled(true);
            GAmpLabel2.setEnabled(true);
            GFreqTextField1.setEnabled(true);
            G1RunTime.setEnabled(true);
            GFreqLabel1.setEnabled(true);
            G1Run.setEnabled(true);
            GMoveButton1.setEnabled(true);
            GSetButton1.setEnabled(true);
            try {
                mmc_.setProperty(Master, "Command", "GALVO0ENABLE=1;");
//                mmc_.setProperty("Arduino_SR-Switch","State","16");
//                mmc_.setProperty("Arduino_SR-Shutter","OnOff","1");
//                mmc_.setProperty("Arduino_SR-Shutter","OnOff","0");
            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("ERROR IN GALVO #1 START");
            }
        }
        else{
            GAmpTextField1.setEnabled(false);
            GAmpTextField2.setEnabled(false);
            GAmpLabel1.setEnabled(false);
            GAmpLabel2.setEnabled(false);
            GFreqTextField1.setEnabled(false);
            G1RunTime.setEnabled(false);
            GFreqLabel1.setEnabled(false);
            G1Run.setEnabled(false);
            GMoveButton1.setEnabled(false);
            GSetButton1.setEnabled(false);
            try {
                mmc_.setProperty(Master, "Command", "GALVO0ENABLE=0;");
//                mmc_.setProperty("Arduino_SR-Switch","State","16");
//                mmc_.setProperty("Arduino_SR-Shutter","OnOff","1");
//                mmc_.setProperty("Arduino_SR-Shutter","OnOff","0");
            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("ERROR IN GALVO #1 START");
            }
        }
    }//GEN-LAST:event_GalvoCheckBox1ActionPerformed

    private void GFreqTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GFreqTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_GFreqTextField1ActionPerformed

    private void GMoveButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GMoveButton1ActionPerformed
        // TODO add your handling code here:
        String AMP1     =GAmpTextField1.getText();
        String CYCLES   =GAmpTextField2.getText();
        double amp1     =Double.parseDouble(AMP1);
        double cycles   =Double.parseDouble(CYCLES);
        String FREQ1    =GFreqTextField1.getText();
        double freq1    =Double.parseDouble(FREQ1);
        try {
            mmc_.setProperty(Galvo1, "AnswerTimeout", "5000");
            mmc_.sleep(1000);
            
            mmc_.setProperty(Master, "Command", "GALVO0ENABLE=0;");
            
            mmc_.setProperty(Galvo1, "Command", "CYCLES="+cycles+";");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo1, "Command", "PERIOD="+freq1+";");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo1, "Command", "AMPLITUDE="+amp1+";");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo1, "Command", "START;");
            mmc_.sleep(100);
            
            mmc_.setProperty(Master, "Command", "GALVO0ENABLE=1;");
            
            MessageTextArea.setText("Galvo1 Defined");
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("ERROR IN MOVE GALVO #1");
        }
    }//GEN-LAST:event_GMoveButton1ActionPerformed

    private void GSetButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GSetButton1ActionPerformed
        String AMP1=GAmpTextField1.getText();
        String AMP2=GAmpTextField2.getText();
        double amp1=Double.parseDouble(AMP1);
        double amp2=Double.parseDouble(AMP2);
        double freq=0;
        try {
            mmc_.setProperty(Galvo1, "AnswerTimeout", "5000");
            mmc_.sleep(1000);
            
            mmc_.setProperty(Master, "Command", "GALVO0ENABLE=0;");
            
            mmc_.setProperty(Galvo1, "Command", "GOTO="+amp1+";");
            mmc_.sleep(100);
            
            mmc_.setProperty(Master, "Command", "GALVO0ENABLE=1;");
            
            MessageTextArea.setText("SET GALVO #1: " + amp1);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("ERROR IN SET GALVO #1");
        }
    }//GEN-LAST:event_GSetButton1ActionPerformed

    private void SetEndButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SetEndButtonActionPerformed
        // TODO add your handling code here:
        String focusdevice=(String) StageMotorComboBox.getSelectedItem();
        try {
            double position = mmc_.getPosition(focusdevice);
            String Position=formatter.format(position);
            EndField.setText(Position);   
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Setting End Position");
        }
//            Number of Slices and acquisition estimation time fields ------------------------------------------
            setDataSize();
            //            MId position Text Field change -----------------------------
            setMiddlePos();
//            ---------------------------------------------------------------

/*----------Legacy - For vertival mode in SPIMFluid ----------*/
            String AMP1=GAmpTextField1.getText();
            String AMP2=GAmpTextField2.getText();
            Global.GampEND1=Double.parseDouble(AMP1);
            Global.GampEND2=Double.parseDouble(AMP2);

            String AMP3=GAmpTextField3.getText();
            String AMP4=GAmpTextField4.getText();
            Global.GampEND3=Double.parseDouble(AMP3);
            Global.GampEND4=Double.parseDouble(AMP4);
    }//GEN-LAST:event_SetEndButtonActionPerformed

    private void MiddleField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MiddleField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_MiddleField1ActionPerformed

    private void UPButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UPButtonActionPerformed
        // TODO add your handling code here:
        try {
        String Step=UpField.getText();
        String focusdevice=(String) StageMotorComboBox.getSelectedItem();        
            moveMotor(focusdevice, Step, true);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Sample Towards Detection");
        }
    }//GEN-LAST:event_UPButtonActionPerformed

    private void RotationSampleCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RotationSampleCheckBoxActionPerformed
    if (RotationSampleCheckBox.isSelected()){
            RotationComboBox.setEnabled(true);
            RotationMotorComboBox.setEnabled(true);
            ShutterRotationCheckBox.setEnabled(true);
            RotationLabel.setEnabled(true);
        }
        else{
            RotationComboBox.setEnabled(false);
            RotationMotorComboBox.setEnabled(false);
            ShutterRotationCheckBox.setEnabled(false);
            RotationLabel.setEnabled(false);
        }
    }//GEN-LAST:event_RotationSampleCheckBoxActionPerformed

    private void StartFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StartFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_StartFieldActionPerformed

    private void StackCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_StackCheckBoxStateChanged
        // TODO add your handling code here:
        if (StackCheckBox.isSelected()){
            StartField.setEnabled(true);
            StackPanel.setEnabled(true);
            SetMiddleButton.setEnabled(true);
            MiddleField1.setEnabled(true);
            EndField.setEnabled(true);
            SetStartButton.setEnabled(true);
            StepField.setEnabled(true);
            StartLabel.setEnabled(true);
            MiddleLabel1.setEnabled(true);
            EndLabel.setEnabled(true);
            SetEndButton.setEnabled(true);
            StepLabel.setEnabled(true);
            ShutterStackCheckBox.setEnabled(true);
            TimeLabel.setEnabled(true);
            NumberofSlicesField.setEnabled(true);
            TotalTimeField.setEnabled(true);

        }
        else{
            StartField.setEnabled(false);
            StackPanel.setEnabled(false);
            SetMiddleButton.setEnabled(false);
            MiddleField1.setEnabled(false);
            EndField.setEnabled(false);
            SetStartButton.setEnabled(false);
            StepField.setEnabled(false);
            StartLabel.setEnabled(false);
            MiddleLabel1.setEnabled(false);
            EndLabel.setEnabled(false);
            SetEndButton.setEnabled(false);
            StepLabel.setEnabled(false);
            ShutterStackCheckBox.setEnabled(false);
            TimeLabel.setEnabled(false);
            NumberofSlicesField.setEnabled(false);
            TotalTimeField.setEnabled(false);
        }
    }//GEN-LAST:event_StackCheckBoxStateChanged

    private void StepFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepFieldActionPerformed
        // TODO add your handling code here:
        try{
            String focusdevice = (String)StageMotorComboBox.getSelectedItem();
            double stepsize = Double.parseDouble(StepField.getText().replaceAll(",", "."));
            mmc_.setProperty(focusdevice, "Trigger Step Size (um)", stepsize);
            setDataSize();
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Trigger Step Size");
        }
            
    }//GEN-LAST:event_StepFieldActionPerformed

    private void YPositionFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_YPositionFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_YPositionFieldActionPerformed

    private void YMotorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_YMotorComboBoxActionPerformed
        // TODO add your handling code here:
        String focusdevice=(String) YMotorComboBox.getSelectedItem();

        try {
            String Position_new=formatter.format(mmc_.getPosition(focusdevice));
            YPositionField.setText(Position_new);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Selecting Y Motor");
        }
    }//GEN-LAST:event_YMotorComboBoxActionPerformed

    private void XPositionFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_XPositionFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_XPositionFieldActionPerformed

    private void XMotorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_XMotorComboBoxActionPerformed
        // TODO add your handling code here:
        String focusdevice=(String) XMotorComboBox.getSelectedItem();
        try {
            String Position_new=formatter.format(mmc_.getPosition(focusdevice));
            XPositionField.setText(Position_new);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Selecting X Motor");
        }
    }//GEN-LAST:event_XMotorComboBoxActionPerformed

    private void XRightRightButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_XRightRightButtonActionPerformed
        // TODO add your handling code here:
        try{
            String Step=XYUpUpField.getText();
            String focusdevice=(String) XMotorComboBox.getSelectedItem();
            moveMotor(focusdevice, Step, true);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Right Fast");
        }
    }//GEN-LAST:event_XRightRightButtonActionPerformed

    private void XRightButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_XRightButtonActionPerformed
        // TODO add your handling code here:
        try{
            String Step=XYUpField.getText();
            String focusdevice=(String) XMotorComboBox.getSelectedItem();
            moveMotor(focusdevice, Step, true);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Right");
        }
    }//GEN-LAST:event_XRightButtonActionPerformed

    private void XLeftButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_XLeftButtonActionPerformed
        // TODO add your handling code here:
        try{
            String Step=XYUpField.getText();
            String focusdevice=(String) XMotorComboBox.getSelectedItem();
            moveMotor(focusdevice, Step, false);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Left Fast");
        }
    }//GEN-LAST:event_XLeftButtonActionPerformed

    private void XLeftLeftButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_XLeftLeftButtonActionPerformed
        // TODO add your handling code here:
        try{
            String Step=XYUpUpField.getText();
            String focusdevice=(String) XMotorComboBox.getSelectedItem();
            moveMotor(focusdevice, Step, false);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Left");
        }
    }//GEN-LAST:event_XLeftLeftButtonActionPerformed

    private void YDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_YDownButtonActionPerformed
        try{
            String Step=XYUpField.getText();
            String focusdevice=(String) YMotorComboBox.getSelectedItem();
            moveMotor(focusdevice, Step, true);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Up Fast");
        }
    }//GEN-LAST:event_YDownButtonActionPerformed

    private void YDownDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_YDownDownButtonActionPerformed
        // TODO add your handling code here:
        try{
            String Step=XYUpUpField.getText();
            String focusdevice=(String) YMotorComboBox.getSelectedItem();
            moveMotor(focusdevice, Step, true);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Up");
        }
    }//GEN-LAST:event_YDownDownButtonActionPerformed

    private void YUPButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_YUPButtonActionPerformed
        // TODO add your handling code here:
        try{
            String Step=XYUpField.getText();
            String focusdevice=(String) YMotorComboBox.getSelectedItem();
            moveMotor(focusdevice, Step, false);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Down Fast");
        }
    }//GEN-LAST:event_YUPButtonActionPerformed

    private void YUPUPButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_YUPUPButtonActionPerformed
        try{
            String Step=XYUpUpField.getText();
            String focusdevice=(String) YMotorComboBox.getSelectedItem();
            moveMotor(focusdevice, Step, false);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving Down");
        }
    }//GEN-LAST:event_YUPUPButtonActionPerformed

    private void XYListButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_XYListButtonActionPerformed
        // TODO add your handling code here:
        gui_.showXYPositionList();
    }//GEN-LAST:event_XYListButtonActionPerformed

    private void XYUpUpFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_XYUpUpFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_XYUpUpFieldActionPerformed

    private void G2RunTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_G2RunTimeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_G2RunTimeActionPerformed

    private void GSetButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GSetButton2ActionPerformed
        String AMP3=GAmpTextField3.getText();
        String AMP4=GAmpTextField4.getText();
        double amp3=Double.parseDouble(AMP3);
        double amp4=Double.parseDouble(AMP4);
        double freq=0;
        try {
            mmc_.setProperty(Galvo2, "AnswerTimeout", "5000");
            mmc_.sleep(1000);
            
            mmc_.setProperty(Master, "Command", "GALVO1ENABLE=0;");
            
            mmc_.setProperty(Galvo2, "Command", "GOTO="+amp3+";");
            mmc_.sleep(100);
            
            mmc_.setProperty(Master, "Command", "GALVO1ENABLE=1;");
            
            MessageTextArea.setText("SET GALVO #2: " + amp3);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("ERROR IN SET GALVO #2");
        }
    }//GEN-LAST:event_GSetButton2ActionPerformed

    private void GMoveButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GMoveButton2ActionPerformed
        String AMP3     =GAmpTextField3.getText();
        String CYCLES   =GAmpTextField4.getText();
        double amp3     =Double.parseDouble(AMP3);
        double cycles   =Double.parseDouble(CYCLES);
        String FREQ3    =GFreqTextField3.getText();
        String FREQ4    =G2RunTime.getText();
        double freq3    =Double.parseDouble(FREQ3);
        double freq4    =Double.parseDouble(FREQ4);
        try {
            mmc_.setProperty(Galvo2, "AnswerTimeout", "5000");
            mmc_.sleep(1000);
            
            mmc_.setProperty(Master, "Command", "GALVO1ENABLE=0;");
            
            mmc_.setProperty(Galvo2, "Command", "CYCLES="+cycles+";");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "Command", "PERIOD="+freq3+";");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "Command", "AMPLITUDE="+amp3+";");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "Command", "START;");
            mmc_.sleep(100);
            
            mmc_.setProperty(Master, "Command", "GALVO1ENABLE=1;");
            
            MessageTextArea.setText("Galvo2 Defined");
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("ERROR IN MOVE GALVO #2");
        }
    }//GEN-LAST:event_GMoveButton2ActionPerformed

    private void GFreqTextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GFreqTextField3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_GFreqTextField3ActionPerformed

    private void GalvoCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GalvoCheckBox2ActionPerformed
        // TODO add your handling code here:
        if (GalvoCheckBox2.isSelected()){
            GAmpTextField3.setEnabled(true);
            GAmpTextField4.setEnabled(true);
            GAmpLabel3.setEnabled(true);
            GAmpLabel4.setEnabled(true);
            GFreqTextField3.setEnabled(true);
            G2RunTime.setEnabled(true);
            GFreqLabel3.setEnabled(true);
            G2Run.setEnabled(true);
            GMoveButton2.setEnabled(true);
            GSetButton2.setEnabled(true);
            try {
                mmc_.setProperty(Master, "Command", "GALVO1ENABLE=1;");

            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("ERROR IN GALVO #2 START");
            }
        }
        else{
            GAmpTextField3.setEnabled(false);
            GAmpTextField4.setEnabled(false);
            GAmpLabel3.setEnabled(false);
            GAmpLabel4.setEnabled(false);
            GFreqTextField3.setEnabled(false);
            G2RunTime.setEnabled(false);
            GFreqLabel3.setEnabled(false);
            G2Run.setEnabled(false);
            GMoveButton2.setEnabled(false);
            GSetButton2.setEnabled(false);
            try {
                mmc_.setProperty(Master, "Command", "GALVO1ENABLE=0;");

            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                MessageTextArea.setText("ERROR IN GALVO #2 START");
            }
        }
    }//GEN-LAST:event_GalvoCheckBox2ActionPerformed

    private void FunctionComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FunctionComboBox1ActionPerformed
        // TODO add your handling code here:
        int stepType = (int)(StepTypeComboBox1.getSelectedIndex());
        float pow_step = (float)Math.pow(2,stepType);
        float STEPS = Float.parseFloat((String)StepsComboBox1.getSelectedItem());
        int f_offset = 67;
        f_offset = Math.round(pow_step*STEPS/6);
        
        try{
        if      (Master.equals(FunctionComboBox1.getSelectedItem())){
            Global.rotationM = 0;
        }
        else if ("ExcitationW".equals(FunctionComboBox1.getSelectedItem())){
            Global.excitM = 0;
            Excitation_ComboBox.setSelectedIndex(0);
            mmc_.setProperty(Master, "Command", "FILTER2MTR=0;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER2ENABLE=1;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER2RESET;");            
           //mmc_.setProperty(Master, "Command", "FILTER1OFFSET="+f_offset+";");
        }
        else if ("EmissionW".equals(FunctionComboBox1.getSelectedItem())){
            Global.emissM = 0;
            Emission_ComboBox.setSelectedIndex(0);
            mmc_.setProperty(Master, "Command", "FILTER0MTR=0;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER0ENABLE=1;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER0RESET;");            
           //mmc_.setProperty(Master, "Command", "FILTER2OFFSET="+f_offset+";");
        }
        else if ("IntensityW".equals(FunctionComboBox1.getSelectedItem())){
            Global.intenM = 0;
            Intensity_ComboBox.setSelectedIndex(0);
            mmc_.setProperty(Master, "Command", "FILTER1MTR=0;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER1ENABLE=1;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER1RESET;");            
            //mmc_.setProperty(Master, "Command", "FILTER3OFFSET="+f_offset+";");
        }
        
        }      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error defining Motor Function");
        }

        String Rotationdevice=(String) FunctionComboBox1.getSelectedItem();
        SettingsTextArea.setText("Motor 1 is define as "+Rotationdevice); 
        mmc_.sleep(500);
    }//GEN-LAST:event_FunctionComboBox1ActionPerformed

    private void FunctionComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FunctionComboBox2ActionPerformed
        // TODO add your handling code here:
        int stepType = (int)(StepTypeComboBox2.getSelectedIndex());
        float pow_step = (float)Math.pow(2,stepType);
        float STEPS = Float.parseFloat((String)StepsComboBox2.getSelectedItem());
        int f_offset = 67;
        f_offset = Math.round(pow_step*STEPS/6);
        try{
            if      (Master.equals(FunctionComboBox2.getSelectedItem())){
                Global.rotationM = 1;
            }
            else if ("ExcitationW".equals(FunctionComboBox2.getSelectedItem())){
                Global.excitM = 1;
                Excitation_ComboBox.setSelectedIndex(0);
                mmc_.setProperty(Master, "Command", "FILTER2MTR=1;");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "FILTER2ENABLE=1;");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "FILTER2RESET;");            
                //mc_.setProperty(Master, "Command", "FILTER1OFFSET="+f_offset+";");
            }
            else if ("EmissionW".equals(FunctionComboBox2.getSelectedItem())){
                Global.emissM = 1;
                Emission_ComboBox.setSelectedIndex(0);
                mmc_.setProperty(Master, "Command", "FILTER0MTR=1;");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "FILTER0ENABLE=1;");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "FILTER0RESET;");            
               //mmc_.setProperty(Master, "Command", "FILTER2OFFSET="+f_offset+";");
            }
            else if ("IntensityW".equals(FunctionComboBox2.getSelectedItem())){
                Global.intenM = 1;
                Intensity_ComboBox.setSelectedIndex(0);
                mmc_.setProperty(Master, "Command", "FILTER1MTR=1;");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "FILTER1ENABLE=1;");
                mmc_.sleep(50);
                mmc_.setProperty(Master, "Command", "FILTER1RESET;");            
               //mmc_.setProperty(Master, "Command", "FILTER3OFFSET="+f_offset+";");
            }
        
        }      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error defining Motor Function");
        }

        String Rotationdevice=(String) FunctionComboBox2.getSelectedItem();
        SettingsTextArea.setText("Motor 2 is define as "+Rotationdevice); 
        mmc_.sleep(500);
    }//GEN-LAST:event_FunctionComboBox2ActionPerformed

    private void FunctionComboBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FunctionComboBox3ActionPerformed
        // TODO add your handling code here:
        int stepType = (int)(StepTypeComboBox3.getSelectedIndex());
        float pow_step = (float)Math.pow(2,stepType);
        float STEPS = Float.parseFloat((String)StepsComboBox3.getSelectedItem());
        int f_offset = 67;
        f_offset = Math.round(pow_step*STEPS/6);
        try{
        if      (Master.equals(FunctionComboBox3.getSelectedItem())){
            Global.rotationM = 2;
        }
        else if ("ExcitationW".equals(FunctionComboBox3.getSelectedItem())){
            Global.excitM = 2;
            Excitation_ComboBox.setSelectedIndex(0);
            mmc_.setProperty(Master, "Command", "FILTER2MTR=2;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER2ENABLE=1;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER2RESET;");            
           //mmc_.setProperty(Master, "Command", "FILTER1OFFSET="+f_offset+";");
        }
        else if ("EmissionW".equals(FunctionComboBox3.getSelectedItem())){
            Global.emissM = 2;
            Emission_ComboBox.setSelectedIndex(0);
            mmc_.setProperty(Master, "Command", "FILTER0MTR=2;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER0ENABLE=1;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER0RESET;");            
           //mmc_.setProperty(Master, "Command", "FILTER2OFFSET="+f_offset+";");
        }
        else if ("IntensityW".equals(FunctionComboBox3.getSelectedItem())){
            Global.intenM = 2;
            Intensity_ComboBox.setSelectedIndex(0);
            mmc_.setProperty(Master, "Command", "FILTER1MTR=2;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER1ENABLE=1;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER1RESET;");            
           //mmc_.setProperty(Master, "Command", "FILTER3OFFSET="+f_offset+";");
        }
        
        }      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error defining Motor Function");
        }

        String Rotationdevice=(String) FunctionComboBox3.getSelectedItem();
        SettingsTextArea.setText("Motor 3 is define as "+Rotationdevice);  
        mmc_.sleep(500);
    }//GEN-LAST:event_FunctionComboBox3ActionPerformed

    private void FunctionComboBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FunctionComboBox4ActionPerformed
        // TODO add your handling code here:
        int stepType = (int)(StepTypeComboBox4.getSelectedIndex());
        float pow_step = (float)Math.pow(2,stepType);
        float STEPS = Float.parseFloat((String)StepsComboBox4.getSelectedItem());
        int f_offset = 67;
        f_offset = Math.round(pow_step*STEPS/6);
        try{
        if      (Master.equals(FunctionComboBox4.getSelectedItem())){
            Global.rotationM = 3;
        }
        else if ("ExcitationW".equals(FunctionComboBox4.getSelectedItem())){
            Global.excitM = 3;
            Excitation_ComboBox.setSelectedIndex(0);
            mmc_.setProperty(Master, "Command", "FILTER2MTR=3;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER2ENABLE=1;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER2RESET;");
           // mmc_.setProperty(Master, "Command", "FILTER3OFFSET="+f_offset+";");
        }
        else if ("EmissionW".equals(FunctionComboBox4.getSelectedItem())){
            Global.emissM = 3;
            Emission_ComboBox.setSelectedIndex(0);
            mmc_.setProperty(Master, "Command", "FILTER0MTR=3;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER0ENABLE=1;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER0RESET;");
            //mmc_.setProperty(Master, "Command", "FILTER3OFFSET="+f_offset+";");
        }
        else if ("IntensityW".equals(FunctionComboBox4.getSelectedItem())){
            Global.intenM = 3;
            Intensity_ComboBox.setSelectedIndex(0);
            mmc_.setProperty(Master, "Command", "FILTER1MTR=3;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER1ENABLE=1;");
            mmc_.sleep(50);
            mmc_.setProperty(Master, "Command", "FILTER1RESET;");
            //mmc_.setProperty(Master, "Command", "FILTER3OFFSET="+f_offset+";");
        }
        
        }      catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error defining Motor Function");
        }

        String Rotationdevice=(String) FunctionComboBox4.getSelectedItem();
        SettingsTextArea.setText("Motor 4 is define as "+Rotationdevice); 
        mmc_.sleep(500);
    }//GEN-LAST:event_FunctionComboBox4ActionPerformed

    private void GalvoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GalvoButtonActionPerformed
        // TODO add your handling code here:
        try{
            mmc_.setProperty(Master, "Command", "GALVOINFO;");
            mmc_.sleep(2000);
            readBuffer();
//            String value = mmc_.getProperty(Master, "Response");
//            SettingsTextArea.setText(value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in GALVO INFO");
        }
    }//GEN-LAST:event_GalvoButtonActionPerformed

    private void FilterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FilterButtonActionPerformed
        // TODO add your handling code here:
        try{
            mmc_.setProperty(Master, "Command", "FILTERINFO;");
            mmc_.sleep(2000);
            readBuffer();
//            String value = mmc_.getProperty(Master, "Response");
//            SettingsTextArea.setText(value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in FILTER INFO");
        }
    }//GEN-LAST:event_FilterButtonActionPerformed

    private void StageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StageButtonActionPerformed
        // TODO add your handling code here:
        try{
            mmc_.setProperty(Master, "Command", "STAGEINFO;");
            mmc_.sleep(2000);
            readBuffer();
//            String value = mmc_.getProperty(Master, "Response");
//            SettingsTextArea.setText(value);
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in STAGE INFO");
        }
    }//GEN-LAST:event_StageButtonActionPerformed

    private void SetSliceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SetSliceButtonActionPerformed
        // TODO add your handling code here:
        try{
            String focusdevice = (String)StageMotorComboBox.getSelectedItem();
            double stepsize = Double.parseDouble(StepField.getText().replaceAll(",", "."));
            mmc_.setProperty(focusdevice, "Trigger Step Size (um)", stepsize);
            setDataSize();
        }catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Trigger Step Size");
        }
    }//GEN-LAST:event_SetSliceButtonActionPerformed

    private void G1RunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_G1RunActionPerformed
        String CYCLES   =G1RunTime.getText();
        double cycles   =Double.parseDouble(CYCLES)*100;
        try {
            mmc_.setProperty(Master, "Command", "GALVO0ENABLE=0;");
            
            mmc_.setProperty(Galvo1, "Command", "CYCLES="+cycles+";");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo1, "Command", "PERIOD=10;");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo1, "Command", "AMPLITUDE=100;");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo1, "Command", "START;");
            mmc_.sleep(100);
            
            mmc_.setProperty(Master, "Command", "GALVO0ENABLE=1;");
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("ERROR IN MOVE GALVO #1");
        }
    }//GEN-LAST:event_G1RunActionPerformed

    private void G2RunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_G2RunActionPerformed

        String CYCLES   =G2RunTime.getText();
        double cycles   =Double.parseDouble(CYCLES) * 100;

        try {
            mmc_.setProperty(Master, "Command", "GALVO1ENABLE=0;");
            
            mmc_.setProperty(Galvo2, "Command", "CYCLES="+cycles+";");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "Command", "PERIOD=10;");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "Command", "AMPLITUDE=100;");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "Command", "START;");
            mmc_.sleep(100);
            
            mmc_.setProperty(Master, "Command", "GALVO1ENABLE=1;");
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("ERROR IN MOVE GALVO #2");
        }
    }//GEN-LAST:event_G2RunActionPerformed

    private void StartPjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StartPjButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_StartPjButtonActionPerformed

    private void StartPjButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_StartPjButtonMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_StartPjButtonMouseClicked

    private void PumpsComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_PumpsComboBoxItemStateChanged
        // TODO add your handling code here:
        String Pump;

        try {
            Pump=(String) PumpsComboBox.getSelectedItem();
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_PumpsComboBoxItemStateChanged

    private void SecondCamComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SecondCamComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SecondCamComboBoxActionPerformed

    private void FirstCamComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FirstCamComboBoxActionPerformed
        // TODO add your handling code here:
        String Cam_name=(String) FirstCamComboBox.getSelectedItem();
        try {
            mmc_.setCameraDevice(Cam_name);
            Cam_name=mmc_.getCameraDevice();
            MessageTextArea.setText(Cam_name);
            if ("DualCAM".equals(Cam_name)){

                mmc_.setProperty("CAM1", "Trigger", "NORMAL");
                mmc_.setProperty("CAM2", "Trigger", "START");
                mmc_.setProperty("CAM1", "TRIGGER ACTIVE", "EDGE");
                mmc_.setProperty("CAM2", "TRIGGER ACTIVE", "EDGE");
                mmc_.setProperty("DualCAM", "Physical Camera 1", "CAM2");
                mmc_.setProperty("DualCAM", "Physical Camera 2", "CAM1");
                String prop=mmc_.getProperty("CAM2", "TRIGGER SOURCE");
                MessageTextArea.setText(prop);
                gui_.refreshGUI();
            }
            else if ("CAM1".equals(Cam_name)){

                gui_.refreshGUI();
            }
            else if ("CAM2".equals(Cam_name)){
                gui_.refreshGUI();
            }

        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error in CAM");
        }
    }//GEN-LAST:event_FirstCamComboBoxActionPerformed

    private void RestoreButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RestoreButtonActionPerformed
        try {
            // TODO add your handling code here:         
            mmc_.setProperty(Master, "AnswerTimeout", "5000");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo1, "AnswerTimeout", "5000");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "AnswerTimeout", "5000");
            mmc_.sleep(100);
            mmc_.setProperty(Master, "CommandTerminator", "\r");
            mmc_.sleep(100);
            mmc_.setProperty(Master, "ResponseTerminator", "\r");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo1, "CommandTerminator", "\r");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo1, "ResponseTerminator", "\r");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "CommandTerminator", "\r");
            mmc_.sleep(100);
            mmc_.setProperty(Galvo2, "ResponseTerminator", "\r");
            
            //---------------------------------Stack Panel-----------------------------------//
            StackCheckBox.setSelected(false);
            StartField.setEnabled(false);
            StackPanel.setEnabled(false);
            SetMiddleButton.setEnabled(false);
            MiddleField1.setEnabled(false);
            EndField.setEnabled(false);
            SetStartButton.setEnabled(false);
            StepField.setEnabled(false);
            StartLabel.setEnabled(false);
            MiddleLabel1.setEnabled(false);
            EndLabel.setEnabled(false);
            SetEndButton.setEnabled(false);
            StepLabel.setEnabled(false);
            ShutterStackCheckBox.setEnabled(false);
            TimeLabel.setEnabled(false);
            NumberofSlicesField.setEnabled(false);
            TotalTimeField.setEnabled(false);
            SetSliceButton.setEnabled(false);
            
            //---------------------------------Rotation Panel-----------------------------------//            
            RotationSampleCheckBox.setSelected(false);
            RotationComboBox.setEnabled(false);
            RotationMotorComboBox.setEnabled(false);
            ShutterRotationCheckBox.setEnabled(false);
            RotationLabel.setEnabled(false);
            
            //---------------------------------Galvo1 Panel-----------------------------------//   
            GalvoCheckBox1.setSelected(false);
            GAmpTextField1.setEnabled(false);
            GAmpTextField2.setEnabled(false);
            GAmpLabel1.setEnabled(false);
            GAmpLabel2.setEnabled(false);
            GFreqTextField1.setEnabled(false);
            G1RunTime.setEnabled(false);
            GFreqLabel1.setEnabled(false);
            G1Run.setEnabled(false);
            GMoveButton1.setEnabled(false);
            GSetButton1.setEnabled(false);
            
            //---------------------------------Galvo2 Panel-----------------------------------// 
            GalvoCheckBox2.setSelected(false);
            GAmpTextField3.setEnabled(false);
            GAmpTextField4.setEnabled(false);
            GAmpLabel3.setEnabled(false);
            GAmpLabel4.setEnabled(false);
            GFreqTextField3.setEnabled(false);
            G2RunTime.setEnabled(false);
            GFreqLabel3.setEnabled(false);
            G2Run.setEnabled(false);
            GMoveButton2.setEnabled(false);
            GSetButton2.setEnabled(false);
            
            //---------------------------------Settings Tab-----------------------------------//             
            LaserCheckBox1.setSelected(false);
            LaserCheckBox2.setSelected(false);
            LaserCheckBox3.setSelected(false);
            LaserCheckBox4.setSelected(false);
            LaserSlider1.setEnabled(false);
            LaserSlider2.setEnabled(false);
            LaserSlider3.setEnabled(false);
            LaserSlider4.setEnabled(false);
            LaserSlider1.setValue(0);
            LaserSlider2.setValue(0);
            LaserSlider3.setValue(0);
            LaserSlider4.setValue(0);
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
            FunctionComboBox1.setEnabled(false);
            FunctionComboBox2.setEnabled(false);
            FunctionComboBox3.setEnabled(false);
            FunctionComboBox4.setEnabled(false);
            
            MessageTextArea.setText("");
            SettingsTextArea.setText("");
            
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Reset");
        }
    }//GEN-LAST:event_RestoreButtonActionPerformed

    private void GAmpTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GAmpTextField1ActionPerformed
        // TODO add your handling code here:
        GSetButton1.doClick();
    }//GEN-LAST:event_GAmpTextField1ActionPerformed

    private void GAmpTextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GAmpTextField3ActionPerformed
        // TODO add your handling code here:
        GSetButton2.doClick();
    }//GEN-LAST:event_GAmpTextField3ActionPerformed

    private void DataSetSizeFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DataSetSizeFieldActionPerformed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_DataSetSizeFieldActionPerformed

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
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Enable 1");
        }
    }//GEN-LAST:event_LedCheckBox1ActionPerformed

    private void LedCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LedCheckBox2ActionPerformed
        // TODO add your handling code here:
        try{
            if(LedCheckBox1.isSelected()){
                mmc_.setProperty("SRotation", "Command", "LED1ENABLE=1;");
                LedSlider1.setEnabled(true);
            }
            else{
                mmc_.setProperty("SRotation", "Command", "LED1ENABLE=0;");
                LedSlider1.setEnabled(false);
            }
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Enable 2");
        }
    }//GEN-LAST:event_LedCheckBox2ActionPerformed

    private void LedCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LedCheckBox3ActionPerformed
        // TODO add your handling code here:
        try{
            if(LedCheckBox1.isSelected()){
                mmc_.setProperty("SRotation", "Command", "LED2ENABLE=1;");
                LedSlider1.setEnabled(true);
            }
            else{
                mmc_.setProperty("SRotation", "Command", "LED2ENABLE=0;");
                LedSlider1.setEnabled(false);
            }
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Enable 3");
        }
    }//GEN-LAST:event_LedCheckBox3ActionPerformed

    private void LedSlider1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LedSlider1StateChanged
        // TODO add your handling code here:
        int value = LedSlider1.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "LED0INTENSITY="+value+";");
            mmc_.sleep(100);
            mmc_.setProperty("SRotation", "Command", "LED0STATE=1;");
            SettingsTextArea.setText("Led 1 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Intensity 1");
        }
    }//GEN-LAST:event_LedSlider1StateChanged

    private void LedCheckBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LedCheckBox4ActionPerformed
        // TODO add your handling code here:
        try{
            if(LedCheckBox1.isSelected()){
                mmc_.setProperty("SRotation", "Command", "LED3ENABLE=1;");
                LedSlider1.setEnabled(true);
            }
            else{
                mmc_.setProperty("SRotation", "Command", "LED3ENABLE=0;");
                LedSlider1.setEnabled(false);
            }
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Enable 4");
        }
    }//GEN-LAST:event_LedCheckBox4ActionPerformed

    private void LedSlider2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LedSlider2StateChanged
        // TODO add your handling code here:
        int value = LedSlider1.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "LED1INTENSITY="+value+";");
            mmc_.sleep(100);
            mmc_.setProperty("SRotation", "Command", "LED1STATE=1;");
            SettingsTextArea.setText("Led 2 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Intensity 2");
        }
    }//GEN-LAST:event_LedSlider2StateChanged

    private void LedSlider3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LedSlider3StateChanged
        // TODO add your handling code here:
        int value = LedSlider1.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "LED2INTENSITY="+value+";");
            mmc_.sleep(100);
            mmc_.setProperty("SRotation", "Command", "LED2STATE=1;");
            SettingsTextArea.setText("Led 3 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Intensity 3");
        }
    }//GEN-LAST:event_LedSlider3StateChanged

    private void LedSlider4StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_LedSlider4StateChanged
        // TODO add your handling code here:
        int value = LedSlider1.getValue();
        try{
            mmc_.setProperty("SRotation", "Command", "LED3INTENSITY="+value+";");
            mmc_.sleep(100);
            mmc_.setProperty("SRotation", "Command", "LED3STATE=1;");
            SettingsTextArea.setText("Led 4 Intensity "+value);
        }catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Led Intensity 4");
        }
    }//GEN-LAST:event_LedSlider4StateChanged

    public void setPlugin(SPIMIIcontrolsPlugin plugin) {
      plugin_ = plugin;
    }

    public void setDataSize(){
        //            Calculate estimated time and DataSet Size ------------------------------------------
        double cht = 100;
        try{
            cht = mmc_.getExposure();  //exposure value
            int ts = 1;
            double time = 1; //Estimation time of the acquisition
            double datasize = 8; //total size of the dataset
            String sizetype = "MB"; 
            int chtotal = 0; //number channels
            double ds = 1;
            String chs;
            
            if(Channel_CheckBox.isSelected()){ //It will sum the exposure time total for each slice (ie 100ms for Ch1 + 200ms for Ch2 = 300ms) and the total number of channels (chtotal)
                double ch[] = new double[6];
                for(int i = 0; i<6; i++){
                    if(Channeltable.getValueAt(i, 6).equals(true)){
                        chtotal++;
                        chs = (String) Channeltable.getValueAt(i, 3).toString();
                        ch[i] = Double.parseDouble(chs);
                        cht += ch[i]; // Sum of all exposure times from different channels
                    } else{
                        ch[i] = 0;
                    };
                }    
            } else{
                chtotal = 1;
            } 

            double il = 1;

            if (GalvoCheckBox1.isSelected() && GalvoCheckBox2.isSelected()){
                il = 2;
            }
            
            int timepoints = 1;
            
            if (TimeLapseCheckBox.isSelected()){
                timepoints = (Integer) TimeFrames.getValue();
            }
            
            int rot = 1;
            
            if (RotationSampleCheckBox.isSelected()){
                rot =(int) ((int)360/Float.parseFloat((String)RotationComboBox.getSelectedItem()));
            } 
            
            PositionList pl = new PositionList();
            pl = gui_.getPositionList();
            
            if (pl.getNumberOfPositions() > rot){
                rot = pl.getNumberOfPositions();
            }
                
            if ((Double.parseDouble(EndField.getText()) - Double.parseDouble(StartField.getText())) > 0){
                ds = 1 + (Double.parseDouble(EndField.getText()) - Double.parseDouble(StartField.getText()))/Double.parseDouble(StepField.getText());
                ts =(int) Math.round(ds);
                time = (ds*cht*0.001*1.1*il);
                datasize = (ds*chtotal*8*il*timepoints*rot);
            }
            
            if (ts < 1){
                ts = 1;
            }
            
            if (datasize >= 1024){
                datasize = datasize/1024;
                sizetype = "GB";
            }
            
            String tslices = Integer.toString(ts);
            NumberofSlicesField.setText(tslices);
            TotalTimeField.setText(timeformat.format(time));            
            DataSetSizeField.setText("DataSet Size = " + formatter.format(datasize) + sizetype);
            }catch (Exception ex) {
                Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            SettingsTextArea.setText("Error in Set Step");
        }
    }
    
    public void setMiddlePos(){
            double startpos = Double.parseDouble(StartField.getText()); //get start position from start field
            double endpos = Double.parseDouble(EndField.getText()); //get end position from end field
            double middlepos = (startpos+endpos)/2;
            String Midposition=formatter.format(middlepos);
            MiddleField1.setText(Midposition);
    }
    
    public String moveMotor(String motor, String step, boolean dir){ //motor = selected motor, step = distance moved, dir = direcção true = positivo / false = negativo
        String Position_new = null;
        try {
            double STEP=Double.parseDouble(step);
            if(!dir){
                STEP = -STEP;
            }
            double position = mmc_.getPosition(motor);
            double position_new=position+STEP;
            mmc_.setPosition(motor,position_new);
            mmc_.sleep(500);
            Position_new=formatter.format(mmc_.getPosition(motor));
            PositionField.setText(Position_new);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            MessageTextArea.setText("Error Moving " + motor);
        }
        return Position_new;
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
    public javax.swing.JLabel CameraControlLabel;
    public javax.swing.JPanel CameraPanel;
    public javax.swing.JPanel ChannelControlPanel;
    public javax.swing.JScrollPane ChannelScrollPane;
    public javax.swing.JCheckBox Channel_CheckBox;
    public javax.swing.JTable Channeltable;
    public javax.swing.JButton CheckAlignButton;
    public javax.swing.JTextField DataSetSizeField;
    public javax.swing.JTextField DelayField;
    public javax.swing.JLabel DelayLabel;
    public javax.swing.JTextField DelayTextField;
    public javax.swing.JButton DirectoryButton;
    public javax.swing.JButton DownButton;
    public javax.swing.JButton DownDownButton;
    public javax.swing.JComboBox<String> Emission_ComboBox;
    public javax.swing.JLabel Emission_label;
    public javax.swing.JTextField EndField;
    public javax.swing.JLabel EndLabel;
    public javax.swing.JComboBox<String> Excitation_ComboBox;
    public javax.swing.JLabel Excitation_label;
    public javax.swing.JLabel FileLabel;
    public javax.swing.JTextField FileNameField;
    private javax.swing.JButton FilterButton;
    public javax.swing.JComboBox FirstCamComboBox;
    public javax.swing.JLabel FirstCamLabel;
    private javax.swing.JComboBox FunctionComboBox1;
    private javax.swing.JComboBox FunctionComboBox2;
    private javax.swing.JComboBox FunctionComboBox3;
    private javax.swing.JComboBox FunctionComboBox4;
    private javax.swing.JLabel FunctionLabel;
    public javax.swing.JButton G1Run;
    public javax.swing.JTextField G1RunTime;
    public javax.swing.JButton G2Run;
    public javax.swing.JTextField G2RunTime;
    public javax.swing.JLabel GAmpLabel1;
    public javax.swing.JLabel GAmpLabel2;
    public javax.swing.JLabel GAmpLabel3;
    public javax.swing.JLabel GAmpLabel4;
    public javax.swing.JTextField GAmpTextField1;
    public javax.swing.JTextField GAmpTextField2;
    public javax.swing.JTextField GAmpTextField3;
    public javax.swing.JTextField GAmpTextField4;
    public javax.swing.JLabel GFreqLabel1;
    public javax.swing.JLabel GFreqLabel3;
    public javax.swing.JTextField GFreqTextField1;
    public javax.swing.JTextField GFreqTextField3;
    public javax.swing.JButton GMoveButton1;
    public javax.swing.JButton GMoveButton2;
    public javax.swing.JButton GSetButton1;
    public javax.swing.JButton GSetButton2;
    private javax.swing.JButton GalvoButton;
    public javax.swing.JCheckBox GalvoCheckBox1;
    public javax.swing.JCheckBox GalvoCheckBox2;
    public javax.swing.JPanel GalvoControlPanel1;
    public javax.swing.JPanel GalvoControlPanel2;
    public javax.swing.JButton HelpButton;
    public javax.swing.JComboBox<String> Intensity_ComboBox;
    public javax.swing.JLabel Intensity_label;
    public javax.swing.JLabel IntervalLabel;
    public javax.swing.JTextField IntervalTime;
    public javax.swing.JComboBox IntervalUnits;
    public javax.swing.JButton LaserButton;
    public javax.swing.JCheckBox LaserCheckBox1;
    public javax.swing.JCheckBox LaserCheckBox2;
    public javax.swing.JCheckBox LaserCheckBox3;
    public javax.swing.JCheckBox LaserCheckBox4;
    public javax.swing.JPanel LaserPanel;
    public javax.swing.JSlider LaserSlider1;
    public javax.swing.JSlider LaserSlider2;
    public javax.swing.JSlider LaserSlider3;
    public javax.swing.JSlider LaserSlider4;
    public javax.swing.JCheckBox LedCheckBox1;
    public javax.swing.JCheckBox LedCheckBox2;
    public javax.swing.JCheckBox LedCheckBox3;
    public javax.swing.JCheckBox LedCheckBox4;
    public javax.swing.JLabel LedIntLabel;
    public javax.swing.JLabel LedIntLabel1;
    public javax.swing.JLabel LedLabel;
    public javax.swing.JLabel LedLabel1;
    public javax.swing.JLabel LedOnOffLabel;
    public javax.swing.JLabel LedOnOffLabel1;
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
    public javax.swing.JTextField MiddleField1;
    public javax.swing.JLabel MiddleLabel1;
    public javax.swing.JComboBox ModeComboBox;
    public javax.swing.JLabel ModeLabel;
    public javax.swing.JPanel ModePanel;
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
    public javax.swing.JTextField NumberofSlicesField;
    public javax.swing.JTextField PositionField;
    public javax.swing.JLabel PositionLabel;
    public javax.swing.JComboBox PumpsComboBox;
    public javax.swing.JLabel PumpsLabel;
    public javax.swing.JPanel PumpsPanel;
    private javax.swing.JButton RestoreButton;
    public javax.swing.JButton RightButton;
    public javax.swing.JTextField RootDirectoryField;
    public javax.swing.JLabel RootLabel;
    public javax.swing.JComboBox RotationComboBox;
    public javax.swing.JLabel RotationControlLabel;
    public javax.swing.JPanel RotationControlPanel;
    public javax.swing.JLabel RotationLabel;
    public javax.swing.JComboBox RotationMotorComboBox;
    public javax.swing.JPanel RotationPanel;
    public javax.swing.JCheckBox RotationSampleCheckBox;
    public javax.swing.JCheckBox SaveCheckBox;
    public javax.swing.JPanel SaveImagesPanel;
    public javax.swing.JComboBox SecondCamComboBox;
    public javax.swing.JLabel SecondCamLabel;
    public javax.swing.JButton SetEndButton;
    public javax.swing.JButton SetMiddleButton;
    public javax.swing.JButton SetSliceButton;
    public javax.swing.JButton SetStartButton;
    public javax.swing.JPanel Settings;
    public javax.swing.JTextArea SettingsTextArea;
    public javax.swing.JButton ShutterButton;
    public javax.swing.JCheckBox ShutterRotationCheckBox;
    public javax.swing.JCheckBox ShutterStackCheckBox;
    public javax.swing.JTextField SpeedTextField;
    public javax.swing.JCheckBox StackCheckBox;
    public javax.swing.JPanel StackPanel;
    private javax.swing.JButton StageButton;
    public javax.swing.JLabel StageControlLabel;
    public javax.swing.JPanel StageControlPanel;
    public javax.swing.JComboBox StageMotorComboBox;
    public javax.swing.JTextField StartField;
    public javax.swing.JLabel StartLabel;
    public javax.swing.JButton StartPjButton;
    private javax.swing.JComboBox StepAngleComboBox;
    public javax.swing.JLabel StepAngleLabel;
    public javax.swing.JTextField StepField;
    public javax.swing.JLabel StepLabel;
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
    public javax.swing.JLabel TimeLabel;
    public javax.swing.JCheckBox TimeLapseCheckBox;
    public javax.swing.JPanel TimeLapsePanel;
    public javax.swing.JTextField TotalTimeField;
    public javax.swing.JButton UPButton;
    public javax.swing.JButton UPUPButton;
    public javax.swing.JTextField UpField;
    public javax.swing.JTextField UpUpField;
    public javax.swing.JLabel VelocityLabel;
    public javax.swing.JTextField VolumeTextField;
    public javax.swing.JButton XLeftButton;
    public javax.swing.JButton XLeftLeftButton;
    public javax.swing.JComboBox XMotorComboBox;
    public javax.swing.JLabel XMotorLabel;
    public javax.swing.JTextField XPositionField;
    public javax.swing.JButton XRightButton;
    public javax.swing.JButton XRightRightButton;
    public javax.swing.JButton XYListButton;
    public javax.swing.JPanel XYPanel;
    public javax.swing.JLabel XYStageControl;
    public javax.swing.JTextField XYUpField;
    public javax.swing.JTextField XYUpUpField;
    public javax.swing.JButton YDownButton;
    public javax.swing.JButton YDownDownButton;
    public javax.swing.JComboBox YMotorComboBox;
    public javax.swing.JLabel YMotorLabel;
    public javax.swing.JTextField YPositionField;
    public javax.swing.JButton YUPButton;
    public javax.swing.JButton YUPUPButton;
    public javax.swing.JLabel ZMotorLabel;
    private javax.swing.JFileChooser fc;
    private javax.swing.JButton jButton1;
    public javax.swing.JPanel jPanel1;
    public javax.swing.JPanel jPanel10;
    public javax.swing.JPanel jPanel11;
    public javax.swing.JPanel jPanel12;
    public javax.swing.JPanel jPanel2;
    public javax.swing.JPanel jPanel3;
    public javax.swing.JPanel jPanel4;
    public javax.swing.JPanel jPanel6;
    public javax.swing.JPanel jPanel7;
    public javax.swing.JPanel jPanel8;
    public javax.swing.JPanel jPanel9;
    public javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration//GEN-END:variables
}
