/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SPIM_FLUID;

/**
 *
 * @author pcadmin
 */
public class Global {
    public static String Master = "SRotation";  //---------------------- Name of Master Arduino defined at Hardware Configurations
    public static String Galvo1 = "Galvo1";     //---------------------- Name of Galvo1 Arduino defined at Hardware Configurations
    public static String Galvo2 = "Galvo2";     //---------------------- Name of Galvo2 Arduino defined at Hardware Configurations
    public static int shutter = 0;
    public static int Emi_Previous_Item=0;
    public static int Exci_Previous_Item=0;
    public static int Int_Previous_Item=0;
    public static int rotationM = 0;
    public static int excitM = 1;
    public static int emissM = 2;
    public static int intenM = 3;
    public static int onfocus=0;
    public static double GampINI1=0;    ///ONLY THIS PARAMETERS WORKS IN VERTICAL SPIM
    public static double GampEND1=0;    ///ONLY THIS PARAMETERS WORKS IN VERTICAL SPIM
    
    public static double GampINI2=0;
    public static double GampEND2=0;
    
    public static double GampINI3=0;
    public static double GampEND3=0;
    
    public static double GampINI4=0;
    public static double GampEND4=0;
}
