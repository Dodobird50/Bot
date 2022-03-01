package data_classes;
import java.util.ArrayList;

public class OpenCitiesList extends ArrayList<String> implements Cloneable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String state;
	
	public OpenCitiesList( String state ) {
		this.state = state;
	}
	
	public String getState() {
		return state;
	}
	
	@Override
	public Object clone() {
		OpenCitiesList clone = new OpenCitiesList( state );
		for ( String city : this ) {
			clone.add( city );
		}
		
		return clone;
	}

}
