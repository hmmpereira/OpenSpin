/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


package SPIM_FLUID;


import mmcorej.CMMCore;
import org.micromanager.MMStudio;
import org.micromanager.api.ScriptInterface;
import org.micromanager.acquisition.AcquisitionEngine;


/**
 *
 * @author Gualda
 */


public class SPIMIIcontrolsPlugin implements org.micromanager.api.MMPlugin{

    public static String menuName = "SPIMvII";
    public static String versionNumber = "2.180807";
    private MMStudio gui_;
    private JDialFinal dialog_;
    private AcquisitionEngine acq_;
    private CMMCore mmc_;
 
    
    public String getDescription() {
        return "The SPIMVII Control plugin allows the user to control the double side illumination LSFM microscope";
    }

    public String getInfo() {
        return null;
    }

    public String getVersion() {
        return "2.0";
    }

    public String getCopyright() {
        return "Instituto Gulbenkian de Ciência, Portugal, 2018. Author: Hugo Pereira & Emilio J. Gualda";
    }
    
    public void configurationChanged() {
    }
   
    public void dispose() {
        if (dialog_ != null) {
            dialog_.setVisible(false);
            dialog_ = null;
            dialog_.dispose();
        }
    }
    
    public void setApp(ScriptInterface app) {
        gui_ = (MMStudio) app;
        mmc_ = gui_.getMMCore();
        acq_=gui_.getAcquisitionEngine();
        
        gui_.addMMBackgroundListener(dialog_);
    }

    public void show() {
        if (dialog_ == null) {
            dialog_ = new JDialFinal(this, gui_, acq_) {};
            dialog_.setVisible(true);
        } else {
            dialog_.setPlugin(this);
            dialog_.toFront();
        }
    }
   
}

