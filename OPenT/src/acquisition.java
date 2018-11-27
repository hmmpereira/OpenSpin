
import mmcorej.CMMCore;
import mmcorej.TaggedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Color;
import org.micromanager.acquisition.AcquisitionEngine;
import org.micromanager.MMStudio;
import org.micromanager.api.ScriptInterface;


import ij.plugin.frame.ColorPicker;
import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;
import org.micromanager.api.StagePosition;


public class acquisition {
    private final CMMCore mmc_;                 
    private final JDialFinal dialog_;
    private final AcquisitionEngine acq_;
    private final MMStudio gui_;       
    ColorPicker colorpicker;
    private ShutterControl ShC_;
    private FilterControl FC_;
    private RotationControl2 RC_;
        
//GENERAL variables
    //Save files configuration
    private String Filename;
    private String Rootname;
    private boolean issaved;
    //Time lapse configuration
    private double intervalms;
    private int numFrames;
    //Channels configuration
    public int channels;
    private final String channelname[]= new String[6];
    private final String Excitation[]= new String[6];
    private final String Emission[]= new String[6];
    private final String Exposure[]= new String[6];
    private final String Intensity[]= new String[11];
    private final String Excitation_filters[]= new String[4];
    private final String Emission_filters[]= new String[6];
    private final String Intensity_filters[]= new String[11];
    private final Color color[] = new Color[6];
    private String StateDev_;
    private String ShutterDev_;
    private String StateDev_FW_;
    private String ShutterDev_FW_;

    public double input;
    public double Threshold;
 
