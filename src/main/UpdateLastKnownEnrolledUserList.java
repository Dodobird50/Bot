package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

import data_classes.User;
import data_classes.Watchlist;

// The enrolled user list is the official list of enrolled users that MasterThread can notify of openings. While
// program detects new users in the users folder to be enrolled, and to mark them as enrolled, sends them a text 
// message confirming their enrollment in the bot. At the same time, it detects deleted users, and sends them a 
// text message confirming their unenrollment from the bot.
public class UpdateLastKnownEnrolledUserList {
	public static void synchronizeLastKnownEnrolledUserList() {
		File usersFolder = new File( "enrolled_users" );
		TreeSet<String> usersInUsersFolder = new TreeSet<>();
		for ( File user : usersFolder.listFiles() ) {
			String fileName = user.getAbsolutePath();
			if ( fileName.indexOf( "last_known" ) >= 0 )
				continue;

			usersInUsersFolder.add( fileName.substring( fileName.lastIndexOf( "\\" ) + 1, fileName.indexOf( "." ) ) );
		}

		TreeSet<String> lastKnownUserList = new TreeSet<String>();
		File lastKnownUserListFile = new File( "enrolled_users\\last_known_enrolled_user_list.txt" );
		try {
			Scanner in = new Scanner( lastKnownUserListFile );
			while ( in.hasNext() ) {
				lastKnownUserList.add( in.nextLine() );
			}
			in.close();
			TreeSet<String> newlyEnrolledUsers = getNewlyEnrolledUsers( lastKnownUserList, usersInUsersFolder );
			TreeSet<String> newlyUnenrolledUsers = getNewlyUnenrolledUsers( lastKnownUserList, usersInUsersFolder );

			if ( !getSynchronizationStatus() ) {
				in = new Scanner( System.in );
				System.out.print( "Type \"sync\" to synchronize enrolled user list: " );
				String text = in.nextLine();
				in.close();
				if ( !text.equals( "sync" ) )
					System.exit( 0 );
				
				for ( String user : newlyEnrolledUsers ) {
					notifyUserOfEnrollment( "+" + user );
				}
				for ( String user : newlyUnenrolledUsers ) {
					notifyUserOfUnenrollment( "+" + user );
				}

				storeCurrentUserList( usersInUsersFolder );
			}
			else
				System.out.println( "Last known enrolled user list is up to date." );
			
		}
		catch ( FileNotFoundException e ) {
			System.err.println( "Last known enrolled user list not found." );
		}
	}

	private static TreeSet<String> getNewlyEnrolledUsers( TreeSet<String> currentEnrolledUsers,
			TreeSet<String> usersInUserFolder ) {
		TreeSet<String> newlyEnrolledUsers = new TreeSet<>();
		for ( String user : usersInUserFolder ) {
			// A user is newly enrolled if he or she is in the user folder but is not in the enrolled user list yet.
			if ( !currentEnrolledUsers.contains( user ) )
				newlyEnrolledUsers.add( user );
		}
		return newlyEnrolledUsers;
	}

	private static TreeSet<String> getNewlyUnenrolledUsers( TreeSet<String> currentEnrolledUsers,
			TreeSet<String> usersInUserFolder ) {
		TreeSet<String> newlyUnenrolledUsers = new TreeSet<>();
		for ( String user : currentEnrolledUsers ) {
			// A user is newly unenrolled if he or she is in the enrolled user list but is no longer in the user folder.
			if ( !usersInUserFolder.contains( user ) )
				newlyUnenrolledUsers.add( user );
		}
		return newlyUnenrolledUsers;
	}

