package data_classes;
import java.util.ArrayList;

public class Watchlist extends ArrayList<String> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String state;
	private boolean isFullWatchlist;
	
	public Watchlist( String state, boolean isFullWatchlist ) {
		this.state = state;
		this.isFullWatchlist = isFullWatchlist;
	}
	
	public Watchlist( String state, boolean isFullWatchlist, ArrayList<String> cities ) {
		this.state = state;
		this.isFullWatchlist = isFullWatchlist;
		for ( String city : cities ) {
			this.add( city );
		}
	}
	
	public String getState() {
		return state;
	}
	
	public boolean isFullWatchlist() {
		return isFullWatchlist;
	}
	
	@Override
	public boolean contains( Object city ) {
		String c = (String) city;
		if ( isFullWatchlist ) 
			return true;
		else
			return super.contains( c );
	}
}
