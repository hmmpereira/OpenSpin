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
 * @author pcadmin
 */
public class ShutterControl {
    private CMMCore mmc_;
    private String StateDev_;
    private String ShutterDev_;
    private JDialFinal dialog_;
    private String Master = Global.Master;
    private int lasers = 3;
    public ShutterControl(CMMCore core, String StateDev, String ShutterDev, JDialFinal dialog){
        mmc_ = core;
        StateDev_=StateDev;
        ShutterDev_=ShutterDev;
        dialog_ = dialog;
    }
    void openshutter(){
        try {
            if(!dialog_.LaserCheckBox1.isSelected() && !dialog_.LaserCheckBox2.isSelected() && !dialog_.LaserCheckBox3.isSelected() && !dialog_.LaserCheckBox4.isSelected()){
                dialog_.MessageTextArea.setText("No LED Enable");
            }
            else{
                int Move_Item = dialog_.Excitation_ComboBox.getSelectedIndex();
                String CI = (String)Integer.toString(Move_Item);

                for(int n = 0; n<lasers; n++){
                    if(n==Move_Item){
                        mmc_.setProperty(Master, "Command", "LASER"+CI+"STATE=1;");
                    }
                    else{
                        String nstring = Integer.toString(n);
                        mmc_.setProperty(Master, "Command", "LASER"+nstring+"STATE=0;");
                    }
                }
                Global.shutter=1;
                dialog_.MessageTextArea.setText("Laser On");
            }
        } catch (Exception ex) {
            Logger.getLogger(ShutterControl.class.getName()).log(Level.SEVERE, null, ex);
            dialog_.MessageTextArea.setText("ERROR IN Turn On Laser");
        }
			
    }
    
    void closeshutter(){
        try {
            if(!dialog_.LaserCheckBox1.isSelected() && !dialog_.LaserCheckBox2.isSelected() && !dialog_.LaserCheckBox3.isSelected() && !dialog_.LaserCheckBox4.isSelected()){
                dialog_.MessageTextArea.setText("No LED Enable");
            }
            else{
                for(int n = 0; n<lasers; n++){
                    String nstring = Integer.toString(n);
                    mmc_.setProperty(Master, "Command", "LASER"+nstring+"STATE=0;");
                }
                Global.shutter=0; 
                dialog_.MessageTextArea.setText("Lasers Off");
            }
        } catch (Exception ex) {
            Logger.getLogger(ShutterControl.class.getName()).log(Level.SEVERE, null, ex);
            dialog_.MessageTextArea.setText("ERROR IN Turn Off laser");
        }

    }
}
