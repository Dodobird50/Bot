package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

public class SendCustomMessage {
	public static void main( String[] args ) {
		boolean isLocked = false;
		try {
			Scanner scanner = new Scanner( new File( "other/custom_message_allowed.txt" ) );
			int status = scanner.nextInt();
			if ( status == 0 )
				isLocked = true;
			scanner.close();
		}
		catch ( FileNotFoundException e ) {
			isLocked = true;
		}

		if ( isLocked ) {
			System.out.println( "Locked" );
			System.exit( 0 );
		}

		String[] phoneNumbers = { "+12038897666", "+16094324160", "+16095298751", "+19734959321" };
		String message = "From the previous announcement, it is with regret that I am announcing that this bot will "
				+ "no longer be operational as of now. Thank you for enrolling in the bot, and good luck with your "
				+ "future endeavors!";

		for ( String phoneNumber : phoneNumbers ) {
//			Twilio.init( MasterControl.getAccountSid(), MasterControl.getAuthToken() );
//			Message.creator( new com.twilio.type.PhoneNumber( phoneNumber ),
//					new com.twilio.type.PhoneNumber( "+17328387875" ), message ).create();
			System.out.println( "Custom message sent to " + phoneNumber );
		}

		try {
			FileWriter fw = new FileWriter( new File( "other/custom_message_allowed.txt" ), false );
			fw.write( "0" );
			fw.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}

		System.out.println( "Success!" );
	}
}