    public acquisition(CMMCore core, JDialFinal dialog, AcquisitionEngine acq, ScriptInterface app, ShutterControl ShC, FilterControl FC, RotationControl2 RC){  
        mmc_ = core;
        dialog_=dialog;
        acq_= (AcquisitionEngine) acq;
        gui_=(MMStudio) app;
        ShC_=ShC;
        FC_=FC;
        RC_=RC;

    }
    
          
    public void Configure () {   /// THIS CONFIGURATIONS ARE IDENTICAL TO ALL ACQUISITON MODES
        ShC_ = new ShutterControl(mmc_, StateDev_, ShutterDev_, dialog_);
        FC_ = new FilterControl(mmc_, StateDev_FW_, ShutterDev_FW_, ShutterDev_);
        RC_ = new RotationControl2(mmc_, StateDev_, dialog_);
//        acq_=gui_.getAcquisitionEngine();
 ///////////   Save Files Configuration //////////////
        Filename="test";
        Rootname=dialog_.RootDirectoryField.getText();
        issaved=false;
        
        if(dialog_.SaveCheckBox.isSelected()){
            Rootname=dialog_.RootDirectoryField.getText();
            Filename=dialog_.FileNameField.getText();
            dialog_.MessageTextArea.setText(Rootname+"\\" +Filename);
            mmc_.sleep(1000);
            
            issaved=true;
            try{
            acq_.setDirName(Filename);
            acq_.setRootName(Rootname);
            acq_.setSaveFiles(true);
            acq_.setComment(dialog_.MessageTextArea.getText());
            } catch (Exception ex) {
                dialog_.MessageTextArea.setText("ERROR IN GETTING FILENAME");
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
 ////////////////  Time Lapse Configuration  ////////////////////  
        numFrames=1;
        Object numFramesS=dialog_.TimeFrames.getValue();
        numFrames=((Integer) numFramesS).intValue();
        double interval=Double.parseDouble(dialog_.IntervalTime.getText());
        String units=(String)dialog_.IntervalUnits.getSelectedItem();
        try{
        acq_.enableFramesSetting(false);
        } catch (Exception ex) {
                dialog_.MessageTextArea.setText("ERROR IN GETTING enableframessetting");
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            }
        intervalms=0;
        
       //// Creates time points in ms intervals////
       try{
        if (dialog_.TimeLapseCheckBox.isSelected()){
            acq_.enableFramesSetting(true);
                if ("s".equals(units)) {intervalms=interval*1000;}
                else if ("min".equals(units)) {intervalms=interval*60000;}
                else if ("ms".equals(units)) {intervalms=interval;}            
            acq_.setFrames(numFrames,intervalms);
        }
        else{
            numFrames=1;
            acq_.enableFramesSetting(true);
            acq_.setFrames(numFrames,interval);
        }
        } catch (Exception ex) {
                dialog_.MessageTextArea.setText("ERROR IN GETTING timepoints");
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            }

//////////// Channels configuration    /////////////////// 
        /// TO CONTROL TWO FILTER WHEELS CONTROLLED WITH ARDUINO BOARDS ///
        /// THIS MODE ONLY WORKS WITH ONE CAMERA ///
       
       channels=1;      
        for(int n=0;n<4;n++){
                Excitation_filters[n]=(String)dialog_.Excitation_ComboBox.getItemAt(n);
        }
        for(int n=0;n<6;n++){
                Emission_filters[n]=(String)dialog_.Emission_ComboBox.getItemAt(n);
                    }
        for(int l=0;l<11;l++){
                Intensity_filters[l]=(String)dialog_.Intensity_ComboBox.getItemAt(l);
        }
        if (dialog_.Channel_CheckBox.isSelected()){
            channels=0;
                for(int n=0;n<6;n++){
                    if(((Boolean)dialog_.Channeltable.getValueAt(n, 6)).booleanValue()==true){                           
                        channelname[channels]=(String)dialog_.Channeltable.getValueAt(n, 0);
                        Excitation[channels]=(String)dialog_.Channeltable.getValueAt(n, 1);
                        Emission[channels]=(String)dialog_.Channeltable.getValueAt(n, 2);                       
                        Exposure[channels]=(String)dialog_.Channeltable.getValueAt(n, 3).toString();
                        Intensity[channels]=(String)dialog_.Channeltable.getValueAt(n, 4).toString();
                        color[channels]=(Color)dialog_.Channeltable.getValueAt(n,5);
                        channels=channels+1;                         
                    }               
                }
            dialog_.MessageTextArea.setText("Number of channels is"+Integer.toString(channels));            
        }
        else{
             channels=1;
             channelname[0] = "Channel_0";
            try {               
                Exposure[0]=Double.toString(mmc_.getExposure());
            } catch (Exception ex) {
                dialog_.MessageTextArea.setText("ERROR IN GETTING EXPOSURE");
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            }
             Excitation[0]=(String)dialog_.Excitation_ComboBox.getSelectedItem();
             Emission[0]=(String)dialog_.Emission_ComboBox.getSelectedItem();            
        }
     }
    
  //SINGLE CAM MODES       


void OPT_Trig(){
    
    int Step_=200;
    int positions=1;
    boolean isshown=true;           
    String cam_name = mmc_.getCameraDevice();
    
    String Rotationdevice=(String) dialog_.RotationMotorComboBox.getSelectedItem();
    float RotStep=Float.parseFloat((String)dialog_.RotationComboBox.getSelectedItem());

    float STEPType = 1;
    int q = 0;
    float[] STEPS = {
        Float.parseFloat((String)dialog_.StepsComboBox1.getSelectedItem()),
        Float.parseFloat((String)dialog_.StepsComboBox2.getSelectedItem()),
        Float.parseFloat((String)dialog_.StepsComboBox3.getSelectedItem()),
        Float.parseFloat((String)dialog_.StepsComboBox4.getSelectedItem())
    }; 
    String[] StepT = {
        Integer.toString(dialog_.StepTypeComboBox1.getSelectedIndex()),
        Integer.toString(dialog_.StepTypeComboBox2.getSelectedIndex()),
        Integer.toString(dialog_.StepTypeComboBox3.getSelectedIndex()),
        Integer.toString(dialog_.StepTypeComboBox4.getSelectedIndex())
    };
    
    if ("Motor_1".equalsIgnoreCase(Rotationdevice)){
        q = 0;
    }
    else if ("Motor_2".equalsIgnoreCase(Rotationdevice)){
        q = 1;
    }
    else if ("Motor_3".equalsIgnoreCase(Rotationdevice)){
        q = 2;
    }
    else if ("Motor_4".equalsIgnoreCase(Rotationdevice)){
        q = 3;
    }
    
        //////////////////////////////////////////Define waht kind of steps motor will do (Full, Half, Quarter, Eight
    if("0".equalsIgnoreCase(StepT[q])){
        STEPType = 1;
    }
    else if("1".equalsIgnoreCase(StepT[q])){
        STEPType = 2;
    }
    else if("2".equalsIgnoreCase(StepT[q])){
        STEPType = 4;
    }
    else if("3".equalsIgnoreCase(StepT[q])){
        STEPType = 8;
    }
    PositionList pl=new PositionList();
    try {
            pl = gui_.getPositionList();
            gui_.setPositionList(pl);
            if(pl.getNumberOfPositions()>1){
                positions=pl.getNumberOfPositions(); 
            }
            String posString=Integer.toString(positions);
            dialog_.MessageTextArea.setText("Num pos"+posString);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            dialog_.MessageTextArea.setText("ERROR IN POSITION LIST");
        }
   
    try{
        
        if (gui_.isLiveModeOn()){
            gui_.enableLiveMode(false);
        }
        /* 
        This settings will change from camera to camera - we are using Hamamatsu Orca Flash 4.0 v2
        */
        String cam = mmc_.getCameraDevice();
        mmc_.setProperty(cam, "TRIGGER SOURCE", "EXTERNAL");
        mmc_.sleep(100);
        mmc_.setProperty(cam, "TRIGGER ACTIVE", "LEVEL");
        mmc_.sleep(100);
        mmc_.setProperty(cam, "TriggerPolarity", "POSITIVE");
        mmc_.sleep(100);
    } catch (Exception ex) {
            dialog_.MessageTextArea.setText("ERROR IN SETTING Cam in Acquisition Mode");
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    int Step_A = Math.round(360/RotStep);
    
    Step_ = Math.round((RotStep*STEPS[q]*STEPType)/360);
    String Step_f = Integer.toString(Step_);    
    dialog_.MessageTextArea.setText(Step_f+ " " + RotStep);
    
    int[] Excitation_Filters_Index={0,0,0,0};
    int[] Emission_Filters_Index={0,0,0,0,0,0};
    int[] Intensity_Index = {0,0,0,0,0,0,0,0,0,0,0};
    try{ 
        if(gui_.acquisitionExists(Filename)){
            gui_.closeAcquisition(Filename);
        }
        mmc_.sleep(500);
        gui_.openAcquisition(Filename, Rootname, numFrames, channels, Step_A, isshown, issaved);
        
        for(int n=0;n<channels;n++){                     

            for(int m=0;m<4;m++){
                if(Excitation_filters[m].equalsIgnoreCase(Excitation[n])){
                Excitation_Filters_Index[n]=m;
                }
            }
            for(int m=0;m<6;m++){
                if(Emission_filters[m].equalsIgnoreCase(Emission[n])){
                Emission_Filters_Index[n]=m;
                }
            }
            for(int l=0;l<11;l++){
                if(Intensity_filters[l].equalsIgnoreCase(Intensity[n])){
                Intensity_Index[n]=l;
                }
            }
        }
    
        int wi = (int) mmc_.getImageWidth();
        int he = (int)mmc_.getImageHeight();
        int by = (int)mmc_.getBytesPerPixel();
        int bi = (int)mmc_.getImageBitDepth();

        gui_.initializeAcquisition(Filename, wi, he, by, bi);
        }catch (Exception ex) {
        Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
        dialog_.MessageTextArea.setText("Error in Define acquisition");
    }
    
    
               
/////////// Define channels exposure times, intensities and display color //////////////////
            
    try{        
        //if(Global.shutter==0){ShC_.openshutter(); Global.shutter=1;};
            
        for (int o=0; o<(channels);o++){ /// Channels loop///
            if (dialog_.Channel_CheckBox.isSelected()){
                String INT = dialog_.Intensity[Intensity_Index[o]];
                mmc_.sleep(100);
                String CINT = FC_.ChangeIntensity(Integer.toString(Excitation_Filters_Index[o]), INT);
                mmc_.sleep(100);

                gui_.setChannelColor(Filename, o, color[o]);
                gui_.setChannelName(Filename, o, channelname[o]);

                try {
                    mmc_.sleep(100);
                    mmc_.setProperty("SRotation", "Command", "SEQUENCELD"+Excitation_Filters_Index[o]+"CAMEXP="+Exposure[o]+";");
                    mmc_.sleep(100);                    
                } catch (Exception ex) {
                    dialog_.MessageTextArea.setText("ERROR IN SETTING EXPOSURE Multiple Channel");
                    Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                }
            } 
            else{
                try {
                    mmc_.sleep(100);
                    mmc_.setProperty("SRotation", "Command", "SEQUENCELD"+Excitation_Filters_Index[o]+"CAMEXP="+Exposure[o]+";");
                    mmc_.sleep(100);                    
                } catch (Exception ex) {
                    dialog_.MessageTextArea.setText("ERROR IN SETTING EXPOSURE Single Channel");
                    Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                }
            } 
        }
 /////////////// Send information to arduino about number of steps/slices //////////////      
        long TIni;
        long TEnd;
        long TotalT;
        long Intervalms=(long)intervalms;

        mmc_.sleep(100);
        mmc_.setProperty("SRotation", "Command", "SEQUENCESTEPSBETWEEN="+Step_f+";");
        mmc_.sleep(100);
        mmc_.setProperty("SRotation", "Command", "SEQUENCEMODE=1;");

        double exposureMS = mmc_.getExposure();
        for (int n=0;n<(numFrames);n++){/// Time lapse loop ///

            TIni=System.currentTimeMillis();
            for (int m=0; m<positions;m++){
                /// SETS XYZ POSITIONS AND ANGLE

                if (pl.getNumberOfPositions()>0){
                    MultiStagePosition MSposition = pl.getPosition(m);
                    
                    double spX = MSposition.getX();
                    double spY = MSposition.getY();
                    String TargetX = Double.toString(spX);  
                    String TargetY = Double.toString(spY);      
                    mmc_.setXYPosition(spX, spY);
               
                }                        
                int slices = 0;
                mmc_.initializeCircularBuffer();
                mmc_.prepareSequenceAcquisition(cam_name);
                mmc_.startSequenceAcquisition(Step_A*channels, 0, true);
    //////////// Start Acquisition - camera is ready to receive and send trigrers from Arduino ////////////

                mmc_.sleep(500);
                mmc_.setProperty("SRotation", "Command", "START;"); 
                dialog_.MessageTextArea.setText("Start Acquisition");

                while (mmc_.getRemainingImageCount() > 0 || mmc_.isSequenceRunning(cam_name)){
                    if(mmc_.getRemainingImageCount() > 0){
                        //dialog_.MessageTextArea.setText("getremainingimagecount");

                        for(int c=0; c<channels;c++){                 
                                    mmc_.waitForDevice(cam_name);
                                    mmc_.setExposure(Double.parseDouble(Exposure[c]));
                                    mmc_.sleep(Double.parseDouble(Exposure[c]));
                                    TaggedImage img = mmc_.popNextTaggedImage();
                                    gui_.addImageToAcquisition(Filename, n, c, slices, 0, img);
                        } 

                        slices++;
                    }
                    else{
                        mmc_.sleep(Math.min(0.5*exposureMS, 20));
                    }

                }
                mmc_.stopSequenceAcquisition(cam_name);
            }

            TEnd=System.currentTimeMillis();
            TotalT=TEnd-TIni;
            /// WAITS FOR NEXT TIMEPOINT ///
            if (TotalT<Intervalms && n!=(numFrames-1)){gui_.sleep(Intervalms-TotalT);}

        }
    dialog_.MessageTextArea.setText("Acquisition Finish");
    try{
        String cam = mmc_.getCameraDevice();
        mmc_.setProperty(cam, "TRIGGER SOURCE", "INTERNAL");
        mmc_.sleep(100);
        mmc_.setProperty(cam, "TRIGGER ACTIVE", "SYNCREADOUT");
        mmc_.sleep(100);
        mmc_.setProperty(cam, "TriggerPolarity", "NEGATIVE");
        mmc_.sleep(100);
    } catch (Exception ex) {
            dialog_.MessageTextArea.setText("ERROR IN SETTING Cam in Live Mode");
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
    }    
            
    }      catch (Exception ex) {
        Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
        dialog_.MessageTextArea.setText("Error in Sequence Steps Between");
    }
    
};
void CheckAlignement (){   
            
        int slices=1;
            
        mmc_.setAutoShutter(false);
 
//////////  Sample Rotation Configuration  ///////////   
   String RotPosition=dialog_.AnglePositionField.getText();
   String RotStep="180";
   

///////////////  Stack Configuration  ////////////////// 
    /// WE ASIGN ROTATION ANGLES OF THE SAMPLE AS SLICES ////
    
        float RStep=Float.parseFloat(RotStep);
        slices=1;
        if (RStep==0){slices=1;}
        else {slices =(int) ((int) 360/RStep);}
        dialog_.MessageTextArea.setText("Slices are: "+Integer.toString(slices));
        boolean isshown=true;  
        
        try{ 
            gui_.openAcquisition(Filename, Rootname, numFrames, channels, slices, isshown, false);
            int wi = (int)mmc_.getImageWidth();
            int he = (int)mmc_.getImageHeight();
            int by = (int)mmc_.getBytesPerPixel();
            int bi = (int)mmc_.getImageBitDepth();

            gui_.initializeAcquisition(Filename, wi, he, by, bi);
        }catch (Exception ex) {
        Logger.getLogger(Final.class.getName()).log(Level.SEVERE, null, ex);
        dialog_.MessageTextArea.setText("Error in Define acquisition");
    }
////////////////  ACQUISITION IN OPT Mode (Slices are angles)  //////////////////   

        try {         
                    /////////////////////  RENAME CHANNELS  //////////////////

            int[] Excitation_Filters_Index={0,0,0,0};
            int[] Emission_Filters_Index={0,0,0,0,0,0};
            int[] Intensity_Index = {0,0,0,0,0,0,0,0,0,0,0};
               for(int n=0;n<channels;n++){                     
                        gui_.setChannelName(Filename, n, channelname[n]);
                                                  
                    for(int m=0;m<4;m++){
                        if(Excitation_filters[m].equalsIgnoreCase(Excitation[n])){
                            Excitation_Filters_Index[n]=m;
                        }
                    }
                    for(int m=0;m<6;m++){
                        if(Emission_filters[m].equalsIgnoreCase(Emission[n])){
                            Emission_Filters_Index[n]=m;
                        }
                    }
                    for(int l=0;l<11;l++){
                        if(Intensity_filters[l].equalsIgnoreCase(Intensity[n])){
                            Intensity_Index[n]=l;
                        }
                    }
            }                
               
//////////////////////  ACQUIRE  /////////////////////////////////////
            
            for(int l=0;l<(slices);l++){ /// Sample rotation loop ///                          
                for (int o=0; o<(channels);o++){ /// Channels loop///
                    if (dialog_.Channel_CheckBox.isSelected()){
                        String INT = dialog_.Intensity[Intensity_Index[o]];
                        mmc_.sleep(100);
                        String CINT = FC_.ChangeIntensity(Integer.toString(Excitation_Filters_Index[o]), INT);                                                           
                        gui_.setChannelColor(Filename, o, color[o]);
                    }
                    if(gui_.acquisitionExists(Filename)){
                        int mm=0;
                        gui_.snapAndAddImage(Filename, 0, o, l, mm);
                    }                                                            
                }///END OF: Channels loop ///

                RotPosition=RC_.leftMove(RotStep,RotPosition);                   
                dialog_.AnglePositionField.setText(RotPosition);
                mmc_.sleep(1000);
            }///END OF: Sample rotation loop ///
            gui_.closeAcquisition(Filename);
                    
           } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                dialog_.MessageTextArea.setText("Error in Check Alignment");
            }           



     }


}
