package data_classes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.Scanner;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

import main.MasterControl;

public class User {
	private static final int MAX_MESSAGES_PER_DAY = 12;

	private String phoneNumber;
	private ArrayList<Watchlist> watchlists;
	private ArrayList<OpenCitiesList> openCitiesLists;
	private File userFile;

	private int currentDayOfMonth;
	private int numberOfMessagesSentToday;

	public User( File userFile ) {
		this.userFile = userFile;
		loadData();
	}

	public void loadData() {
		Scanner in = null;
		try {
			in = new Scanner( userFile );
		}
		catch ( FileNotFoundException e ) {
			System.err.println( "Error: user file " + userFile.getPath() + " not found" );
		}
		String filePath = userFile.getPath();
		phoneNumber = "+" + filePath.substring( filePath.lastIndexOf( "\\" ) + 1, filePath.indexOf( ".user" ) );
		currentDayOfMonth = in.nextInt();
		numberOfMessagesSentToday = in.nextInt();
		in.nextLine();

		watchlists = new ArrayList<>();
		int sizeOfWatchlists = Integer.parseInt( in.nextLine().trim() );
		for ( int i = 0; i < sizeOfWatchlists; i++ ) {
			String state = in.nextLine().trim().toUpperCase();
			boolean isFullWatchlist = false;
			if ( state.charAt( 0 ) == '!' ) {
				isFullWatchlist = true;
				state = state.substring( 1 );
			}

			Watchlist newWatchlist = new Watchlist( state, isFullWatchlist );
			if ( !isFullWatchlist ) {
				int numberOfCitiesInWatchlist = Integer.parseInt( in.nextLine().trim() );

				for ( int j = 0; j < numberOfCitiesInWatchlist; j++ ) {
					String city = in.nextLine().trim().toUpperCase();
					newWatchlist.add( city );
				}
			}

			watchlists.add( newWatchlist );
		}

		openCitiesLists = new ArrayList<>();
		int sizeOfOpenCitiesLists;
		try {
			sizeOfOpenCitiesLists = Integer.parseInt( in.nextLine().trim() );
		}
		catch ( NoSuchElementException e ) {
			sizeOfOpenCitiesLists = 0;
		}
		for ( int i = 0; i < sizeOfOpenCitiesLists; i++ ) {
			String state = in.nextLine().trim().toUpperCase();
			int numberOfCitiesInOpenCitiesList = Integer.parseInt( in.nextLine().trim() );

			OpenCitiesList openCitiesList = new OpenCitiesList( state );
			for ( int j = 0; j < numberOfCitiesInOpenCitiesList; j++ ) {
				String city = in.nextLine().trim().toUpperCase();
				openCitiesList.add( city );
			}

			openCitiesLists.add( openCitiesList );
		}

		in.close();

		// Do data validation
		dataValidation();
		updateUserFile();
	}

