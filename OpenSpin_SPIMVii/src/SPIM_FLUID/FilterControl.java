/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SPIM_FLUID;

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
    private String Master = Global.Master;
    private JDialFinal dialog_;
    
    public FilterControl(CMMCore core, String StateDev_FW, String ShutterDev_FW){
        mmc_ = core;
        StateDev_FW_=StateDev_FW;
        ShutterDev_FW_=ShutterDev_FW;
    }
    
    //This is method is not used on the current set up
    String ChangeExcitationFilter(int Current_Item){
        int Move_Item               = Current_Item;
        String CI                   = Integer.toString(Current_Item);        
        Global.Exci_Previous_Item   = Current_Item;
        try{
          for(int n = 0; n<3; n++){
            if(n==Move_Item){
                mmc_.setProperty(Master, "Command", "LASER"+CI+"STATE=1;");
            }
            else{
                String nstring = Integer.toString(n);
                mmc_.setProperty(Master, "Command", "LASER"+nstring+"STATE=0;");
            }
        }
        return CI;
      }
      catch (Exception ex) {          
            Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
            return("Error in Excitation Filter: "+Integer.toString(Move_Item));
        }
    }
    String ChangeEmissionFilter(int Current_Item){
        int Move_Item               = Current_Item;
        int emiss_M                 = Global.emissM;     
        int previous_item           = Global.Emi_Previous_Item;
        Global.Emi_Previous_Item    = Move_Item;
        try{
            mmc_.setProperty(Master, "Command", "FILTER"+emiss_M+"ENABLE=1;");
            mmc_.sleep(100);
            if (Move_Item != previous_item){
                mmc_.setProperty(Master, "Command", "FILTER"+emiss_M+"MOVETO="+Current_Item+";");
            }       
        return "";
        }   
        catch (Exception ex) {
            Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
            return("Error in Emision Filter: "+dialog_.Emission_filters[Current_Item]);
        }
    }
       
    String ChangeExcitationLaser(int Current_Item){
        int Move_Item             = Current_Item;
        String CI                 = Integer.toString(Current_Item);
        int previousItem          = Global.Exci_Previous_Item;
        Global.Exci_Previous_Item = Current_Item;
        try{
            for(int n = 0; n<3; n++){
                if(n==Move_Item){
                    mmc_.setProperty(Master, "Command", "LASER"+CI+"STATE=1;");//Turn on the selected laser at Excitation combo box
                    mmc_.sleep(100);
                }
                else{
                    String nstring = Integer.toString(n);
                    mmc_.setProperty(Master, "Command", "LASER"+nstring+"STATE=0;");//Turn off the unselected lasers
                    mmc_.sleep(100);
                }
            }
        return CI;
        }catch (Exception ex) {
          Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
          return("Error in Excitation Laser: "+ dialog_.Excitation_filters[Current_Item]);
        }
    }
    
    String ChangeIntensity(int Current_Ex, int Current_Int){
        String CI   = Integer.toString(Current_Ex);
        int C_Int   = Current_Int;
        String INT  = Integer.toString(Current_Int);
        int inten_M = Global.intenM;

        switch (Current_Ex) { // check which laser is selected - intensity control is different for each laser
            case 0: 
                if (C_Int > 4){
                    C_Int = C_Int-5;
                }
                else{
                    C_Int = 0;
                }
                int previous_int = Global.Int_Previous_Item;
                Global.Int_Previous_Item = C_Int;
                try{
                    mmc_.setProperty(Master, "Command", "FILTER"+inten_M+"ENABLE=1;");
                    mmc_.sleep(100);

                    if (C_Int == previous_int){

                    }
                    else{
                        mmc_.setProperty(Master, "Command", "FILTER"+inten_M+"MOVETO="+C_Int+";");
                    }
                return CI;

                }catch (Exception ex) {
                    Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
                    return("Error in Intensity Filter: "+C_Int);
                }
            case 1:
                int cint = (int) (C_Int * 0.5);
                String cstr = Integer.toString(cint);
                try{
                    mmc_.setProperty("Laser_561", "PowerSetpoint", cstr);
                } catch (Exception ex) {
                    Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
                    return("Error in Intensity Filter: "+C_Int);
                } break;
            case 2:
                try{
                    mmc_.setProperty("Laser_633", "Laser Power Set-point Select [%]", C_Int);
                } catch (Exception ex) {
                    Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
                }     break;
            default:
                break;
        }
    return INT;
  }
}
