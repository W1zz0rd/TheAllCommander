package c2.session.macro;

import java.util.ArrayList;
import java.util.List;

public class MacroOutcome {
	
	private List<String> errors = new ArrayList<>();
	private List<String> outputLines = new ArrayList<>();
	private List<String> auditFindings = new ArrayList<>();
	
	/**
	* This method returns true if the MacroOutcome contains any errors. 
	*
	* @return true/false if there are errors
	*/
	public boolean hasErrors() {
		return !errors.isEmpty();
	}
	
	/**
	* This method returns the list of any encountered errors. 
	*
	* @return List of string errors
	*/
	public List<String> getErrors(){
		return errors;
	}
	
	/**
	* This method returns the list of all output from the macro. This includes commands sent, responses received,
	* and errors encountered in the order that they occurred.  
	*
	* @return List of strings representing lines of output
	*/
	public List<String> getOutput(){
		return outputLines;
	}

	/**
	 * This method returns a list of all audit findings.
	 * 
	 * @return List of strings of audit findings
	 */
	public List<String> getAuditFindings(){
		return auditFindings;
	}
	
	/**
	* This adds an error message to the list of errors and the overall set of consolidated IO  
	*
	* @param error The error message supplied by the Macro
	*/
	public void addError(String error) {
		errors.add(error);
		outputLines.add("Error: " + error);
	}
	
	/**
	* This adds an command to the overall set of consolidated IO  
	*
	* @param command The command to log
	*/
	public void addSentCommand(String command) {
		outputLines.add("Sent Command: '" + command + "'");
	}
	
	/**
	* This adds a response from the client to the overall set of consolidated IO  
	*
	* @param response The response to log
	*/
	public void addResponseIo(String response) {
		outputLines.add("Received response: '" + response + "'");
	}
	
	/**
	* This adds a message from the AbstractCommandMacro to the user in to the overall set of consolidated IO  
	*
	* @param message The message to log
	*/
	public void addMacroMessage(String message) {
		outputLines.add("Macro Executor: '" + message + "'");
	}
	
	/**
	* This adds a message from the AbstractCommandMacro to the user informing them of an audit finding  
	*
	* @param message The message to log
	*/
	public void addAuditFinding(String message) {
		String formattedMessage = "Audit Finding: '" + message + "'";
		outputLines.add(formattedMessage);
		auditFindings.add(formattedMessage);
	}
	
	/**
	 * This method takes another MacroOutcome and appends it to this object
	 * 
	 * @param outcome
	 */
	public void appendMacro(MacroOutcome outcome) {
		errors.addAll(outcome.errors);
		outputLines.addAll(outcome.outputLines);
		auditFindings.addAll(outcome.auditFindings);
	}
}
