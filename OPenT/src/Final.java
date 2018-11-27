
import mmcorej.CMMCore;
import org.micromanager.MMStudio;
import org.micromanager.acquisition.AcquisitionEngine;
import org.micromanager.api.ScriptInterface;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Hugo Pereira
 */
public class Final implements org.micromanager.api.MMPlugin{
   public static final String menuName = "Final OPenT";
   public static final String tooltipDescription = "Automatically acquire multiple angles of a sample";
   public static String versionNumber = "1.171205";
   private MMStudio gui_;
   private JDialFinal myFrame_;
   private  AcquisitionEngine acq_;
   private RotationControl2 RC_;
   private CMMCore mmc_;
   //private Mytable mtable_;
   //Static variables so we can use script panel to tweak interpolation params
   //Exponent for Shepard interpolation
   
    /**
     *
     */
       public static double shepardExponent = 2; 
   
    @Override
    public void dispose() {
        if (myFrame_ != null) {
            myFrame_.setVisible(false);
            myFrame_.dispose();
            myFrame_ = null;
        }
    }

    @Override
    public void setApp(ScriptInterface si) {
      gui_ = (MMStudio)si;
      mmc_ = gui_.getMMCore();
      acq_ = gui_.getAcquisitionEngine();
      
      
      // Used to change the background layout of the form.  Does not work on Windows
      gui_.addMMBackgroundListener(myFrame_);    
    }

    @Override
    public void show() {
        if (myFrame_ == null){
        myFrame_ = new JDialFinal(this, gui_, acq_);
        myFrame_.setVisible(true);
        } else {
            myFrame_.setPlugin(this);
            myFrame_.toFront();
        }
    }

    @Override
    public String getDescription() {
        return tooltipDescription;
    }

    @Override
    public String getInfo() {
        return tooltipDescription;
    }

    @Override
    public String getVersion() {
        return versionNumber;
    }

    @Override
    public String getCopyright() {
        return "Instituto Gulbenkian de Ciência, 2017";    
    }
    
}
