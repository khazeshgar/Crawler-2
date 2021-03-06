import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Timer;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;


public class Main extends TimerTask {
	static String defaultDBPath = "D:\\crawler\\crawler.db";
	static String defaultSavePath = "D:\\crawler";
	static int currentPageID = 2900;
	static Mehrnews mn;
	static String startTime = "01:00";
	static String finishTime = "06:00";
	static String lastTryTableName = "lastTry";
	static String lastTryTableFields = "ID, date";
	static String lastTryTableDateFieldName = "date";
	static String lastTryTableIDFieldName = "ID";
	static String lastTryDateFormat = "yyyy.MM.dd";
	
	
	public static void main(String[] args){
		if(args.length == 2){
			if(args[0].length() != 5 || args[1].length() != 5){
				System.out.println("Wrong Arguments");
				System.out.println("Correct arguments must be like: \"java -jar crawler.jar 01:00 06:00\"");
				System.exit(1);
			}
			else{
				SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
				try {
					@SuppressWarnings("unused")
					Date temp = parser.parse(args[0]);
					temp = parser.parse(args[1]);
					startTime = args[0];
					finishTime = args[1];
				} catch (ParseException e) {
					System.out.println("Wrong Arguments");
					System.out.println("Correct arguments must be like: \"java -jar crawler.jar 01:00 06:00\"");
					System.exit(1);
				}
				
			}
		}
		else if(args.length > 0){
			System.out.println("Wrong Arguments");
			System.out.println("Correct arguments must be like: \"java -jar crawler.jar 01:00 06:00\"");
			System.exit(1);
		}
		
		
		//***********************
		//  DataBase File Path
		//***********************
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Select your database file");
		FileNameExtensionFilter ff = new FileNameExtensionFilter("*.db", "db");
		fc.setFileFilter(ff);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setVisible(true);
		fc.showOpenDialog(null);
		File f = fc.getSelectedFile();
		
		DataBase db;
		if(f != null){
			defaultDBPath = f.getPath();
		}
		else{
			String dbDefaultMessage = "You Did not specify any path for the db file. So we assume that it is located in: " + defaultDBPath + "\n we are going to try to create that file for you if it does not exists!";
			JOptionPane.showMessageDialog(null, dbDefaultMessage, "Notice", JOptionPane.WARNING_MESSAGE);
			
			//create the file if it does not exists
			String dir = defaultDBPath.substring(0,defaultDBPath.lastIndexOf('\\'));
			File tempDir = new File(dir);
			tempDir.mkdirs();
			try {
				new File(defaultDBPath).createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		db = new DataBase(defaultDBPath);
		
		
		//***********************
		//  Save File Path
		//***********************
		JFileChooser dc = new JFileChooser();
		dc.setDialogTitle("Select your saving path");
		dc.setAcceptAllFileFilterUsed(false);
		dc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		dc.showOpenDialog(null);
		File d = dc.getSelectedFile();
		
		
		if(d != null){
			defaultSavePath = d.getPath();
		}
		else{
			String savePathDefaultMessage = "You Did not specify any saving path. So we assume that the saving path is: " + defaultSavePath + "\n we are going to try to create that directory for you if it does not exists!";
			JOptionPane.showMessageDialog(null, savePathDefaultMessage, "Notice", JOptionPane.WARNING_MESSAGE);
		}
		String dir = defaultSavePath + "\\sites\\mehrnews";
		File tempDir = new File(dir);
		tempDir.mkdirs();
		
		//*****************************************
		//   Detect the first list page to visit
		//*****************************************
				
		try {
			//if the table does not exist create it
			if(!db.tableExists(lastTryTableName)){
				db.createTable(lastTryTableName, lastTryTableFields);
			}
			
			//query the table to get the latest date
			String then_date = db.getlastTry_Field(lastTryTableName, lastTryTableDateFieldName);
			String then_id = db.getlastTry_Field(lastTryTableName, lastTryTableIDFieldName);
			
			//	--> fist run skip to end
			//  |
			//	\-> try to estimate the time that has passed from that time. and change 
			//			currentPageID accordingly
			if(then_date != null){
				//there is value in database
				
				DateFormat formatter = new SimpleDateFormat(lastTryDateFormat);
				Date then = formatter.parse(then_date);
				Date now = Calendar.getInstance().getTime();
				
				//calculate the amount of time that has passed
				int days = (int) (now.getTime() - then.getTime())/ (24*60*60*1000); //days in between now and then
				if(days < 365){
					//if more than one year start again
					//means do not change anything
						//else change date accordingly
					currentPageID = Integer.parseInt(then_id);
					currentPageID += (days + 1); //add the number of days that have passed from that day to the pageID
				}
			}
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		
		
		
		
		
		mn = new Mehrnews(defaultDBPath, defaultSavePath);
		
		//Scheduler
		Main main = new Main();
		Timer timer = new Timer();
		long delay = 0;
		long period = 1000*120; //every 120 seconds // 2 minutes
		
		//the run
		try {
			if(!db.tableExists("mehrnews")){
				mn.createTable();
			}
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		timer.scheduleAtFixedRate(main, delay, period);
	}

	@Override
	public void run() {
		if(currentPageID > 0){
			Date currentTime = new Date(System.currentTimeMillis());
			if(timeIsOK()){
				//do one page fetch
				try {
					mn.getLogger().logGoodRun("Run Time: " + currentTime.toString() + "----> Page ID: " + currentPageID);
					mn.getListPage(currentPageID);
					
					mn.getDataBase().emptyTable(lastTryTableName);//only one element in this table
					
					SimpleDateFormat parser = new SimpleDateFormat(lastTryDateFormat);
					String now = parser.format(Calendar.getInstance().getTime());
					
					String insertFields = "'" + currentPageID + "', '" + now + "'";
					mn.getDataBase().insert(lastTryTableName, insertFields);
				} catch (Exception e) {
					e.printStackTrace();
				}
				//decreasing the <<currentPageID>>
				currentPageID--;
			}
			else{
				try {
					mn.getLogger().logTimeNotMatch("Run Time: " + currentTime.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else{
			JOptionPane.showMessageDialog(null, "Crawler finished work!", "Finished", JOptionPane.INFORMATION_MESSAGE);
			System.out.println("Crawler finished work!");
			System.exit(0);
		}
	}

	private boolean timeIsOK() {
		//temporary solution for disabling timing features!
		if(true) return true;
		@SuppressWarnings("unused")
		//if you want to re enable it just remove the upper two lines.
		
		SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
		SimpleDateFormat parser2 = new SimpleDateFormat("HH:mm:ss");
		Date one = null;
		Date six = null;
		Date current = null;
		Calendar cal = null;
		try {
			one = parser.parse(startTime); 
			six = parser.parse(finishTime); 
			cal = Calendar.getInstance();
			current = parser.parse(parser.format(cal.getTime()));//be careful: DST
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		if((one.before(current) || one.equals(current)) && (current.before(six) || current.equals(six))){
			return true;
		}
		else{
			try {
				current = parser2.parse(parser2.format(cal.getTime()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			int position = current.toString().indexOf(':');
			String time = current.toString().substring(position-2, position+6);
			System.out.println("On Pause. Current time --> " + time);
			return false;
		}
	}
}
