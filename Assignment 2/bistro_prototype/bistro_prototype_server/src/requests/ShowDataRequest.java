package requests;

import java.io.Serializable;

public class ShowDataRequest implements Serializable{
	
	private  CommandType commandType;
	
	public ShowDataRequest(CommandType commandType) {
		this.commandType = commandType;
	}
	
	public CommandType getCommandType() {
		return this.commandType;
	}
}
