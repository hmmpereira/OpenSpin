/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


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
    private String StateDev_FW_;
    private String ShutterDev_FW_;
    private FilterControl FC_;
    private JDialFinal dialog_;
    public ShutterControl(CMMCore core, String StateDev, String ShutterDev, JDialFinal dialog){
        mmc_ = core;
        StateDev_=StateDev;
        ShutterDev_=ShutterDev;
        FC_ = new FilterControl(mmc_, StateDev_FW_, ShutterDev_FW_, ShutterDev_);
        dialog_ = dialog;
    }
    void openshutter(){
        try {
            if(!dialog_.LedCheckBox1.isSelected() && !dialog_.LedCheckBox2.isSelected() && !dialog_.LedCheckBox3.isSelected() && !dialog_.LedCheckBox4.isSelected()){
                dialog_.MessageTextArea.setText("No LED Enable");
            }
            else{
                int Current_Item=dialog_.Excitation_ComboBox.getSelectedIndex();
                String INT = dialog_.Intensity_ComboBox.getItemAt(dialog_.Intensity_ComboBox.getSelectedIndex());

                FC_.ChangeIntensity(Integer.toString(Current_Item), INT);
                FC_.ChangeExcitationFilter(Current_Item);
                dialog_.MessageTextArea.setText("LED On");
            }
        } catch (Exception ex) {
            Logger.getLogger(ShutterControl.class.getName()).log(Level.SEVERE, null, ex);
            dialog_.MessageTextArea.setText("Error in Open Shutter");
        }
			
            }
    
        void closeshutter(){
        try {

            if(dialog_.LedCheckBox1.isSelected()){
                mmc_.setProperty("SRotation", "Command", "LED0STATE=0;");
                dialog_.MessageTextArea.setText("LED Off");
            }
            if(dialog_.LedCheckBox2.isSelected()){
                mmc_.setProperty("SRotation", "Command", "LED1STATE=0;");
                dialog_.MessageTextArea.setText("LED Off");
            }
            if(dialog_.LedCheckBox3.isSelected()){
                mmc_.setProperty("SRotation", "Command", "LED2STATE=0;");
                dialog_.MessageTextArea.setText("LED Off");
            }
            if(dialog_.LedCheckBox4.isSelected()){
                mmc_.setProperty("SRotation", "Command", "LED3STATE=0;");
                dialog_.MessageTextArea.setText("LED Off");
            }
            if(!dialog_.LedCheckBox1.isSelected() && !dialog_.LedCheckBox2.isSelected() && !dialog_.LedCheckBox3.isSelected() && !dialog_.LedCheckBox4.isSelected()){
                dialog_.MessageTextArea.setText("No LED Enable");
            }

        } catch (Exception ex) {
            Logger.getLogger(ShutterControl.class.getName()).log(Level.SEVERE, null, ex);
            dialog_.MessageTextArea.setText("Error in Close Shutter");
        }
			
            }
}
