package main;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;

import data_classes.User;

public class MasterControl {
	private static final String ACCOUNT_SID = "";
	private static final String AUTH_TOKEN = "";
	private static final String[] states = { "NJ" };
	private static ArrayList<User> users;

	public static void main( String[] args ) {
		users = new ArrayList<User>();

		// Perform synchronization check
		UpdateLastKnownEnrolledUserList.synchronizeLastKnownEnrolledUserList();
		System.out.println();

		File userFolder = new File( "enrolled_users" );
		// By this point, enrolled user list is synchronized with user files in usersFolder, so usersFolder
		// accurately represents list of enrolled users
		for ( File enrolledUser : userFolder.listFiles() ) {
			if ( enrolledUser.getPath().indexOf( "last_known" ) >= 0 )
				continue;
			try {
				users.add( new User( enrolledUser ) );
			}
			catch ( Exception e ) {
				String path = enrolledUser.getPath();
				String phoneNumber = "+" + path.substring( path.indexOf( "\\" ) + 1, path.indexOf( ".user" ) );
				System.err.println( phoneNumber + " failed to process." );
			}
		}
//		for ( User user : users ) {
//			System.out.println( user + "\n" );
//		}

		System.out.println( "CAUTION: make sure that you are actively collecting data from the CVS website! "
				+ "Otherwise, any changes to vaccine availability online will not be reflected here.\n" );
		for ( String state : states ) {
			Thread thread = new Thread() {
				public void run() {
					new StateTracker().trackState( state );
				}
			};
			thread.setName( state.toUpperCase() );
			thread.start();
		}
	}

	public static synchronized void printLog( ArrayList<String> availableCities, String state ) {
		Calendar now = Calendar.getInstance();
		System.out.println( "Update from " + StateTracker.calendarToString( now ) + " for " + state + ":" );

		// List all available cities
		if ( availableCities.size() >= 2 )
			System.out.println( "\t- " + availableCities.size() + " CVS pharmacies are currently open for "
					+ "vaccine appointments:" );
		else if ( availableCities.size() == 1 )
			System.out.println( "\t- 1 CVS pharmacy is currently open for vaccine appointments:" );
		else
			System.out.println( "\t- 0 CVS pharmacies are currently open for vaccine appointments." );

		for ( int i = 0; i < 5 && i < availableCities.size(); i++ ) {
			String city = availableCities.get( i );
			System.out.println( "\t\t- " + city.toUpperCase() + ", " + state );
		}
		if ( availableCities.size() > 5 ) {
			int more = availableCities.size() - 5;
			System.out.println( "\t\t- and " + more + " more..." );
		}
	}

	public static ArrayList<User> getUsers() {
		return users;
	}

	public static String getAccountSid() {
		return ACCOUNT_SID;
	}

	public static String getAuthToken() {
		return AUTH_TOKEN;
	}

	public static int numberOfStates() {
		return states.length;
	}
}
