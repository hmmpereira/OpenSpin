package SPIM_FLUID;


import mmcorej.CMMCore;
import mmcorej.TaggedImage;
import mmcorej.StrVector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.DecimalFormat;
import java.awt.Color;
import org.micromanager.acquisition.AcquisitionEngine;
import org.micromanager.MMStudio;
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.*;

import java.text.NumberFormat;

import org.micromanager.dialogs.AcqControlDlg;

public class acquisition {
        private CMMCore mmc_;                 
        private JDialFinal dialog_;
        private AcquisitionEngine acq_;
        private MMStudio gui_;     
        private AcqControlDlg acqD_;
         
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
        private String channelname[]= new String[6];
        private String Excitation[]= new String[6];
        private String Emission[]= new String[6];
        public String Exposure[]= new String[6];
        private String Excitation_filters[]= new String[3];
        private String Emission_filters[]= new String[6];
        private Color color[] = new Color[6];
        
        private final String Intensity[]= new String[11];
        private final String Intensity_filters[]= new String[11];
        
        private String StateDev_;
        private String StateDev_FW_;
        private String ShutterDev_FW_;
        private String Master = Global.Master;
        public double input;
        public double Threshold;
 
   public acquisition(CMMCore core, JDialFinal dialog, AcquisitionEngine acq, ScriptInterface app, ShutterControl ShC, FilterControl FC, RotationControl2 RC){  
    mmc_ = core;
    dialog_=dialog;
    acq_=acq;
    gui_=(MMStudio) app;
    ShC_=ShC;
    FC_=FC;
    RC_=RC;
    }
    
          
    void Configure (){   /// THIS CONFIGURATIONS ARE IDENTICAL TO ALL ACQUISITON MODES
        
        String ShutterDev=mmc_.getShutterDevice();
        String StateDev="Arduino_SR-Switch";
        ShC_ = new ShutterControl(mmc_, StateDev, ShutterDev, dialog_);
        FC_ = new FilterControl(mmc_, StateDev_FW_, ShutterDev_FW_);
        RC_ = new RotationControl2(mmc_, StateDev_, dialog_);
        
 ///////////   Save Files Configuration //////////////             
        Filename="test";
        Rootname=dialog_.RootDirectoryField.getText();
        issaved=false;
        
        if(dialog_.SaveCheckBox.isSelected()){
            Rootname=dialog_.RootDirectoryField.getText();
            Filename=dialog_.FileNameField.getText();
            dialog_.MessageTextArea.setText(Rootname+"\\" +Filename);
            
            issaved=true;
            
            acq_.setDirName(Filename);
            acq_.setRootName(Rootname);
            acq_.setSaveFiles(true);
            acq_.setComment(dialog_.MessageTextArea.getText());
        }
        
 ////////////////  Time Lapse Configuration  ////////////////////  
        numFrames=1;
        Object numFramesS=dialog_.TimeFrames.getValue();
        numFrames=((Integer) numFramesS).intValue();
        double interval=Double.parseDouble(dialog_.IntervalTime.getText());
        String units=(String)dialog_.IntervalUnits.getSelectedItem();
        acq_.enableFramesSetting(false);
        intervalms=0;
        
       //// Creates time points in ms intervals////
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

//////////// Channels configuration    /////////////////// 
        /// TO CONTROL TWO FILTER WHEELS CONTROLLED WITH ARDUINO BOARDS ///
        /// THIS MODE ONLY WORKS WITH ONE CAMERA ///
       
       channels=1;      
        for(int n=0;n<3;n++){
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
String DSLM (){   
         
    acq_.clear();    
    gui_.closeAllAcquisitions();

    int slices=1;

    int positions=1;

    String focusdevice=(String) dialog_.StageMotorComboBox.getSelectedItem();
    String xyStage = mmc_.getXYStageDevice();
    mmc_.setAutoShutter(false);

    //////////// This is only to detect the Focus device position property!!!! ///////
    // IF appears in metadata this can be deleted ////
    StrVector devicePropertyNames;  
    String position_property="";
    NumberFormat formatter2 = new DecimalFormat("#.####");
    try {
        devicePropertyNames = mmc_.getDevicePropertyNames(focusdevice);

         for(int x=0;x<devicePropertyNames.size();x++){
             if (!mmc_.isPropertyReadOnly(focusdevice, devicePropertyNames.get(x))){
                    String property_value = mmc_.getProperty(focusdevice, devicePropertyNames.get(x));
                    String Position=formatter2.format(mmc_.getPosition(focusdevice)).replaceAll(",", ".");
                    if (property_value.compareTo(Position)==0){ position_property=devicePropertyNames.get(x);}
                    dialog_.MessageTextArea.setText(property_value+"  "+Position + devicePropertyNames.get(x));                    
                                   }
         }      
       } catch (Exception ex) {
        Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
         dialog_.MessageTextArea.setText("Error in device detection");
    }


/////////////// Mode Configuration  /////////////        
        
    String Mode=(String) dialog_.ModeComboBox.getSelectedItem();    
 
//////////  Sample Rotation Configuration  ///////////   
    String RotPosition=dialog_.AnglePositionField.getText();
    String RotStep=(String)dialog_.RotationComboBox.getSelectedItem();
    String Delay_OPT=dialog_.DelayField.getText();

    long delay_OPT=Long.parseLong(Delay_OPT);
   
    if (dialog_.RotationSampleCheckBox.isSelected()){     

       //// Creates number of multiview positions ////
        float RStep=Float.parseFloat(RotStep);
        positions=1;
        if (RStep==0){positions=1;}
        else {positions =(int) ((int) 360/RStep);}        
        
        //// Shutter configuration ////
        if(dialog_.ShutterRotationCheckBox.isSelected()==true){acq_.keepShutterOpenForChannels(false);}
        else{acq_.keepShutterOpenForChannels(true);}
    } 
 
///////////////  Stack Configuration  //////////////////        
        NumberFormat formatter = new DecimalFormat("#.0000");
        double ZINI=0;
        double ZSTEP=0.5;
        
    if ("DSLM/SPIM".equals(Mode)){
        double bottom=Double.parseDouble(dialog_.StartField.getText().replaceAll(",", "."));
        double top=Double.parseDouble(dialog_.EndField.getText().replaceAll(",", "."));
        double step=Double.parseDouble(dialog_.StepField.getText().replaceAll(",", "."));
        boolean absolute=true;
        String ZStep=dialog_.StepField.getText();
        ZSTEP=Double.parseDouble(ZStep.replaceAll(",", "."));
        String ZIni=dialog_.StartField.getText();
        ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        acq_.enableZSliceSetting(false);
       
        //// Creates 3D stack postions (TOP,BOTTOM,STEP)////
        if (dialog_.StackCheckBox.isSelected()){
            acq_.enableZSliceSetting(true);
            acq_.setSlices(bottom,top,step,absolute);                                   
            double aux=(top-bottom)/step;
            slices=(int)Math.ceil(aux)+1;
            ZIni=dialog_.StartField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        }
        else{
            acq_.enableZSliceSetting(false);
            slices=1;
            ZIni=dialog_.PositionField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        }           
        
        //// Shutter configuration ////
        if(dialog_.ShutterStackCheckBox.isSelected()==true){acq_.keepShutterOpenForStack(false);}
        else{acq_.keepShutterOpenForStack(true);}
    }


////////////////  ACQUISITION IN DSLM/SPIM Mode //////////////////                  
    if ("DSLM/SPIM".equals(Mode)){
        try {
            boolean isshown=true; 
            
            if (gui_.isLiveModeOn()){
                gui_.enableLiveMode(false);
            }

            gui_.openAcquisition(Filename, Rootname, numFrames, channels, slices, positions, isshown, issaved);

   /////////////////////  RENAME CHANNELS  //////////////////
                
            int[] Excitation_Filters_Index={0,0,0};
            int[] Emission_Filters_Index={0,0,0,0,0,0};
            for(int n=0;n<channels;n++){                     
                gui_.setChannelName(Filename, n, channelname[n]);
                for(int m=0;m<3;m++){
                    if(Excitation_filters[m].equalsIgnoreCase(Excitation[n])){
                        Excitation_Filters_Index[n]=m;
                    }
                }
                for(int m=0;m<6;m++){
                    if(Emission_filters[m].equalsIgnoreCase(Emission[n])){
                        Emission_Filters_Index[n]=m;
                    }
                }  
            }
           
            int wi = (int) mmc_.getImageWidth();
            int he = (int)mmc_.getImageHeight();
            int by = (int)mmc_.getBytesPerPixel();
            int bi = (int)mmc_.getImageBitDepth();

            gui_.initializeAcquisition(Filename, wi, he, by, bi);
            

//////////////////////  ACQUIRE  /////////////////////////////////////

            try {
                mmc_.setPosition(focusdevice,ZINI);
                mmc_.sleep(100);
                String Position_new=formatter.format(mmc_.getPosition(focusdevice));
                dialog_.PositionField.setText(Position_new);
            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                dialog_.MessageTextArea.setText("ERROR IN GOING TO INITIAL POSITION");
            }
            long TIni;
            long TEnd;
            long TotalT;
            long Intervalms=(long)intervalms;
                       
            for (int n=0;n<(numFrames);n++){ /// Time lapse loop ///
                TIni=System.currentTimeMillis();            
                if (Global.shutter==0){ShC_.openshutter(); Global.shutter=1;}
                    
                for(int m=0;m<(positions);m++){ /// Multiview loop ///
                    if (Global.shutter==0){ShC_.openshutter(); Global.shutter=1;}

                    for (int o=0; o<(channels);o++){ ///Channels loop ///

                        if (dialog_.Channel_CheckBox.isSelected()){
                            ///CHANGE FILTERS CONFIGURATION///
                            String CIFEx=FC_.ChangeExcitationLaser(Excitation_Filters_Index[o]);
                            dialog_.Excitation_ComboBox.setSelectedIndex(Excitation_Filters_Index[o]);
                            String CIEm=FC_.ChangeEmissionFilter(Emission_Filters_Index[o]);
                            dialog_.Emission_ComboBox.setSelectedIndex(Emission_Filters_Index[o]);
                            gui_.setChannelColor(Filename, o, color[o]);
                            try {
                                mmc_.setExposure(Double.parseDouble(Exposure[o]));
                            } catch (Exception ex) {
                                dialog_.MessageTextArea.setText("ERROR IN SETTING EXPOSURE");
                                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            dialog_.MessageTextArea.setText("The filter is: "+Integer.toString(Emission_Filters_Index[o]+1)+"/n"+"The CIEm is: "+ CIEm);
                            gui_.sleep(delay_OPT);
                            }
                            else{dialog_.Excitation_ComboBox.setSelectedIndex(Excitation_Filters_Index[o]);};
                            /*
                            // Calculate time for each stack - Galvo will run non-stop during this interval
                            */
                            double TG= Integer.parseInt(Exposure[o])*slices*1.2;
                            dialog_.G1RunTime.setText(Integer.toString((int)Math.ceil(0.001*TG)));
                            dialog_.G1Run.doClick();
                            double TGIni=System.currentTimeMillis();

                            /////////////////////////////////////////////////////////////////////////////////

                            for(int l=0;l<(slices);l++){ ///3D Stack loop ////

                                if(gui_.acquisitionExists(Filename)){
                                    gui_.snapAndAddImage(Filename, n, o, l, m);
                                }
                                else {n=numFrames;m=positions;l=slices;}
                                if(l!=slices-1){

                                    try {  ///SETS NEW POSITION
                                        double position = mmc_.getPosition(focusdevice); 
                                        double position_new=position+ZSTEP;

                                        String Position_new0=Double.toString(position_new).replace(",",".");
                                        dialog_.MessageTextArea.setText(Position_new0);

                                        mmc_.setPosition(focusdevice,position_new);
                                        String Position_new=formatter.format(mmc_.getPosition(focusdevice));
                                        dialog_.PositionField.setText(Position_new);
                                    } catch (Exception ex) {
                                        Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                                        dialog_.MessageTextArea.setText("ERROR IN MOVING");
                                    }
                                }

                            }  ///END OF: 3D Stack loop ////

                            try {////GO TO INI POSITION ////
                                mmc_.setPosition(focusdevice,ZINI);
                                mmc_.sleep(100);
                                String Position_new=formatter.format(mmc_.getPosition(focusdevice));
                                dialog_.PositionField.setText(Position_new);
                            } catch (Exception ex) {
                                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            if (dialog_.ShutterRotationCheckBox.isSelected()==true){ShC_.closeshutter();Global.shutter=0;dialog_.MessageTextArea.setText("shutter OFF");}

                            /*
                            // Set a wait check point to prevent eventual conflict if the galvo is still running
                            */
                            double TGEnd = System.currentTimeMillis();
                            double TotalGT = TGEnd- TGIni;
                            if (TotalGT<TG){gui_.sleep((long) (TG-TotalGT));}
                            ////////////////////////////////////////////////////////////////////////////////////

                    } ///END OF: Channel loop ////

                    if(positions>1){ShC_.openshutter();Global.shutter=1;}
                    ///SETS NEW VIEW///
                    RotPosition=RC_.leftMove(RotStep,RotPosition);                        
                    dialog_.AnglePositionField.setText(RotPosition);

                    if(dialog_.ShutterRotationCheckBox.isSelected()==true){ShC_.closeshutter();Global.shutter=0;}
                    } ///END OF: Multiview loop ////

                    TEnd=System.currentTimeMillis();
                    TotalT=TEnd-TIni;
                    dialog_.MessageTextArea.setText(String.valueOf(TotalT));

                    if (Global.shutter==1){ShC_.closeshutter(); Global.shutter=0;}

                    /// WAITS FOR NEXT TIMEPOINT ///
                    if (TotalT<Intervalms && n!=(numFrames-1)){gui_.sleep(Intervalms-TotalT);}
             } ///END OF: Time lapse loop ////
                
       
                ShC_.closeshutter();Global.shutter=0;dialog_.MessageTextArea.setText("shutter OFF");
                gui_.closeAcquisition(Filename);
                //FC_.ChangeExcitationFilter(Excitation_Filters_Index[0]);
                dialog_.Excitation_ComboBox.setSelectedIndex(Excitation_Filters_Index[0]);
                FC_.ChangeEmissionFilter(Emission_Filters_Index[0]);
                dialog_.Emission_ComboBox.setSelectedIndex(Emission_Filters_Index[0]);
            
            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                dialog_.MessageTextArea.setText("Error in DSLM/SPIM Mode");
            }
        }//END IF

        return Mode;
     }


String SPIM_Trig(){
    String mode = "SPIM_Trig";
    String cam_name = mmc_.getCameraDevice();
    acq_.clear();    
    gui_.closeAllAcquisitions();
    
    PositionList pl=new PositionList();

    int slices  = 1;
    int slices_ = 0;
    int galvos  = 1;
    int channelsG = channels * galvos;
    String Step_f = Integer.toString(slices);

    int positions=1;
    
    if(dialog_.GalvoCheckBox1.isSelected() || dialog_.GalvoCheckBox2.isSelected()){
        int galvo1 = dialog_.GalvoCheckBox1.isSelected() ? 1:0;
        int galvo2 = dialog_.GalvoCheckBox2.isSelected() ? 1:0;
        galvos = galvo1+galvo2;
    } 
    
    String focusdevice=(String) dialog_.StageMotorComboBox.getSelectedItem();
    String xyStage = mmc_.getXYStageDevice();
    mmc_.setAutoShutter(false);

    //////////// This is only to detect the Focus device position property!!!! ///////
    // IF appears in metadata this can be deleted ////
    StrVector devicePropertyNames;  
    String position_property="";
    NumberFormat formatter2 = new DecimalFormat("#.####");
    try {
        devicePropertyNames = mmc_.getDevicePropertyNames(focusdevice);

        for(int x=0;x<devicePropertyNames.size();x++){
            if (!mmc_.isPropertyReadOnly(focusdevice, devicePropertyNames.get(x))){
                String property_value = mmc_.getProperty(focusdevice, devicePropertyNames.get(x));
                String Position=formatter2.format(mmc_.getPosition(focusdevice)).replaceAll(",", ".");
                if (property_value.compareTo(Position)==0){ position_property=devicePropertyNames.get(x);}
                dialog_.MessageTextArea.setText(property_value+"  "+Position + devicePropertyNames.get(x));                    
            }
        }      
    } catch (Exception ex) {
        Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
        dialog_.MessageTextArea.setText("Error in device detection");
    }


/////////////// Mode Configuration  /////////////        
        
    String Mode=(String) dialog_.ModeComboBox.getSelectedItem();    
 
//////////  Sample Rotation Configuration  ///////////   
    String RotPosition=dialog_.AnglePositionField.getText();
    String RotStep=(String)dialog_.RotationComboBox.getSelectedItem();
    String Delay_Stage=dialog_.DelayField.getText();

    double delay_Stage=Double.parseDouble(Delay_Stage);

    if (dialog_.RotationSampleCheckBox.isSelected()){     

        //// Creates number of multiview positions ////
        float RStep=Float.parseFloat(RotStep);
        positions=1;
        if (RStep==0){positions=1;}
        else {positions =(int) ((int) 360/RStep);}        

        //// Shutter configuration ////
        if(dialog_.ShutterRotationCheckBox.isSelected()==true){acq_.keepShutterOpenForChannels(false);}
        else{acq_.keepShutterOpenForChannels(true);}
    } 
 
///////////////  Stack Configuration  //////////////////        
    NumberFormat formatter = new DecimalFormat("#.0000");
    double ZINI=0;
    double ZEND=0;
    double ZSTEP=0.5;
    
    double bottom=Double.parseDouble(dialog_.StartField.getText().replaceAll(",", "."));
    double top=Double.parseDouble(dialog_.EndField.getText().replaceAll(",", "."));
    double step=Double.parseDouble(dialog_.StepField.getText().replaceAll(",", "."));
    
    if ("SPIM_Trig".equals(Mode)){

        boolean absolute=true;
        String ZStep=dialog_.StepField.getText();
        ZSTEP=Double.parseDouble(ZStep.replaceAll(",", "."));
        String ZIni=dialog_.StartField.getText();
        ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        String ZEnd=dialog_.EndField.getText();
        ZEND=Double.parseDouble(ZEnd.replaceAll(",", "."));
        acq_.enableZSliceSetting(false);

        //// Creates 3D stack postions (TOP,BOTTOM,STEP)////
        if (dialog_.StackCheckBox.isSelected()){
            acq_.enableZSliceSetting(true);
            acq_.setSlices(bottom,top,step,absolute);                                   
            double aux=(top-bottom)/step;
            slices=(int)Math.ceil(aux)+1;
            ZIni=dialog_.StartField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
            ZEnd=dialog_.EndField.getText();
            ZEND=Double.parseDouble(ZEnd.replaceAll(",", "."));
            Step_f = Integer.toString(slices);
        }
        else{
            acq_.enableZSliceSetting(false);
            slices=1;
            ZIni=dialog_.PositionField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        }
        
        channelsG = channels * galvos; //---------------------------- Slices times number of active galvos ----------------------

        //// Shutter configuration ////
        if(dialog_.ShutterStackCheckBox.isSelected()==true){acq_.keepShutterOpenForStack(false);}
        else{acq_.keepShutterOpenForStack(true);}
    }
        //// Multipositon ///////
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


////////////////  ACQUISITION IN SPIM Trig Mode //////////////////                  
    if ("SPIM_Trig".equals(Mode)){
        try {
            boolean isshown=true; 
            
            if(gui_.acquisitionExists(Filename)){
                gui_.closeAcquisition(Filename);
            }
            
            if (gui_.isLiveModeOn()){
                gui_.enableLiveMode(false);
            }
            String cam = mmc_.getCameraDevice();
            mmc_.setProperty(cam, "TRIGGER SOURCE", "EXTERNAL");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "OUTPUT TRIGGER POLARITY[2]", "POSITIVE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TRIGGER ACTIVE", "EDGE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "SENSOR MODE", "AREA");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TriggerPolarity", "POSITIVE");
            mmc_.sleep(100);
            


            gui_.openAcquisition(Filename, Rootname, numFrames, channelsG, slices, positions, isshown, issaved);

   /////////////////////  RENAME CHANNELS  //////////////////
                
            int[] Excitation_Filters_Index={0,0,0};
            int[] Emission_Filters_Index={0,0,0,0,0,0};
            int[] Intensity_Index = {0,0,0,0,0,0,0,0,0,0,0};
            for(int n=0;n<channels;n++){                     
                gui_.setChannelName(Filename, n, channelname[n]);
                                                  
                for(int m=0;m<3;m++){
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
           
            int wi = (int)mmc_.getImageWidth();
            int he = (int)mmc_.getImageHeight();
            int by = (int)mmc_.getBytesPerPixel();
            int bi = (int)mmc_.getImageBitDepth();

            gui_.initializeAcquisition(Filename, wi, he, by, bi);
            
            

               
/////////// Define channels exposure times, intensities and display color //////////////////
            
            try{        
                if(Global.shutter==0){ShC_.openshutter(); Global.shutter=1;};

                for (int o=0; o<(channels);o++){ /// Channels loop///
                    if (dialog_.Channel_CheckBox.isSelected()){
                        int INT = Integer.valueOf(dialog_.Intensity[Intensity_Index[o]]);
                        if (Excitation_Filters_Index[o] == 0){
                            INT = Intensity_Index[o];
                        };
                        mmc_.sleep(100);
                        FC_.ChangeIntensity(Excitation_Filters_Index[o], INT);
                        mmc_.sleep(100);
                        FC_.ChangeEmissionFilter(Emission_Filters_Index[o]);
                        mmc_.sleep(100);
//                        dialog_.MessageTextArea.setText("Current Emission Filter is: "+CEMI);
                        if(galvos == 2){
                            gui_.setChannelColor(Filename, (o*2), color[o]);
                            mmc_.sleep(1);
                            gui_.setChannelColor(Filename, (o*2)+1, color[o]);
                            mmc_.sleep(1);
                            gui_.setChannelName(Filename, (o*2), channelname[o]+"_IL1");
                            mmc_.sleep(1);
                            gui_.setChannelName(Filename, (o*2)+1, channelname[o]+"_IL2");
                        }
                        else{
                            gui_.setChannelColor(Filename, o, color[o]);
                            gui_.setChannelName(Filename, o, channelname[o]);
                        }
                        

                        try {
                            mmc_.sleep(100);
                            mmc_.setProperty(Master, "Command", "SEQUENCELASER"+Excitation_Filters_Index[o]+"CAMEXP="+Exposure[o]+";");
                            dialog_.MessageTextArea.setText("EXPOSURE: "+Exposure[o]);
                        } catch (Exception ex) {
                            dialog_.MessageTextArea.setText("ERROR IN SETTING EXPOSURE");
                            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                else{
                    try {
                        if(galvos == 2){
                            gui_.setChannelName(Filename, (o*2), channelname[o]+"_IL1");
                            mmc_.sleep(1);
                            gui_.setChannelName(Filename, (o*2)+1, channelname[o]+"_IL2");
                        }
                        else{
                            gui_.setChannelColor(Filename, o, color[o]);
                            gui_.setChannelName(Filename, o, channelname[o]);
                        }
                        mmc_.sleep(100);
                        mmc_.setProperty("SRotation", "Command", "SEQUENCELASER"+Excitation_Filters_Index[o]+"CAMEXP="+Exposure[o]+";");
                        mmc_.sleep(100);                    
                    } catch (Exception ex) {
                        dialog_.MessageTextArea.setText("ERROR IN SETTING EXPOSURE");
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
                mmc_.setProperty(Master, "Command", "STAGE0DELAY="+Delay_Stage+";");
                mmc_.sleep(100);
                mmc_.setProperty(Master, "Command", "SEQUENCEMODE=2;");
                mmc_.sleep(100);
                mmc_.setProperty(Master, "Command", "STAGE0TOTALSTEPS="+slices+";");
                mmc_.sleep(100);

                double exposureMS = mmc_.getExposure();
                for (int n=0;n<(numFrames);n++){/// Time lapse loop ///
                    
                    TIni=System.currentTimeMillis();
                    
                    for (int m=0; m<positions;m++){
                        /// SETS XYZ POSITIONS AND ANGLE
                        
                        if (pl.getNumberOfPositions()>0){
                            MultiStagePosition MSposition = pl.getPosition(m);

                            String StageX=(String) dialog_.XMotorComboBox.getSelectedItem();                     
                            StagePosition sp = MSposition.get(StageX); 
                            String TargetX=Double.toString(sp.x);
                            dialog_.MessageTextArea.setText("TARGET "+StageX+ " " +TargetX);   
                            mmc_.setPosition(StageX,sp.x);                
                            String fpos=formatter.format(mmc_.getPosition(StageX));
                            dialog_.XPositionField.setText(fpos);
                            
                            int posC = 0;
                            while (mmc_.getPosition(StageX) != sp.x && posC <= 200){
                                mmc_.sleep(10);
                                posC++;
                            }

                            String StageY=(String) dialog_.YMotorComboBox.getSelectedItem(); 
                            sp = MSposition.get(StageY);   
                            String TargetY=Double.toString(sp.x);
                            dialog_.MessageTextArea.setText("TARGET "+StageY+ " " +TargetY);              
                            mmc_.setPosition(StageY,sp.x);
                            fpos=formatter.format(mmc_.getPosition(StageY));
                            dialog_.YPositionField.setText(fpos);
                            
                            posC = 0;
                            while (mmc_.getPosition(StageY) != sp.x && posC <= 200){
                                mmc_.sleep(10);
                                posC++;
                            }
                            
                            String StageZ=(String) dialog_.StageMotorComboBox.getSelectedItem();
                            sp = MSposition.get(StageZ);   
                            String TargetZ=Double.toString(sp.x);
                            dialog_.MessageTextArea.setText("TARGET "+StageZ+ " " +TargetZ);              
                            mmc_.setPosition(StageZ,sp.x);
                            fpos=formatter.format(mmc_.getPosition(StageZ));
                            dialog_.PositionField.setText(fpos);
                            ZINI = sp.x;
                            
                            posC = 0;
                            while (mmc_.getPosition(StageZ) != sp.x && posC <= 200){
                                mmc_.sleep(10);
                                posC++;
                            }
                        }
                        /////////////////////////////////////
                        try {  ///SETS NEW POSITION
                        double position = mmc_.getPosition(focusdevice); 
                        if (position != ZINI){
                            mmc_.setPosition(focusdevice,ZINI);
                            mmc_.sleep(100);
                            String Position_new=formatter.format(mmc_.getPosition(focusdevice));
                            dialog_.PositionField.setText(Position_new);
                        }
                        } catch (Exception ex) {
                            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                            dialog_.MessageTextArea.setText("ERROR IN MOVING");
                        }
                        
                        gui_.setAcquisitionProperty(Filename, "z-step_um", Double.toString(step));
                        
                        mmc_.initializeCircularBuffer();
                        mmc_.prepareSequenceAcquisition(cam_name);
                        mmc_.startSequenceAcquisition(slices*channelsG, 0, true);

            //////////// Start Acquisition - camera is ready to receive and send trigrers from Arduino ////////////

                        int a = 0;
                        mmc_.sleep(500);
                        mmc_.setProperty(Master, "Command", "START;"); 
                        dialog_.MessageTextArea.setText("Start Acquisition");

                        while (mmc_.getRemainingImageCount() > 0 || mmc_.isSequenceRunning(cam_name)){
                            if(mmc_.getRemainingImageCount() > 0){

                                for(int c=0; c<channelsG;c++){
                                    mmc_.waitForDevice(cam_name);
                                    mmc_.sleep(Double.parseDouble(Exposure[a]));
                                    TaggedImage img = mmc_.popNextTaggedImage();
                                    gui_.addImageToAcquisition(Filename, n, c, slices_, m, img);
                                    if(c % 2 == 0 && c>1){a++;};
                                } 

                                slices_++;
                                a = 0;
                            }
                            else{
                                mmc_.sleep(Math.min(0.5*exposureMS, 20));
                            }
                        }
                        slices_=0;
                        mmc_.stopSequenceAcquisition();

                        ////GO TO Initial POSITION ////
                        try {
                            mmc_.setPosition(focusdevice,ZINI);
                            mmc_.sleep(500);
                            String Position_new=formatter.format(mmc_.getPosition(focusdevice));
                            dialog_.PositionField.setText(Position_new);
                        } catch (Exception ex) {
                            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                            dialog_.MessageTextArea.setText("ERROR GOING TO INI POS");
                        }
                        if (dialog_.RotationSampleCheckBox.isSelected()){
                            String Step;
                            String Position;
                            Step=(String)dialog_.RotationComboBox.getSelectedItem();
                            Position=dialog_.AnglePositionField.getText();
                            Position=RC_.rightMove(Step,Position);
                            dialog_.AnglePositionField.setText(Position);
                        }
                        mmc_.sleep(1000);
                        ShC_.closeshutter(); 
                        Global.shutter=0;
                    }

                    TEnd=System.currentTimeMillis();
                    TotalT=TEnd-TIni;
                    /// WAITS FOR NEXT TIMEPOINT ///
                    if (TotalT<Intervalms && n!=(numFrames-1)){gui_.sleep(Intervalms-TotalT);}

                }
            dialog_.MessageTextArea.setText("Acquisition Finish");
                    
            mmc_.setProperty(cam, "TRIGGER SOURCE", "INTERNAL");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "OUTPUT TRIGGER POLARITY[2]", "NEGATIVE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TRIGGER ACTIVE", "EDGE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "SENSOR MODE", "AREA");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TriggerPolarity", "NEGATIVE");
            mmc_.sleep(100);

            }      catch (Exception ex) {
                Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
                dialog_.MessageTextArea.setText("Error in Sequence Steps Between");
            }

        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            dialog_.MessageTextArea.setText("Error in SPIM_Trig Mode");
        }
    }
    return mode;
            
};

String Test(){
    String mode = "Test";
    String cam_name = mmc_.getCameraDevice();
    acq_.clear();    
    gui_.closeAllAcquisitions();

    int slices  = 1;
    int slices_ = 0;
    int galvos  = 1;
    int slicesG = slices * galvos;
    String Step_f = Integer.toString(slices);

    int positions=1;
    
    if(dialog_.GalvoCheckBox1.isSelected() || dialog_.GalvoCheckBox2.isSelected()){
        int galvo1 = dialog_.GalvoCheckBox1.isSelected() ? 1:0;
        int galvo2 = dialog_.GalvoCheckBox1.isSelected() ? 1:0;
        galvos = galvo1+galvo2;
    }

    String focusdevice=(String) dialog_.StageMotorComboBox.getSelectedItem();
    String xyStage = mmc_.getXYStageDevice();
    mmc_.setAutoShutter(false);

    //////////// This is only to detect the Focus device position property!!!! ///////
    // IF appears in metadata this can be deleted ////
    StrVector devicePropertyNames;  
    String position_property="";
    NumberFormat formatter2 = new DecimalFormat("#.####");
    try {
        devicePropertyNames = mmc_.getDevicePropertyNames(focusdevice);

        for(int x=0;x<devicePropertyNames.size();x++){
            if (!mmc_.isPropertyReadOnly(focusdevice, devicePropertyNames.get(x))){
                String property_value = mmc_.getProperty(focusdevice, devicePropertyNames.get(x));
                String Position=formatter2.format(mmc_.getPosition(focusdevice)).replaceAll(",", ".");
                if (property_value.compareTo(Position)==0){ position_property=devicePropertyNames.get(x);}
                dialog_.MessageTextArea.setText(property_value+"  "+Position + devicePropertyNames.get(x));                    
            }
        }      
    } catch (Exception ex) {
        Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
        dialog_.MessageTextArea.setText("Error in device detection");
    }


/////////////// Mode Configuration  /////////////        
        
    String Mode=(String) dialog_.ModeComboBox.getSelectedItem();    
 
//////////  Sample Rotation Configuration  ///////////   
    String RotPosition=dialog_.AnglePositionField.getText();
    String RotStep=(String)dialog_.RotationComboBox.getSelectedItem();
    String Delay_Stage=dialog_.DelayField.getText();

    double delay_Stage=Double.parseDouble(Delay_Stage);

    if (dialog_.RotationSampleCheckBox.isSelected()){     

        //// Creates number of multiview positions ////
        float RStep=Float.parseFloat(RotStep);
        positions=1;
        if (RStep==0){positions=1;}
        else {positions =(int) ((int) 360/RStep);}        

        //// Shutter configuration ////
        if(dialog_.ShutterRotationCheckBox.isSelected()==true){acq_.keepShutterOpenForChannels(false);}
        else{acq_.keepShutterOpenForChannels(true);}
    } 
 
///////////////  Stack Configuration  //////////////////        
    NumberFormat formatter = new DecimalFormat("#.0000");
    double ZINI=0;
    double ZEND=0;
    double ZSTEP=0.5;
    double bottom=Double.parseDouble(dialog_.StartField.getText().replaceAll(",", "."));
    double top=Double.parseDouble(dialog_.EndField.getText().replaceAll(",", "."));
    double step=Double.parseDouble(dialog_.StepField.getText().replaceAll(",", "."));
        
    if ("Test".equals(Mode)){
        
        boolean absolute=true;
        String ZStep=dialog_.StepField.getText();
        ZSTEP=Double.parseDouble(ZStep.replaceAll(",", "."));
        String ZIni=dialog_.StartField.getText();
        ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        String ZEnd=dialog_.EndField.getText();
        ZEND=Double.parseDouble(ZEnd.replaceAll(",", "."));
        acq_.enableZSliceSetting(false);

        //// Creates 3D stack postions (TOP,BOTTOM,STEP)////
        if (dialog_.StackCheckBox.isSelected()){
            acq_.enableZSliceSetting(true);
            acq_.setSlices(bottom,top,step,absolute);                                   
            double aux=(top-bottom)/step;
            slices=(int)Math.ceil(aux)+1;
            ZIni=dialog_.StartField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
            ZEnd=dialog_.EndField.getText();
            ZEND=Double.parseDouble(ZEnd.replaceAll(",", "."));
            Step_f = Integer.toString(slices);
        }
        else{
            acq_.enableZSliceSetting(false);
            slices=1;
            ZIni=dialog_.PositionField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        }
        
        slicesG = slices * galvos; //---------------------------- Slices times number of active galvos ----------------------

        //// Shutter configuration ////
        if(dialog_.ShutterStackCheckBox.isSelected()==true){acq_.keepShutterOpenForStack(false);}
        else{acq_.keepShutterOpenForStack(true);}
    }

    PositionList pl=new PositionList();
                try {
                    pl = gui_.getPositionList();
                    gui_.setPositionList(pl);
                    positions=pl.getNumberOfPositions();
                    String posString=Integer.toString(positions);

                    dialog_.MessageTextArea.setText("Num pos"+posString);
                } catch (Exception ex) {
                    Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                    dialog_.MessageTextArea.setText("ERROR IN POSITION LIST");
                }

////////////////  Test Mode //////////////////                  
    if ("Test".equals(Mode)){
        try {
            boolean isshown=true; 
            
            if (gui_.isLiveModeOn()){
                gui_.enableLiveMode(false);
            }
            gui_.closeAllAcquisitions();

            gui_.openAcquisition(Filename, Rootname, numFrames, channels, slicesG, positions, isshown, issaved);

   /////////////////////  RENAME CHANNELS  //////////////////
                
            int[] Excitation_Filters_Index={0,0,0};
            int[] Emission_Filters_Index={0,0,0,0,0,0};
            int[] Intensity_Index = {0,0,0,0,0,0,0,0,0,0,0};
            for(int n=0;n<channels;n++){                     
                gui_.setChannelName(Filename, n, channelname[n]);
                                                  
                for(int m=0;m<3;m++){
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
           
            int wi = (int)mmc_.getImageWidth();
            int he = (int)mmc_.getImageHeight();
            int by = (int)mmc_.getBytesPerPixel();
            int bi = (int)mmc_.getImageBitDepth();

            gui_.initializeAcquisition(Filename, wi, he, by, bi);
                               
/////////// Define channels exposure times, intensities and display color //////////////////
            
            try{        
                if(Global.shutter==0){ShC_.openshutter(); Global.shutter=1;};

                for (int o=0; o<(channels);o++){ /// Channels loop///
                    if (dialog_.Channel_CheckBox.isSelected()){
                        int INT = Integer.valueOf(dialog_.Intensity[Intensity_Index[o]]);
                        if (Excitation_Filters_Index[o] == 0){
                            INT = Intensity_Index[o];
                        };

                        gui_.setChannelColor(Filename, o, color[o]);
                        gui_.setChannelName(Filename, o, channelname[o]);

                        try {
//                            mmc_.sleep(100);
//                            mmc_.setProperty(Master, "Command", "SEQUENCELASER"+Excitation_Filters_Index[o]+"CAMEXP="+Exposure[o]+";");
                            dialog_.MessageTextArea.setText("EXPOSURE: "+Exposure[o]);
                        } catch (Exception ex) {
                            dialog_.MessageTextArea.setText("ERROR IN SETTING EXPOSURE");
                            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
//                    else{mmc_.sleep(100);
//                    
//                        gui_.setChannelName(Filename, o, channelname[o]);
//                        
//                        mmc_.setProperty(Master, "Command", "SEQUENCELASER"+Excitation_Filters_Index[o]+"CAMEXP="+Exposure[o]+";");
//                    }
                }
         /////////////// Send information to arduino about number of steps/slices //////////////      
                long TIni;
                long TEnd;
                long TotalT;
                long Intervalms=(long)intervalms;
                
//                mmc_.sleep(100);
////                mmc_.setProperty(Master, "Command", "STAGE0ENABLE=1;");
//                mmc_.setProperty(Master, "Command", "STAGE0DELAY="+Delay_Stage+";");
//                mmc_.sleep(100);
//                mmc_.setProperty(Master, "Command", "SEQUENCEMODE=2;");
//                mmc_.sleep(100);
//                mmc_.setProperty(Master, "Command", "STAGE0TOTALSTEPS="+slices+";");
//                mmc_.sleep(100);

                double exposureMS = mmc_.getExposure();
                for (int n=0;n<(numFrames);n++){/// Time lapse loop ///
                    TIni=System.currentTimeMillis();
                    
                    for (int m=0; m<positions;m++){
                        //// SETS XYZ POSITIONS AND ROTATION
                        
                        if (pl.getNumberOfPositions()>0){
                            MultiStagePosition MSposition = pl.getPosition(m);

                            String StageX=(String) dialog_.XMotorComboBox.getSelectedItem();                     
                            StagePosition sp = MSposition.get(StageX);
                            String TargetX=Double.toString(sp.x);
                            dialog_.MessageTextArea.setText("TARGET "+StageX+ " " +TargetX);   
                            mmc_.setPosition(StageX,sp.x);                
                            String fpos=formatter.format(mmc_.getPosition(StageX));
                            dialog_.XPositionField.setText(fpos);
                            
                            int posC = 0;
                            while (mmc_.getPosition(StageX) != sp.x && posC <= 200){
                                mmc_.sleep(10);
                                posC++;
                            }

                            String StageY=(String) dialog_.YMotorComboBox.getSelectedItem(); 
                            sp = MSposition.get(StageY);   
                            String TargetY=Double.toString(sp.x);
                            dialog_.MessageTextArea.setText("TARGET "+StageY+ " " +TargetY);              
                            mmc_.setPosition(StageY,sp.x);
                            fpos=formatter.format(mmc_.getPosition(StageY));
                            dialog_.YPositionField.setText(fpos);
                            
                            posC = 0;
                            while (mmc_.getPosition(StageY) != sp.x && posC <= 200){
                                mmc_.sleep(10);
                                posC++;
                            }
                        }
                        ///////////////////////////////////
                    try {  ///SETS NEW POSITION
                        double position = mmc_.getPosition(focusdevice); 
                        if (position != ZINI){
                            mmc_.setPosition(focusdevice,ZINI);
                            mmc_.sleep(100);
                            String Position_new=formatter.format(mmc_.getPosition(focusdevice));
                            dialog_.PositionField.setText(Position_new);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                        dialog_.MessageTextArea.setText("ERROR IN MOVING");
                    }
                    gui_.setAcquisitionProperty(Filename, "z-step_um", Double.toString(step));
    
                    //int slices = 0;
                    mmc_.initializeCircularBuffer();
                    mmc_.prepareSequenceAcquisition(cam_name);
                    mmc_.startSequenceAcquisition(slicesG*channels, 0, true);

                    TIni=System.currentTimeMillis();
        //////////// Start Acquisition - camera is ready to receive and send trigrers from Arduino ////////////

//                    String cam = mmc_.getCameraDevice();
//                    mmc_.setProperty(cam, "TRIGGER SOURCE", "EXTERNAL");
//                    mmc_.sleep(100);

//                    mmc_.sleep(100);
//                    mmc_.setProperty(Master, "Command", "START;"); 
                    dialog_.MessageTextArea.setText("Start Acquisition");

                    while (mmc_.getRemainingImageCount() > 0 || mmc_.isSequenceRunning(cam_name)){
                        if(mmc_.getRemainingImageCount() > 0){

                            for(int c=0; c<channels;c++){
                                mmc_.waitForDevice(cam_name);
//                                mmc_.setProperty(cam_name, "INTERNAL LINE INTERVAL", Double.parseDouble(Exposure[c])*1000/(2*2048));
//                                mmc_.setExposure(Double.parseDouble(Exposure[c])/2);
                                mmc_.setExposure(Double.parseDouble(Exposure[c]));
                                mmc_.sleep(Double.parseDouble(Exposure[c]));
                                TaggedImage img = mmc_.popNextTaggedImage();
                                //img.tags.put("z-step_um", step);
                                gui_.addImageToAcquisition(Filename, n, c, slices_, m, img);
                                
                            } 

                            slices_++;
                        }
                        else{
                            mmc_.sleep(Math.min(0.5*exposureMS, 20));
                        }
                    }
                    slices_=0;
                    
                    mmc_.stopSequenceAcquisition(cam_name);
                    
                    
                    
                    try {////GO TO INI POSITION ////
                        mmc_.setPosition(focusdevice,ZINI);
                        mmc_.sleep(100);
                        String Position_new=formatter.format(mmc_.getPosition(focusdevice));
                        dialog_.PositionField.setText(Position_new);
                    } catch (Exception ex) {
                        Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                        dialog_.MessageTextArea.setText("ERROR GOING TO INI POS");
                    }
//                    ShC_.closeshutter(); 
//                    Global.shutter=0; 
                    }

                    TEnd=System.currentTimeMillis();
                    TotalT=TEnd-TIni;
                    /// WAITS FOR NEXT TIMEPOINT ///
                    if (TotalT<Intervalms && n!=(numFrames-1)){gui_.sleep(Intervalms-TotalT);}

                }
                    dialog_.MessageTextArea.setText("Acquisition Finish");

            }      catch (Exception ex) {
                Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
                dialog_.MessageTextArea.setText("Error in Sequence Steps Between");
            }

        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            dialog_.MessageTextArea.setText("Error in SPIM_Trig Mode");
        }
    }
    return mode;
            
};

String MultiPos_Trig(){
    String mode = "MultiPos_Trig";
    String cam_name = mmc_.getCameraDevice();
    acq_.clear();    
    gui_.closeAllAcquisitions();

    PositionList pl=new PositionList();
    
    int slices  = 1;
    int slices_ = 0;
    int galvos  = 1;
    int channelsG = channels * galvos;
    String Step_f = Integer.toString(slices);

    int positions=1;
    
    if(dialog_.GalvoCheckBox1.isSelected() || dialog_.GalvoCheckBox2.isSelected()){
        int galvo1 = dialog_.GalvoCheckBox1.isSelected() ? 1:0;
        int galvo2 = dialog_.GalvoCheckBox2.isSelected() ? 1:0;
        galvos = galvo1+galvo2;
    } 

    String focusdevice=(String) dialog_.StageMotorComboBox.getSelectedItem();
    String xyStage = mmc_.getXYStageDevice();
    mmc_.setAutoShutter(false);

    //////////// This is only to detect the Focus device position property!!!! ///////
    // IF appears in metadata this can be deleted ////
    StrVector devicePropertyNames;  
    String position_property="";
    NumberFormat formatter2 = new DecimalFormat("#.####");
    try {
        devicePropertyNames = mmc_.getDevicePropertyNames(focusdevice);

        for(int x=0;x<devicePropertyNames.size();x++){
            if (!mmc_.isPropertyReadOnly(focusdevice, devicePropertyNames.get(x))){
                String property_value = mmc_.getProperty(focusdevice, devicePropertyNames.get(x));
                String Position=formatter2.format(mmc_.getPosition(focusdevice)).replaceAll(",", ".");
                if (property_value.compareTo(Position)==0){ position_property=devicePropertyNames.get(x);}
                dialog_.MessageTextArea.setText(property_value+"  "+Position + devicePropertyNames.get(x));                    
            }
        }      
    } catch (Exception ex) {
        Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
        dialog_.MessageTextArea.setText("Error in device detection");
    }


/////////////// Mode Configuration  /////////////        
        
    String Mode=(String) dialog_.ModeComboBox.getSelectedItem();    
 
//////////  Sample Rotation Configuration  ///////////   
    String RotPosition=dialog_.AnglePositionField.getText();
    String RotStep=(String)dialog_.RotationComboBox.getSelectedItem();
    String Delay_Stage=dialog_.DelayField.getText();

    double delay_Stage=Double.parseDouble(Delay_Stage);

    if (dialog_.RotationSampleCheckBox.isSelected()){     

        //// Creates number of multiview positions ////
        float RStep=Float.parseFloat(RotStep);
        positions=1;
        if (RStep==0){positions=1;}
        else {positions =(int) ((int) 360/RStep);}        

        //// Shutter configuration ////
        if(dialog_.ShutterRotationCheckBox.isSelected()==true){acq_.keepShutterOpenForChannels(false);}
        else{acq_.keepShutterOpenForChannels(true);}
    } 
 
///////////////  Stack Configuration  //////////////////        
    NumberFormat formatter = new DecimalFormat("#.0000");
    double ZINI=0;
    double ZEND=0;
    double ZSTEP=0.5;
    
    double bottom=Double.parseDouble(dialog_.StartField.getText().replaceAll(",", "."));
    double top=Double.parseDouble(dialog_.EndField.getText().replaceAll(",", "."));
    double step=Double.parseDouble(dialog_.StepField.getText().replaceAll(",", "."));
        
    if ("MultiPos_Trig".equals(Mode)){
        
        boolean absolute=true;
        String ZStep=dialog_.StepField.getText();
        ZSTEP=Double.parseDouble(ZStep.replaceAll(",", "."));
        String ZIni=dialog_.StartField.getText();
        ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        String ZEnd=dialog_.EndField.getText();
        ZEND=Double.parseDouble(ZEnd.replaceAll(",", "."));
        acq_.enableZSliceSetting(false);
        
        //////////Multipositions//////////////////
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

        //// Creates 3D stack postions (TOP,BOTTOM,STEP)////
        if (dialog_.StackCheckBox.isSelected()){
            acq_.enableZSliceSetting(true);
            acq_.setSlices(bottom,top,step,absolute);                                   
            double aux=(top-bottom)/step;
            slices=(int)Math.ceil(aux)+1;
            ZIni=dialog_.StartField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
            ZEnd=dialog_.EndField.getText();
            ZEND=Double.parseDouble(ZEnd.replaceAll(",", "."));
            Step_f = Integer.toString(slices);
        }
        else{
            acq_.enableZSliceSetting(false);
            slices=1;
            ZIni=dialog_.PositionField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        }        
        
        channelsG = channels * galvos; //---------------------------- Slices times number of active galvos ----------------------

        //// Shutter configuration ////
        if(dialog_.ShutterStackCheckBox.isSelected()==true){acq_.keepShutterOpenForStack(false);}
        else{acq_.keepShutterOpenForStack(true);}
    }


////////////////  ACQUISITION IN Multi Position Mode //////////////////                  
    if ("MultiPos_Trig".equals(Mode)){
        try {
            boolean isshown=true; 
            
            if (gui_.isLiveModeOn()){
                gui_.enableLiveMode(false);
            }
            
            String cam = mmc_.getCameraDevice();
            mmc_.setProperty(cam, "TRIGGER SOURCE", "EXTERNAL");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "OUTPUT TRIGGER POLARITY[2]", "POSITIVE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TRIGGER ACTIVE", "EDGE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "SENSOR MODE", "AREA");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TriggerPolarity", "POSITIVE");
            mmc_.sleep(100);
            
            if(gui_.acquisitionExists(Filename)){
                gui_.closeAcquisition(Filename);
            }

            gui_.openAcquisition(Filename, Rootname, numFrames, channelsG, slices, positions, isshown, issaved);

   /////////////////////  RENAME CHANNELS  //////////////////
                
            int[] Excitation_Filters_Index={0,0,0};
            int[] Emission_Filters_Index={0,0,0,0,0,0};
            int[] Intensity_Index = {0,0,0,0,0,0,0,0,0,0,0};
            for(int n=0;n<channels;n++){                     
                gui_.setChannelName(Filename, n, channelname[n]);
                                                  
                for(int m=0;m<3;m++){
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
           
            int wi = (int)mmc_.getImageWidth();
            int he = (int)mmc_.getImageHeight();
            int by = (int)mmc_.getBytesPerPixel();
            int bi = (int)mmc_.getImageBitDepth();

            gui_.initializeAcquisition(Filename, wi, he, by, bi);
            
            

               
/////////// Define channels exposure times, intensities and display color //////////////////
            
            try{        
                if(Global.shutter==0){ShC_.openshutter(); Global.shutter=1;};

                for (int o=0; o<(channels);o++){ /// Channels loop///
                    if (dialog_.Channel_CheckBox.isSelected()){
                        int INT = Integer.valueOf(dialog_.Intensity[Intensity_Index[o]]);
                        if (Excitation_Filters_Index[o] == 0){
                            INT = Intensity_Index[o];
                        };
                        mmc_.sleep(100);
                        FC_.ChangeIntensity(Excitation_Filters_Index[o], INT);
                        mmc_.sleep(100);
                        FC_.ChangeEmissionFilter(Emission_Filters_Index[o]);
                        mmc_.sleep(100);
//                        dialog_.MessageTextArea.setText("Current Emission Filter is: "+CEMI);

                        if(galvos == 2){
                            gui_.setChannelColor(Filename, (o*2), color[o]);
                            mmc_.sleep(1);
                            gui_.setChannelColor(Filename, (o*2)+1, color[o]);
                            mmc_.sleep(1);
                            gui_.setChannelName(Filename, (o*2), channelname[o]+"_IL1");
                            mmc_.sleep(1);
                            gui_.setChannelName(Filename, (o*2)+1, channelname[o]+"_IL2");
                        }
                        else{
                            gui_.setChannelColor(Filename, o, color[o]);
                            gui_.setChannelName(Filename, o, channelname[o]);
                        }

                        try {
                            if(galvos == 2){
                                gui_.setChannelName(Filename, (o*2), channelname[o]+"_IL1");
                                mmc_.sleep(1);
                                gui_.setChannelName(Filename, (o*2)+1, channelname[o]+"_IL2");
                            }
                            else{
                                gui_.setChannelColor(Filename, o, color[o]);
                                gui_.setChannelName(Filename, o, channelname[o]);
                            }
                            mmc_.sleep(100);
                            mmc_.setProperty(Master, "Command", "SEQUENCELASER"+Excitation_Filters_Index[o]+"CAMEXP="+Exposure[o]+";");
                            dialog_.MessageTextArea.setText("EXPOSURE: "+Exposure[o]);
                        } catch (Exception ex) {
                            dialog_.MessageTextArea.setText("ERROR IN SETTING EXPOSURE");
                            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    else{mmc_.sleep(100);
                    
                        gui_.setChannelName(Filename, o, channelname[o]);
                        
                        mmc_.setProperty(Master, "Command", "SEQUENCELASER"+Excitation_Filters_Index[o]+"CAMEXP="+Exposure[o]+";");
                    }
                }
         /////////////// Send information to arduino about number of steps/slices //////////////      
                long TIni;
                long TEnd;
                long TotalT;
                long Intervalms=(long)intervalms;
                
                mmc_.sleep(100);
//                mmc_.setProperty(Master, "Command", "STAGE0ENABLE=1;");
                mmc_.setProperty(Master, "Command", "STAGE0DELAY="+Delay_Stage+";");
                mmc_.sleep(100);
                mmc_.setProperty(Master, "Command", "SEQUENCEMODE=2;");
                mmc_.sleep(100);
                mmc_.setProperty(Master, "Command", "STAGE0TOTALSTEPS="+slices+";");
                mmc_.sleep(100);

                double exposureMS = mmc_.getExposure();
                for (int n=0;n<(numFrames);n++){/// Time lapse loop ///
                    
                    
                    TIni=System.currentTimeMillis();
                    
                    for (int m=0; m<positions;m++){
                        /// SETS XYZ POSITIONS AND ANGLE
                        
                        if (pl.getNumberOfPositions()>0){
                            MultiStagePosition MSposition = pl.getPosition(m);

                            String StageX=(String) dialog_.XMotorComboBox.getSelectedItem();                     
                            StagePosition sp = MSposition.get(StageX); 
                            String TargetX=Double.toString(sp.x);
                            dialog_.MessageTextArea.setText("TARGET "+StageX+ " " +TargetX);   
                            mmc_.setPosition(StageX,sp.x);                
                            String fpos=formatter.format(mmc_.getPosition(StageX));
                            dialog_.XPositionField.setText(fpos);
                            
                            int posC = 0;
                            while (mmc_.getPosition(StageX) != sp.x && posC <= 200){
                                mmc_.sleep(10);
                                posC++;
                            }

                            String StageY=(String) dialog_.YMotorComboBox.getSelectedItem(); 
                            sp = MSposition.get(StageY);   
                            String TargetY=Double.toString(sp.x);
                            dialog_.MessageTextArea.setText("TARGET "+StageY+ " " +TargetY);              
                            mmc_.setPosition(StageY,sp.x);
                            fpos=formatter.format(mmc_.getPosition(StageY));
                            dialog_.YPositionField.setText(fpos);
                            
                            posC = 0;
                            while (mmc_.getPosition(StageY) != sp.x && posC <= 200){
                                mmc_.sleep(10);
                                posC++;
                            }
                            
                            String StageZ=(String) dialog_.StageMotorComboBox.getSelectedItem();
                            sp = MSposition.get(StageZ);   
                            String TargetZ=Double.toString(sp.x);
                            dialog_.MessageTextArea.setText("TARGET "+StageZ+ " " +TargetZ);              
                            mmc_.setPosition(StageZ,sp.x);
                            fpos=formatter.format(mmc_.getPosition(StageZ));
                            dialog_.PositionField.setText(fpos);
                            ZINI = sp.x;
                            
                            posC = 0;
                            while (mmc_.getPosition(StageZ) != sp.x && posC <= 200){
                                mmc_.sleep(10);
                                posC++;
                            }
                        }
                        /////////////////////////////////////
                        try {  ///SETS NEW POSITION
                            double position = mmc_.getPosition(focusdevice); 
                            if (position != ZINI){
                                mmc_.setPosition(focusdevice,ZINI);
                                mmc_.sleep(100);
                                String Position_new=formatter.format(mmc_.getPosition(focusdevice));
                                dialog_.PositionField.setText(Position_new);
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                            dialog_.MessageTextArea.setText("ERROR IN MOVING");
                        }

                        mmc_.initializeCircularBuffer();
                        mmc_.prepareSequenceAcquisition(cam_name);
                        mmc_.startSequenceAcquisition(slices*channelsG, 0, true);
                        
            //////////// Start Acquisition - camera is ready to receive and send trigrers from Arduino ////////////

                        int a = 0;
                        mmc_.sleep(500);
                        mmc_.setProperty(Master, "Command", "START;"); 
                        dialog_.MessageTextArea.setText("Start Acquisition");

                        while (mmc_.getRemainingImageCount() > 0 || mmc_.isSequenceRunning(cam_name)){
                            if(mmc_.getRemainingImageCount() > 0){

                                for(int c=0; c<channelsG;c++){
                                    mmc_.waitForDevice(cam_name);
                                    mmc_.setExposure(Double.parseDouble(Exposure[a]));
                                    mmc_.sleep(Double.parseDouble(Exposure[a]));
                                    TaggedImage img = mmc_.popNextTaggedImage();
                                    gui_.addImageToAcquisition(Filename, n, c, slices_, m, img);
                                    if(c % 2 == 0 && c>2){a++;};
                                } 

                                slices_++;
                                a=0;
                            }
                            else{
                                mmc_.sleep(Math.min(0.5*exposureMS, 20));
                            }
                        }
                        slices_=0;
                        mmc_.stopSequenceAcquisition(cam_name);

                        try {////GO TO INI POSITION ////
                            mmc_.setPosition(focusdevice,ZINI);
                            mmc_.sleep(100);
                            String Position_new=formatter.format(mmc_.getPosition(focusdevice));
                            dialog_.PositionField.setText(Position_new);
                        } catch (Exception ex) {
                            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                            dialog_.MessageTextArea.setText("ERROR GOING TO INI POS");
                        }
                        
                        if (dialog_.RotationSampleCheckBox.isSelected()){
                            String Step;
                            String Position;
                            Step=(String)dialog_.RotationComboBox.getSelectedItem();
                            Position=dialog_.AnglePositionField.getText();
                            Position=RC_.rightMove(Step,Position);
                            dialog_.AnglePositionField.setText(Position);
                        }
                        mmc_.sleep(1000);
                        ShC_.closeshutter(); 
                        Global.shutter=0; 
                    }
                    
//                    ShC_.closeshutter(); 
//                    Global.shutter=0; 

                    TEnd=System.currentTimeMillis();
                    TotalT=TEnd-TIni;
                    /// WAITS FOR NEXT TIMEPOINT ///
                    if (TotalT<Intervalms && n!=(numFrames-1)){gui_.sleep(Intervalms-TotalT);}

                }
            dialog_.MessageTextArea.setText("Acquisition Finish");
             
            mmc_.setProperty(cam, "TRIGGER SOURCE", "INTERNAL");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "OUTPUT TRIGGER POLARITY[2]", "NEGATIVE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TRIGGER ACTIVE", "EDGE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "SENSOR MODE", "AREA");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TriggerPolarity", "NEGATIVE");
            mmc_.sleep(100);    

            }      catch (Exception ex) {
                Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
                dialog_.MessageTextArea.setText("Error in Sequence Steps Between");
            }

        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            dialog_.MessageTextArea.setText("Error in SPIM_Trig Mode");
        }
    }
    return mode;
            
};

String Confocal_Trig(){
    String mode = "Confocal_Trig";
    String cam_name = mmc_.getCameraDevice();
    acq_.clear();    
    gui_.closeAllAcquisitions();

    PositionList pl=new PositionList();
    
    int slices  = 1;
    int slices_ = 0;
    int galvos  = 1;
    int channelsG = channels * galvos;
    String Step_f = Integer.toString(slices);

    int positions=1;
    
    if(dialog_.GalvoCheckBox1.isSelected() || dialog_.GalvoCheckBox2.isSelected()){
        int galvo1 = dialog_.GalvoCheckBox1.isSelected() ? 1:0;
        int galvo2 = dialog_.GalvoCheckBox2.isSelected() ? 1:0;
        galvos = galvo1+galvo2;
    } 

    String focusdevice=(String) dialog_.StageMotorComboBox.getSelectedItem();
    String xyStage = mmc_.getXYStageDevice();
    mmc_.setAutoShutter(false);

    //////////// This is only to detect the Focus device position property!!!! ///////
    // IF appears in metadata this can be deleted ////
    StrVector devicePropertyNames;  
    String position_property="";
    NumberFormat formatter2 = new DecimalFormat("#.####");
    try {
        devicePropertyNames = mmc_.getDevicePropertyNames(focusdevice);

        for(int x=0;x<devicePropertyNames.size();x++){
            if (!mmc_.isPropertyReadOnly(focusdevice, devicePropertyNames.get(x))){
                String property_value = mmc_.getProperty(focusdevice, devicePropertyNames.get(x));
                String Position=formatter2.format(mmc_.getPosition(focusdevice)).replaceAll(",", ".");
                if (property_value.compareTo(Position)==0){ position_property=devicePropertyNames.get(x);}
                dialog_.MessageTextArea.setText(property_value+"  "+Position + devicePropertyNames.get(x));                    
            }
        }      
    } catch (Exception ex) {
        Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
        dialog_.MessageTextArea.setText("Error in device detection");
    }


/////////////// Mode Configuration  /////////////        
        
    String Mode=(String) dialog_.ModeComboBox.getSelectedItem();    
 
//////////  Sample Rotation Configuration  ///////////   
    String RotPosition=dialog_.AnglePositionField.getText();
    String RotStep=(String)dialog_.RotationComboBox.getSelectedItem();
    String Delay_Stage=dialog_.DelayField.getText();

    double delay_Stage=Double.parseDouble(Delay_Stage);

    if (dialog_.RotationSampleCheckBox.isSelected()){     

        //// Creates number of multiview positions ////
        float RStep=Float.parseFloat(RotStep);
        positions=1;
        if (RStep==0){positions=1;}
        else {positions =(int) ((int) 360/RStep);}        

        //// Shutter configuration ////
        if(dialog_.ShutterRotationCheckBox.isSelected()==true){acq_.keepShutterOpenForChannels(false);}
        else{acq_.keepShutterOpenForChannels(true);}
    } 
 
///////////////  Stack Configuration  //////////////////        
    NumberFormat formatter = new DecimalFormat("#.0000");
    double ZINI=0;
    double ZEND=0;
    double ZSTEP=0.5;
    
    double bottom=Double.parseDouble(dialog_.StartField.getText().replaceAll(",", "."));
    double top=Double.parseDouble(dialog_.EndField.getText().replaceAll(",", "."));
    double step=Double.parseDouble(dialog_.StepField.getText().replaceAll(",", "."));
        
    if ("Confocal_Trig".equals(Mode)){
        
        boolean absolute=true;
        String ZStep=dialog_.StepField.getText();
        ZSTEP=Double.parseDouble(ZStep.replaceAll(",", "."));
        String ZIni=dialog_.StartField.getText();
        ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        String ZEnd=dialog_.EndField.getText();
        ZEND=Double.parseDouble(ZEnd.replaceAll(",", "."));
        acq_.enableZSliceSetting(false);
        
        //////////Multipositions//////////////////
        try {
            pl = gui_.getPositionList();
            gui_.setPositionList(pl);
            if(pl.getNumberOfPositions()>1){
                positions=pl.getNumberOfPositions(); 
            }
            String posString=Integer.toString(positions);
            dialog_.MessageTextArea.setText("Number of positions: "+posString);
        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            dialog_.MessageTextArea.setText("ERROR IN POSITION LIST");
        }

        //// Creates 3D stack postions (TOP,BOTTOM,STEP)////
        if (dialog_.StackCheckBox.isSelected()){
            acq_.enableZSliceSetting(true);
            acq_.setSlices(bottom,top,step,absolute);                                   
            double aux=(top-bottom)/step;
            slices=(int)Math.ceil(aux)+1;
            ZIni=dialog_.StartField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
            ZEnd=dialog_.EndField.getText();
            ZEND=Double.parseDouble(ZEnd.replaceAll(",", "."));
            Step_f = Integer.toString(slices);
        }
        else{
            acq_.enableZSliceSetting(false);
            slices=1;
            ZIni=dialog_.PositionField.getText();
            ZINI=Double.parseDouble(ZIni.replaceAll(",", "."));
        }           
        
        channelsG = channels * galvos; //---------------------------- Slices times number of active galvos ----------------------

        //// Shutter configuration ////
        if(dialog_.ShutterStackCheckBox.isSelected()==true){acq_.keepShutterOpenForStack(false);}
        else{acq_.keepShutterOpenForStack(true);}
    }


////////////////  ACQUISITION IN Confocal Mode //////////////////                  
    if ("Confocal_Trig".equals(Mode)){
        try {
            boolean isshown=true; 
            
            if (gui_.isLiveModeOn()){
                gui_.enableLiveMode(false);
            }
            
            String cam = mmc_.getCameraDevice();
            mmc_.setProperty(cam, "TRIGGER SOURCE", "EXTERNAL");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "OUTPUT TRIGGER POLARITY[2]", "POSITIVE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TRIGGER ACTIVE", "LEVEL");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "SENSOR MODE", "PROGRESSIVE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TriggerPolarity", "POSITIVE");
            mmc_.sleep(100);
            
            if(gui_.acquisitionExists(Filename)){
                gui_.closeAcquisition(Filename);
            }

            gui_.openAcquisition(Filename, Rootname, numFrames, channelsG, slices, positions, isshown, issaved);

   /////////////////////  RENAME CHANNELS  //////////////////
                
            int[] Excitation_Filters_Index={0,0,0};
            int[] Emission_Filters_Index={0,0,0,0,0,0};
            int[] Intensity_Index = {0,0,0,0,0,0,0,0,0,0,0};
            for(int n=0;n<channels;n++){                     
                gui_.setChannelName(Filename, n, channelname[n]);
                                                  
                for(int m=0;m<3;m++){
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
           
            int wi = (int)mmc_.getImageWidth();
            int he = (int)mmc_.getImageHeight();
            int by = (int)mmc_.getBytesPerPixel();
            int bi = (int)mmc_.getImageBitDepth();

            gui_.initializeAcquisition(Filename, wi, he, by, bi);
                     
/////////// Define channels exposure times, intensities and display color //////////////////
            
            try{        
                if(Global.shutter==0){ShC_.openshutter(); Global.shutter=1;};

                for (int o=0; o<(channels);o++){ /// Channels loop///
                    if (dialog_.Channel_CheckBox.isSelected()){
                        int INT = Integer.valueOf(dialog_.Intensity[Intensity_Index[o]]);
                        if (Excitation_Filters_Index[o] == 0){
                            INT = Intensity_Index[o];
                        };
                        mmc_.sleep(100);
                        FC_.ChangeIntensity(Excitation_Filters_Index[o], INT);
                        mmc_.sleep(100);
                        FC_.ChangeEmissionFilter(Emission_Filters_Index[o]);
                        mmc_.sleep(100);

                        if(galvos == 2){
                            gui_.setChannelColor(Filename, (o*2), color[o]);
                            mmc_.sleep(1);
                            gui_.setChannelColor(Filename, (o*2)+1, color[o]);
                            mmc_.sleep(1);
                            gui_.setChannelName(Filename, (o*2), channelname[o]+"_IL1");
                            mmc_.sleep(1);
                            gui_.setChannelName(Filename, (o*2)+1, channelname[o]+"_IL2");
                        }
                        else{
                            gui_.setChannelColor(Filename, o, color[o]);
                            gui_.setChannelName(Filename, o, channelname[o]);
                        }

                        try {
                            if(galvos == 2){
                                gui_.setChannelName(Filename, (o*2), channelname[o]+"_IL1");
                                mmc_.sleep(1);
                                gui_.setChannelName(Filename, (o*2)+1, channelname[o]+"_IL2");
                            }
                            else{
                                gui_.setChannelColor(Filename, o, color[o]);
                                gui_.setChannelName(Filename, o, channelname[o]);
                            }
                            //mmc_.setProperty(cam_name, "INTERNAL LINE INTERVAL", Double.parseDouble(Exposure[o])*1000/(2*2048));
                            mmc_.sleep(100);
                            mmc_.setProperty(Master, "Command", "SEQUENCELASER"+Excitation_Filters_Index[o]+"CAMEXP="+Exposure[o]+";");
                            dialog_.MessageTextArea.setText("EXPOSURE: "+Exposure[o]);
                        } catch (Exception ex) {
                            dialog_.MessageTextArea.setText("ERROR IN SETTING EXPOSURE");
                            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    else{
                        mmc_.sleep(100);
                        gui_.setChannelName(Filename, o, channelname[o]);
                        int expConf = Math.round(Float.parseFloat(Exposure[o])*2);
                        mmc_.setProperty(Master, "Command", "SEQUENCELASER"+Excitation_Filters_Index[o]+"CAMEXP="+expConf+";");
                    }
                }
         /////////////// Send information to arduino about number of steps/slices //////////////      
                long TIni;
                long TEnd;
                long TotalT;
                long Intervalms=(long)intervalms;
                
                mmc_.sleep(100);
                mmc_.setProperty(Master, "Command", "STAGE0DELAY="+Delay_Stage+";");
                mmc_.sleep(100);
                mmc_.setProperty(Master, "Command", "SEQUENCEMODE=2;");
                mmc_.sleep(100);
                mmc_.setProperty(Master, "Command", "STAGE0TOTALSTEPS="+slices+";");
                mmc_.sleep(100);
                
                if (dialog_.Channel_CheckBox.isSelected()){
                    mmc_.setProperty(cam_name, "INTERNAL LINE INTERVAL", Double.parseDouble(Exposure[0])*1000/(2*2048));
                    mmc_.sleep(100);
                    mmc_.setExposure(Double.parseDouble(Exposure[0])/2);
                }

                double exposureMS = mmc_.getExposure();
                for (int n=0;n<(numFrames);n++){/// Time lapse loop ///
                    
                    TIni=System.currentTimeMillis();
                    
                    for (int m=0; m<positions;m++){
                        /// SETS XY POSITIONS ON MULTIWHEEL PLATE 
                        
                        if (pl.getNumberOfPositions()>0){
                            MultiStagePosition MSposition = pl.getPosition(m);

                            String StageX=(String) dialog_.XMotorComboBox.getSelectedItem();                     
                            StagePosition sp = MSposition.get(StageX); 
                            String TargetX=Double.toString(sp.x);
                            dialog_.MessageTextArea.setText("TARGET "+StageX+ " " +TargetX);   
                            mmc_.setPosition(StageX,sp.x);                
                            String fpos=formatter.format(mmc_.getPosition(StageX));
                            dialog_.XPositionField.setText(fpos);
                            
                            int posC = 0;
                            while (mmc_.getPosition(StageX) != sp.x && posC <= 200){
                                mmc_.sleep(10);
                                posC++;
                            }

                            String StageY=(String) dialog_.YMotorComboBox.getSelectedItem(); 
                            sp = MSposition.get(StageY);   
                            String TargetY=Double.toString(sp.x);
                            dialog_.MessageTextArea.setText("TARGET "+StageY+ " " +TargetY);              
                            mmc_.setPosition(StageY,sp.x);
                            fpos=formatter.format(mmc_.getPosition(StageY));
                            dialog_.YPositionField.setText(fpos);
                            
                            posC = 0;
                            while (mmc_.getPosition(StageY) != sp.x && posC <= 200){
                                mmc_.sleep(10);
                                posC++;
                            }
                            
                            String StageZ=(String) dialog_.StageMotorComboBox.getSelectedItem();
                            sp = MSposition.get(StageZ);   
                            String TargetZ=Double.toString(sp.x);
                            dialog_.MessageTextArea.setText("TARGET "+StageZ+ " " +TargetZ);              
                            mmc_.setPosition(StageZ,sp.x);
                            fpos=formatter.format(mmc_.getPosition(StageZ));
                            dialog_.PositionField.setText(fpos);
                            ZINI = sp.x;
                            
                            posC = 0;
                            while (mmc_.getPosition(StageZ) != sp.x && posC <= 200){
                                mmc_.sleep(10);
                                posC++;
                            }
                        }
                        /////////////////////////////////////
                        try {  ///SETS NEW POSITION
                            double position = mmc_.getPosition(focusdevice); 
                            if (position != ZINI){
                                mmc_.setPosition(focusdevice,ZINI);
                                mmc_.sleep(100);
                                String Position_new=formatter.format(mmc_.getPosition(focusdevice));
                                dialog_.PositionField.setText(Position_new);
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                            dialog_.MessageTextArea.setText("ERROR IN MOVING");
                        }
                        
                        gui_.setAcquisitionProperty(Filename, "z-step_um", Double.toString(step));                       

                        mmc_.sleep(1000);
                        mmc_.initializeCircularBuffer();
                        mmc_.prepareSequenceAcquisition(cam_name);
                        mmc_.startSequenceAcquisition(slices*channelsG, 0, true);
                        
            //////////// Start Acquisition - camera is ready to receive and send trigrers from Arduino ////////////

    //                    String cam = mmc_.getCameraDevice();
    //                    mmc_.setProperty(cam, "TRIGGER SOURCE", "EXTERNAL");
    //                    mmc_.sleep(100);

                        int a = 0;
                        mmc_.sleep(500);
                        mmc_.setProperty(Master, "Command", "START;"); 
                        dialog_.MessageTextArea.setText("Start Acquisition");

                        while (mmc_.getRemainingImageCount() > 0 || mmc_.isSequenceRunning(cam_name)){
                            if(mmc_.getRemainingImageCount() > 0){

                                for(int c=0; c<channelsG;c++){
                                    mmc_.waitForDevice(cam_name);
    //                              mmc_.setExposure(Double.parseDouble(Exposure[c]));
                                    
                                    mmc_.sleep(Double.parseDouble(Exposure[0]));
                                    TaggedImage img = mmc_.popNextTaggedImage();
                                    gui_.addImageToAcquisition(Filename, n, c, slices_, m, img);
                                    //if(c % 2 == 0){a++;};
                                } 

                                slices_++;
                            }
                            else{
                                mmc_.sleep(Math.min(0.5*exposureMS, 20));
                            }
                        }
                        slices_=0;
                        mmc_.stopSequenceAcquisition(cam_name);

                        try {////GO TO INI POSITION ////
                            mmc_.setPosition(focusdevice,ZINI);
                            mmc_.sleep(100);
                            String Position_new=formatter.format(mmc_.getPosition(focusdevice));
                            dialog_.PositionField.setText(Position_new);
                        } catch (Exception ex) {
                            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                            dialog_.MessageTextArea.setText("ERROR GOING TO INI POS");
                        }
                        
                        if (dialog_.RotationSampleCheckBox.isSelected()){
                            String Step;
                            String Position;
                            Step=(String)dialog_.RotationComboBox.getSelectedItem();
                            Position=dialog_.AnglePositionField.getText();
                            Position=RC_.rightMove(Step,Position);
                            dialog_.AnglePositionField.setText(Position);
                        }
                        
                        mmc_.sleep(1000);
                        ShC_.closeshutter(); 
                        Global.shutter=0; 
                    }
                    
//                    ShC_.closeshutter(); 
//                    Global.shutter=0; 

                    TEnd=System.currentTimeMillis();
                    TotalT=TEnd-TIni;
                    /// WAITS FOR NEXT TIMEPOINT ///
                    if (TotalT<Intervalms && n!=(numFrames-1)){gui_.sleep(Intervalms-TotalT);}

                }
            dialog_.MessageTextArea.setText("Acquisition Finish");
                                               
            mmc_.setProperty(cam, "TRIGGER SOURCE", "INTERNAL");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "OUTPUT TRIGGER POLARITY[2]", "NEGATIVE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TRIGGER ACTIVE", "EDGE");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "SENSOR MODE", "AREA");
            mmc_.sleep(100);
            mmc_.setProperty(cam, "TriggerPolarity", "NEGATIVE");
            mmc_.sleep(100);

            }      catch (Exception ex) {
                Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
                dialog_.MessageTextArea.setText("Error in Sequence Steps Between");
            }

        } catch (Exception ex) {
            Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
            dialog_.MessageTextArea.setText("Error in Confocal_Trig Mode");
        }
    }
    return mode;
            
};
   
}
