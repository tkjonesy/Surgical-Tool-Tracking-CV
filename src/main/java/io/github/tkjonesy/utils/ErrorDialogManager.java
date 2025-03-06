package io.github.tkjonesy.utils;

import javax.swing.*;

public class ErrorDialogManager {

    public static void displayErrorDialog(String message){
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void displayErrorDialogFatal(String message){
        JOptionPane.showMessageDialog(null, message, "Fatal Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static void displayWarningDialog(String message){
        JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

}
