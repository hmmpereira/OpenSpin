/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SPIM_FLUID;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.CMMCore;


/**
 *
 * @author Gualda
 */
public class RotationControl2 {
    private CMMCore core_;
    private String StateDev_;
    private JDialFinal dialog_;
    private String Master = Global.Master;

    public RotationControl2(CMMCore core, String StateDev, JDialFinal dialog){
    core_ = core;
    StateDev_=StateDev;
    dialog_=dialog;
    }
    String leftMove(String Step, String PositionIni) {
    String Rotationdevice=(String) dialog_.RotationMotorComboBox.getSelectedItem();
    float STEPType = 1;
    int n = 0;
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
        n = 0;
    }
    else if ("Motor_2".equalsIgnoreCase(Rotationdevice)){
        n = 1;
    }
    else if ("Motor_3".equalsIgnoreCase(Rotationdevice)){
        n = 2;
    }
    else if ("Motor_4".equalsIgnoreCase(Rotationdevice)){
        n = 3;
    }

    //float STEPS = Float.parseFloat((String)dialog_.StepsComboBox1.getSelectedItem());
    if("0".equalsIgnoreCase(StepT[n])){
        STEPType = 1;
    }
    else if("1".equalsIgnoreCase(StepT[n])){
        STEPType = 2;
    }
    else if("2".equalsIgnoreCase(StepT[n])){
        STEPType = 4;
    }
    else if("3".equalsIgnoreCase(StepT[n])){
        STEPType = 8;
    }
    
    int Step_ = Math.round((Float.parseFloat(Step)*STEPS[n]*STEPType)/360);
    String Step_f = Integer.toString(Step_);
   
    NumberFormat formatter = new DecimalFormat("#.0");
        try{
           if (Rotationdevice.equals("Motor_1") || Rotationdevice.equals("Motor_2") || Rotationdevice.equals("Motor_3") || Rotationdevice.equals("Motor_4")){
               
               core_.setProperty(Master, "Command", "MOTOR"+n+"MOVESTEPS=-"+Step_f+";");
               
            }
            else{
               dialog_.MessageTextArea.setText("Please Select a Stepper Motor (i.e. Motor 3)");
               /*
                try {
                    ////
                    double position = core_.getPosition(Rotationdevice);
                    double STEP=Double.parseDouble(Step);
                    double position_new=position-STEP;
                    core_.setPosition(Rotationdevice,position_new);
                    String Position_new=formatter.format(core_.getPosition(Rotationdevice));  
                } catch (Exception ex) {
                    Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                }*/
            }
           
        float STEP=Float.parseFloat(Step);
        float aux2=Float.parseFloat(PositionIni);
        aux2=aux2+STEP;                             
        if (aux2>=360){aux2=aux2-360;}
        String PositionFinal=formatter.format(aux2).replaceAll(",", ".");//Float.toString(aux2);

        return PositionFinal;       
        }
        catch (Exception ex) {
            Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            dialog_.MessageTextArea.setText("Error in Left Rotation");
            return("ERROR");
        }             
    }
    
    String rightMove(String Step, String PositionIni) {
      
    NumberFormat formatter = new DecimalFormat("#.0");
    String Rotationdevice=(String) dialog_.RotationMotorComboBox.getSelectedItem();
            
    float STEPType = 1;
    
    int n = 0;
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
        n = 0;
    }
    else if ("Motor_2".equalsIgnoreCase(Rotationdevice)){
        n = 1;
    }
    else if ("Motor_3".equalsIgnoreCase(Rotationdevice)){
        n = 2;
    }
    else if ("Motor_4".equalsIgnoreCase(Rotationdevice)){
        n = 3;
    }

    //float STEPS = Float.parseFloat((String)dialog_.StepsComboBox1.getSelectedItem());
    if("0".equalsIgnoreCase(StepT[n])){
        STEPType = 1;
    }
    else if("1".equalsIgnoreCase(StepT[n])){
        STEPType = 2;
    }
    else if("2".equalsIgnoreCase(StepT[n])){
        STEPType = 4;
    }
    else if("3".equalsIgnoreCase(StepT[n])){
        STEPType = 8;
    }
    
    int Step_ = Math.round((Float.parseFloat(Step)*STEPS[n]*STEPType)/360);
    String Step_f = Integer.toString(Step_);

    try{
        if (Rotationdevice.equals("Motor_1") || Rotationdevice.equals("Motor_2") || Rotationdevice.equals("Motor_3") || Rotationdevice.equals("Motor_4")){
            core_.setProperty(Master, "Command", "MOTOR"+n+"MOVESTEPS="+Step_f+";");
        }
        else{
            dialog_.MessageTextArea.setText("Please Select a Stepper Motor (i.e. Motor 3)");
            /*try {
                ////               
                double position = core_.getPosition(Rotationdevice);
                double STEP=Double.parseDouble(Step);
                double position_new=position+STEP;
                core_.setPosition(Rotationdevice,position_new);
                String Position_new=formatter.format(core_.getPosition(Rotationdevice));  
            } catch (Exception ex) {
                Logger.getLogger(JDialFinal.class.getName()).log(Level.SEVERE, null, ex);
                dialog_.MessageTextArea.setText("Error in Right Rotation");
            }*/
        } 

        float STEP=Float.parseFloat(Step);
        float aux2=Float.parseFloat(PositionIni);
        aux2=aux2-STEP;
        if (aux2<0){aux2=aux2+360;} 
        String PositionFinal=formatter.format(aux2).replaceAll(",", ".");
        return PositionFinal;
    }catch (Exception ex) {
        Logger.getLogger(SPIMIIcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
        dialog_.MessageTextArea.setText("Error in Right Rotation");
        return("ERROR");
    }
}

}
    
  