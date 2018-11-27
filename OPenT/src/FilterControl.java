/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.CMMCore;

/**
 *
 * @author User
 */
public class FilterControl {
          private CMMCore mmc_;
          private String StateDev_FW_;
          private String ShutterDev_FW_;
          public ShutterControl ShC_;
          public String ShutterDev_; 
 
    public FilterControl(CMMCore core, String StateDev_FW, String ShutterDev_FW, String ShutterDev){
    mmc_ = core;
    StateDev_FW_=StateDev_FW;
    ShutterDev_FW_=ShutterDev_FW;
    ShutterDev_=ShutterDev;
    }
    
    String ChangeExcitationFilter(int Current_Item){
          int Move_Item=Current_Item;

          String CI=Integer.toString(Current_Item);        
          Global.Exci_Previous_Item=Current_Item;

          try{
            for(int n = 0; n<4; n++){
                if(n==Move_Item){
                    mmc_.setProperty("SRotation", "Command", "LED"+CI+"STATE=1;");
                }
                else{
                    mmc_.setProperty("SRotation", "Command", "LED"+n+"STATE=0;");
                }
            }
            Global.shutter = 1; 
            return CI;
      }
          
      catch (Exception ex) {
            Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
            return("Error in Excitation Filter"+Integer.toString(Move_Item));
                    }
    }
       String ChangeEmissionFilter(int Current_Item){
          int Move_Item=Current_Item;//-Global.Emi_Previous_Item;  
          //if (Move_Item<0) {Move_Item=Move_Item+6;}
          String CI=Integer.toString(Current_Item);        
          Global.Emi_Previous_Item=Current_Item;
          try{
            if (Move_Item==0) {
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","0");
                        mmc_.setProperty(StateDev_FW_,"State","14");                   
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
             if (Move_Item==1) {
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","0");
                        mmc_.setProperty(StateDev_FW_,"State","32");                   
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
              if (Move_Item==2) {
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","0");
                        mmc_.setProperty(StateDev_FW_,"State","18");                   
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
              if (Move_Item==3) {
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","0");
                        mmc_.setProperty(StateDev_FW_,"State","20");                   
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
            if (Move_Item==4) {
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","0");
                        mmc_.setProperty(StateDev_FW_,"State","22");                   
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","1");		
            } 
            if (Move_Item==5) {
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","0");
                        mmc_.setProperty(StateDev_FW_,"State","24");                   
                        mmc_.setProperty(ShutterDev_FW_,"OnOff","1");		
            } 

           return CI;
      }
          
      catch (Exception ex) {
                        Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
                        return("Error in Emision Filter"+Integer.toString(Move_Item));
                    }
    }
    
        String ChangeIntensity(String Current_Ex, String Current_Int){
            String CI = Current_Ex;
            String INT = Current_Int;//(String)(dialog_.Intensity_ComboBox.getSelectedItem());
            try{
            mmc_.sleep(100);
            mmc_.setProperty("SRotation", "Command", "LED"+CI+"INTENSITY="+INT+";");
            mmc_.sleep(100);
            mmc_.setProperty("SRotation", "Command", "LED"+CI+"STATE=1;");

            } catch (Exception ex) {
                        Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
                        return("Error in Excitation Filter"+INT);
                    } 
            Global.shutter = 1;
            return INT;
      }
}