	private static void notifyUserOfEnrollment( String phoneNumber ) {
		Twilio.init( MasterControl.getAccountSid(), MasterControl.getAuthToken() );
		String message = "Hello! This message is to nofify you that you have been enrolled in the CVS COVID-19 "
				+ "vaccine bot. If you would like to unenroll, please fill out the form at "
				+ "https://forms.gle/QGjM5M7aihtnKFfQ8. Thank you!";

		User user = getUser( phoneNumber );
		message += "\n\nAs a reminder, here are the cities that are you are tracking:\n";
		for ( Watchlist watchlist : user.getWatchlists() ) {
			if ( watchlist.isFullWatchlist() )
				message += "\t- In " + watchlist.getState() + ": all cities\n";
			else {
				message += "\t- In " + watchlist.getState() + ":\n";
				for ( String city : watchlist ) {
					message += "\t\t- " + city + "\n";
				}
			}
		}
		message = message.substring( 0, message.length() - 1 ); // Delete final newline character

		Message.creator( new com.twilio.type.PhoneNumber( phoneNumber ),
				new com.twilio.type.PhoneNumber( "+17328387875" ), message ).create();
		System.out.println( "Notified " + phoneNumber + " of enrollment." );
	}

	private static User getUser( String phoneNumber ) {
		if ( phoneNumber.charAt( 0 ) == '+' )
			phoneNumber = phoneNumber.substring( 1 );
		String filePath = "enrolled_users\\" + phoneNumber + ".user";
		return new User( new File( filePath ) );
	}

	private static void storeCurrentUserList( TreeSet<String> currentUserList ) {
		FileWriter fw;
		try {
			fw = new FileWriter( new File( "enrolled_users\\last_known_enrolled_user_list.txt" ) );
			Iterator<String> iter = currentUserList.iterator();
			while ( iter.hasNext() ) {
				String user = iter.next();
				if ( iter.hasNext() )
					fw.write( user + "\n" );
				else
					fw.write( user );
			}
			fw.close();
		}
		catch ( IOException e ) {
		}

	}

	private static void notifyUserOfUnenrollment( String phoneNumber ) {
		Twilio.init( MasterControl.getAccountSid(), MasterControl.getAuthToken() );
		String message = "Hello! This message is to nofify you that you have unenrolled in the CVS COVID-19 vaccine "
				+ "bot. If you would like to re-enroll, please fill out the form at "
				+ "https://forms.gle/QGjM5M7aihtnKFfQ8. We are sorry to see you go!";
		Message.creator( new com.twilio.type.PhoneNumber( phoneNumber ),
				new com.twilio.type.PhoneNumber( "+17328387875" ), message ).create();

		System.out.println( "Notified " + phoneNumber + " of unenrollment." );
	}

	private static boolean getSynchronizationStatus() {
		File userList = new File( "enrolled_users/last_known_enrolled_user_list.txt" );
		boolean isSynchronized = true;
		ArrayList<String> enrolledUsers = new ArrayList<>();
		ArrayList<String> usersInUserFolder = new ArrayList<>();

		try {
			Scanner in = new Scanner( userList );

			// Do not directly use files from users folder; use enrolled users in enrolled_user_list.txt
			while ( in.hasNext() ) {
				String enrolledUserFilePath = in.nextLine();
				enrolledUsers.add( enrolledUserFilePath );
			}

			in.close();

			File userFolder = new File( "enrolled_users" );
			for ( File userFile : userFolder.listFiles() ) {
				String userInUserFolder = userFile.getPath();
				if ( userInUserFolder.indexOf( "last_known" ) >= 0 )
					continue;
				userInUserFolder = userInUserFolder.substring( userInUserFolder.lastIndexOf( "\\" ) + 1,
						userInUserFolder.indexOf( ".user" ) );
				usersInUserFolder.add( userInUserFolder );
			}

			// Users that are still in the enrolled list but no longer in the users folder need to be unenrolled.
			for ( String enrolledUser : enrolledUsers ) {
				if ( !usersInUserFolder.contains( enrolledUser ) ) {
					System.out.println( "+" + enrolledUser + " needs to be unenrolled." );
					isSynchronized = false;
				}
			}
			// Users that are in the users folder but not in the enrolled list yet need to be enrolled.
			for ( String userInUserFolder : usersInUserFolder ) {
				if ( !enrolledUsers.contains( userInUserFolder ) ) {
					System.out.println( "+" + userInUserFolder + " needs to be enrolled." );
					isSynchronized = false;
				}
			}
		}
		catch ( FileNotFoundException e ) {
			System.out.println( "Enrolled user list not found." );
			isSynchronized = false;
		}

		return isSynchronized;
	}
}
