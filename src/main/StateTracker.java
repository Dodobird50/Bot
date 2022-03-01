package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Scanner;

import data_classes.OpenCitiesList;
import data_classes.User;

public class StateTracker {
	private String state;
	private OpenCitiesList openCitiesList;

	public void trackState( String state ) {
		state = state.toUpperCase();
		this.state = state;
		openCitiesList = null;

		delayNextCheck();

		while ( true ) {
			File downloadsFolder = new File( "C:/Users/icype/Downloads" );
			File fileToUse = null;
			long mostRecentLastModified = 0;
			for ( File file : downloadsFolder.listFiles() ) {
				String fileName = file.getAbsolutePath();
				if ( fileName.indexOf( "CVS_" + state ) == -1 )
					continue;
				// Only files with "CVS_" + state in them are of interest
				if ( file.lastModified() > mostRecentLastModified ) {
					mostRecentLastModified = file.lastModified();
					fileToUse = file;
				}
			}

			// If there is new data from the last check, as the last check deletes all used data.
			if ( fileToUse != null ) {
				// Delete all older data
				for ( File file : downloadsFolder.listFiles() ) {
					String fileName = file.getAbsolutePath();
					if ( fileName.indexOf( "CVS_" + state ) >= 0 && !fileName.equals( fileToUse.getAbsolutePath() ) )
						file.delete();
				}

				try {
					Scanner scanner = new Scanner( fileToUse );

					String line = scanner.nextLine();
					scanner.close();

					detectAvailableCities( line );
					// Delete data which is now used
					fileToUse.delete();

					// Notify each user about the open cities for state
					for ( User user : MasterControl.getUsers() ) {
						user.updateCurrentlyOpenCities( openCitiesList );
					}
					//MasterControl.notifyStateFrequenciesList( openCitiesList, state );
				}
				catch ( FileNotFoundException e ) {
					Calendar now = Calendar.getInstance();
					System.out.println( "Update from " + calendarToString( now ) + " for " + state + ":"
							+ "\n\t- No data was found." );

					try {
						Thread.sleep( 10000 );
					}
					catch ( InterruptedException ex ) {
						ex.printStackTrace();
					}
					continue;
				}
				
				delayNextCheck();
			}
			else {
				// Last check deletes data that it used. If there still isn't any new data by this check 60 seconds
				// later, then data collection is not active, because data collection collects new data every 59
				// seconds (60 seconds is actually more than enough).
				Calendar now = Calendar.getInstance();
				System.err.println( "Update from " + calendarToString( now ) + " for " + state + ":"
						+ "\n\t- No new data was detected. Is active data collection for " + state
						+ " still occurring?" );
				
				try {
					Thread.sleep( 10000 );
				}
				catch ( InterruptedException e ) {
					e.printStackTrace();
				}
			}
		}
	}

	private void delayNextCheck() {
		try {
			Calendar now = Calendar.getInstance();
			int seconds = now.get( Calendar.SECOND );
			int milliseconds = now.get( Calendar.MILLISECOND );
			Thread.sleep( 60000 - 1000 * seconds - milliseconds + 50 );
		}
		catch ( InterruptedException e ) {
		}
	}

	// Detects which cities are available based on JSON input
	private void detectAvailableCities( String line ) {
		int lastIndex = 0;
		openCitiesList = new OpenCitiesList( state );
		while ( line.indexOf( "city", lastIndex ) >= 0 ) {
			// Get index of "city"
			int cityIndex = line.indexOf( "\"city\"", lastIndex );
			// Get index of first "status" after index of "city"
			int statusIndex = line.indexOf( "\"status\":", cityIndex );
			// Get indices of quotation marks surrounding the value of the status
			int statusValueLeftQuotationIndex = line.indexOf( "\"", statusIndex + 9 );
			int statusValueRightQuotationIndex = line.indexOf( "\"", statusValueLeftQuotationIndex + 9 );
			// Status value is in between the quotation marks surrounding it
			String status = line.substring( statusValueLeftQuotationIndex + 1, statusValueRightQuotationIndex );

			// Get indices of quotation marks surrounding the value of the city
			int cityValueLeftQuotationIndex = line.indexOf( "\"", cityIndex + 7 );
			int cityValueRightQuotationIndex = line.indexOf( "\"", cityValueLeftQuotationIndex + 1 );
			// City value is in between the quotation marks surrounding it
			String cityName = line.substring( cityValueLeftQuotationIndex + 1, cityValueRightQuotationIndex )
					.toUpperCase();

			if ( status.equals( "Available" ) )
				openCitiesList.add( cityName );

			lastIndex = statusValueRightQuotationIndex + 1;
		}

		MasterControl.printLog( openCitiesList, state );
		
	}

	public static String calendarToString( Calendar calendar ) {
		int year = calendar.get( Calendar.YEAR );
		int month = calendar.get( Calendar.MONTH ) + 1;
		int day = calendar.get( Calendar.DAY_OF_MONTH );
		int hour = calendar.get( Calendar.HOUR_OF_DAY );
		int minute = calendar.get( Calendar.MINUTE );
		int second = calendar.get( Calendar.SECOND );

		String dateAndTime = month + "/" + day + "/" + year + " " + hour;
		if ( minute >= 10 )
			dateAndTime += ":" + minute;
		else
			dateAndTime += ":0" + minute;
		if ( second >= 10 )
			dateAndTime += ":" + second;
		else
			dateAndTime += ":0" + second;

		return dateAndTime;
	}
}
