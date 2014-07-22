/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.globals;

/**
 *
 * @author mauro
 */
public class TupleResult {
    private boolean error; 
    private String message;
    
    public boolean getError(){
        return error;
    };
    
    public String getMessage(){
        return message;
    };
    
    public TupleResult(boolean err, String mess){
        error = err;
        message = mess;
    }
}