	// loadData() loads data as it is, so validateData() makes sure that everything is good
	private void dataValidation() {
		phoneNumber = retainDigits( phoneNumber );
		// If phone number only contains 10 digits [area code][7 digit number], then add 1 to the front
		if ( phoneNumber.length() == 10 )
			phoneNumber = "1" + phoneNumber;
		phoneNumber = "+" + phoneNumber;

		String filePath = userFile.getPath();
		String temp = filePath.substring( filePath.lastIndexOf( '\\' ) + 1, filePath.indexOf( ".user" ) );
		if ( !temp.equals( phoneNumber.substring( 1 ) ) )
			throw new IllegalStateException( "Invalid phone number: phoneNumber = +" + phoneNumber.substring( 1 ) );

		for ( int i = 0; i < openCitiesLists.size(); i++ ) {
			// Remove empty OpenCitiesLists
			if ( openCitiesLists.get( i ).size() == 0 ) {
				openCitiesLists.remove( i );
				i--;
			}
		}
		for ( int i = 0; i < watchlists.size(); i++ ) {
			// Remove empty watchlists that are not "full" (marked to contain every city in a state)
			if ( !watchlists.get( i ).isFullWatchlist() && watchlists.get( i ).size() == 0 ) {
				watchlists.remove( i );
				i--;
			}
		}

		for ( int i = 0; i < openCitiesLists.size(); i++ ) {
			OpenCitiesList openCities = openCitiesLists.get( i );
			boolean foundCorrespondingWatchlist = true;
			for ( Watchlist watchlist : watchlists ) {
				if ( openCities.getState().equals( watchlist.getState() ) ) {
					// Found corresponding watchlist in watchlists
					foundCorrespondingWatchlist = true;
					break;
				}
			}
			if ( !foundCorrespondingWatchlist ) {
				// If cannot find corresponding watchlist in watchlists, then remove openCities, because
				// state of openCities is not being tracked
				openCitiesLists.remove( openCities );
				i--;
			}
		}

		for ( int i = 0; i < openCitiesLists.size(); i++ ) {
			OpenCitiesList openCitiesList = openCitiesLists.get( i );
			Watchlist watchlist = getWatchlist( openCitiesList.getState() );

			for ( int j = 0; j < openCitiesList.size(); j++ ) {
				String city = openCitiesList.get( j );
				// If open city isn't in watchlist, remove it from openCities
				if ( !watchlist.contains( city ) ) {
					openCitiesList.remove( city );
					j--;
				}
			}
		}

		for ( int i = 0; i < openCitiesLists.size(); i++ ) {
			OpenCitiesList openCitiesList = openCitiesLists.get( i );
			Watchlist correspondingWatchlist = getWatchlist( openCitiesList.getState() );
			for ( int j = 0; j < openCitiesList.size(); j++ ) {
				String city = openCitiesList.get( j );
				// If an open city is not a city that the user is tracking, then there is no point in keeping it
				// in openCitiesList.
				if ( !correspondingWatchlist.contains( city ) ) {
					openCitiesList.remove( j );
					j--;
				}
			}

			// Possibility that every city in openCitiesList was removed. If that happens, remove openCitiesList
			// from openCitiesLists
			if ( openCitiesList.size() == 0 ) {
				openCitiesLists.remove( i );
				i--;
			}
		}
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public Watchlist getWatchlist( String state ) {
		state = state.toUpperCase();
		for ( Watchlist watchlist : watchlists ) {
			if ( watchlist.getState().equals( state ) )
				return watchlist;
		}

		return null;
	}

	public OpenCitiesList getOpenCitiesList( String state ) {
		state = state.toUpperCase();
		for ( OpenCitiesList watchlist : openCitiesLists ) {
			if ( watchlist.getState().equals( state ) )
				return watchlist;
		}

		return null;
	}

	private void setCurrentOpenCitiesList( OpenCitiesList updatedOpenCitiesList ) {
		String state = updatedOpenCitiesList.getState().toUpperCase();
		for ( int i = 0; i < openCitiesLists.size(); i++ ) {
			OpenCitiesList openCitiesList = openCitiesLists.get( i );
			// If updated open cities list is not empty, override current open cities list for the state
			// with the updated one. Else, remove the current open cities list.
			if ( openCitiesList.getState().equals( state ) ) {
				// openCitiesList for the state of interest found
				if ( updatedOpenCitiesList.size() > 0 )
					// Replace openCitiesList with updatedOpenCitiesList
					openCitiesLists.set( i, updatedOpenCitiesList );
				else
					// Replace openCitiesList with updatedOpenCitiesList, but since updatedOpenCitiesList is empty,
					// delete it from the list. This is equivalent to just deleting openCitiesList directly.
					openCitiesLists.remove( i );
				return;
			}
		}
		// If openCitiesList for the state of interest was not found, then simply add updatedOpenCitiesList to
		// the openCitiesLists as long as it's not empty
		if ( updatedOpenCitiesList.size() > 0 )
			openCitiesLists.add( updatedOpenCitiesList );
	}

	// Detects if updatedOpenCities has any new cities that aren't in currentOpenCities
	public synchronized void updateCurrentlyOpenCities( OpenCitiesList updatedOpenCitiesList ) {
		Calendar now = Calendar.getInstance();
		if ( currentDayOfMonth != now.get( Calendar.DAY_OF_MONTH ) ) {
			currentDayOfMonth = now.get( Calendar.DAY_OF_MONTH );
			numberOfMessagesSentToday = 0;	// Reset number of messages sent today
		}

		String state = updatedOpenCitiesList.getState().toUpperCase();

		Watchlist watchlist = getWatchlist( state );
		if ( watchlist == null )
			return; // User isn't tracking this state

		// currentlyOpenCitiesList( state ) is old with respect to updatedOpenCitiesList
		// Also, oldOpenCititesList and updatedOpenCitiesList factor in the user's watchlist before doing
		// any computations with them
		OpenCitiesList oldOpenCitiesList = getOpenCitiesList( state );
		if ( oldOpenCitiesList == null )
			oldOpenCitiesList = new OpenCitiesList( state );

		// Weed out cities in updatedOpenCitiesList that aren't in the watchlist
		updatedOpenCitiesList = (OpenCitiesList) updatedOpenCitiesList.clone();
		for ( int i = 0; i < updatedOpenCitiesList.size(); i++ ) {
			String city = updatedOpenCitiesList.get( i );
			if ( !watchlist.contains( city ) ) {
				updatedOpenCitiesList.remove( i );
				i--;
			}
		}

		// justOpenedCitiesList stores cities that are not in oldOpenCitiesList, but are in updatedOpenCitiesList
		ArrayList<String> justOpenedCitiesList = new ArrayList<>();
		for ( String city : updatedOpenCitiesList ) {
			// Just opened cities are cities that are in updatedOpenCitiesList but not in the oldOpenCitiesList
			if ( !oldOpenCitiesList.contains( city ) )
				justOpenedCitiesList.add( city );
		}

		setCurrentOpenCitiesList( updatedOpenCitiesList );
		handleJustOpenedCitiesList( state, justOpenedCitiesList );
		updateUserFile();
	}

	private synchronized void handleJustOpenedCitiesList( String state, ArrayList<String> justOpenedCitiesList ) {
		if ( justOpenedCitiesList.size() == 0 || numberOfMessagesSentToday >= MAX_MESSAGES_PER_DAY )
			return; // No point in notifiying user if no cities have just opened

		String message = "Attention! ";
		if ( justOpenedCitiesList.size() > 1 ) {
			message += "CVS store(s) at ";
			for ( int i = 0; i < justOpenedCitiesList.size(); i++ ) {
				String city = justOpenedCitiesList.get( i );
				if ( i < justOpenedCitiesList.size() - 1 )
					message += city + ", ";
				else
					message += "and " + city + ", ";
			}
		}
		else
			message += "CVS store(s) at " + justOpenedCitiesList.get( 0 ) + ", ";

		message += state;
		message += " have just opened up for vaccine appointments! Quick, go visit https://tinyurl.com/39a3m6zx "
				+ "right now to book your appointment before it's too late!";

		message += "\n\nIf you were able to snag an appointment and no longer need to keep an eye on future openings, "
				+ "you can unenroll from the bot by completing the form at https://forms.gle/QGjM5M7aihtnKFfQ8.";

//		Twilio.init( MasterControl.getAccountSid(), MasterControl.getAuthToken() );
//		Message.creator( new com.twilio.type.PhoneNumber( phoneNumber ),
//				new com.twilio.type.PhoneNumber( "+17328387875" ), message ).create();
		System.out.println( phoneNumber + " was notified about cities that have just opened at " + state + ":" );
		for ( String city : justOpenedCitiesList ) {
			System.out.println( "\t- " + city + ", " + state );
		}
		
		numberOfMessagesSentToday++;
		if ( numberOfMessagesSentToday == MAX_MESSAGES_PER_DAY )
			System.out.println( phoneNumber + " has exhausted his/her notification count for today." );
	}

	public synchronized void updateUserFile() {
		try {
			if ( !userFile.exists() )
				System.out.println( "User file of " + phoneNumber + " was deleted - be careful!" );
			
			FileWriter fw = new FileWriter( userFile );
			fw.write( currentDayOfMonth + " " + numberOfMessagesSentToday + "\n" );
			fw.write( watchlists.size() + "\n" );
			for ( int i = 0; i < watchlists.size(); i++ ) {
				Watchlist watchlist = watchlists.get( i );
				if ( !watchlist.isFullWatchlist() ) {
					fw.write( "\t" + watchlist.getState() + "\n" );
					fw.write( "\t\t" + watchlist.size() + "\n" );
					for ( int j = 0; j < watchlist.size(); j++ ) {
						String city = watchlist.get( j );
						fw.write( "\t\t" + city + "\n" );
					}
				}
				else
					fw.write( "\t!" + watchlist.getState() + "\n" );

			}

			fw.write( openCitiesLists.size() + "\n" );
			for ( int i = 0; i < openCitiesLists.size(); i++ ) {
				OpenCitiesList openCitiesList = openCitiesLists.get( i );
				if ( openCitiesList.size() > 0 ) {
					fw.write( "\t" + openCitiesList.getState() + "\n" );
					fw.write( "\t\t" + openCitiesList.size() + "\n" );
					for ( int j = 0; j < openCitiesList.size(); j++ ) {
						String city = openCitiesList.get( j );
						fw.write( "\t\t" + city + "\n" );
					}
				}
			}

			fw.close();
		}
		catch ( IOException e ) {
			System.err.println( "Failed to save user data to file" );
		}
	}

	private String retainDigits( String s ) {
		// Keep only digits in string that also aren't in brackets.
		String out = "";
		for ( int i = 0; i < s.length(); i++ ) {
			char ch = s.charAt( i );
			if ( Character.isDigit( ch ) ) {
				out += ch;
			}
		}

		return out;
	}

	public String toString() {
		String out = "";
		out += "Phone number: " + phoneNumber + "\n";
		for ( Watchlist watchlist : watchlists ) {
			if ( watchlist.isFullWatchlist() )
				out += "\t- All cities are being tracked in " + watchlist.getState() + "\n";
			else {
				out += "\t- Cities being tracked in " + watchlist.getState() + ":\n";
				for ( String city : watchlist ) {
					out += "\t\t- " + city + "\n";
				}
			}
		}
		out = out.substring( 0, out.length() - 1 ); // Delete final newline character

		return out;
	}

	public ArrayList<Watchlist> getWatchlists() {
		return watchlists;
	}
}
