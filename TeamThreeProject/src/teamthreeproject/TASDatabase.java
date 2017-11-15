
package teamthreeproject;
import java.sql.*;
import java.util.*;
import java.text.*;
import org.json.simple.*;

/**
 *
 * @author TeamThree
 */
public class TASDatabase {
    private Connection conn;
    private Statement state;
    private PreparedStatement prepstate;
    private ResultSet result;
    ArrayList<Punch> badge_punches = new ArrayList<Punch>();
    ArrayList<Punch> day_punches = new ArrayList<Punch>();
    ArrayList<HashMap<String, String>> jsonData = new ArrayList<HashMap<String, String>>();    
    
    //establish a database connection
    public TASDatabase(){
        try{
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String url = "jdbc:mysql://localhost:3306/tas";
            String username = "tasuser";
            String password = "snellybelly42";                
            conn = DriverManager.getConnection(url, username, password);
        } catch(Exception e){}
        
    }
    
    //closes connection
    public void closeConnection() {
        try {
            conn.close();
        } catch (Exception e) {}
    }
    
    //method for retrieving a punch's info from the database given the punch ID
    public Punch getPunch(int id){
        Punch punch = null;
        
        try{
            prepstate = conn.prepareStatement("SELECT id, terminalid, badgeid, unix_timestamp(originaltimestamp) AS ots,"
                                            + "eventtypeid, eventdata FROM event WHERE id = ?");
            prepstate.setInt(1,id);
            result = prepstate.executeQuery();
            if(result != null){
                result.next();
                punch = new Punch(id, result.getInt("terminalid"), result.getString("badgeid"), 
                                  result.getLong("ots"), result.getInt("eventtypeid"), result.getString("eventdata"));
            }
            result.close();
            prepstate.close();
        } catch(Exception e){}
        
        return punch;
    }
    
    //method for inserting a new punch into the database
    public int insertPunch(Punch punch) {
        int punchid = 0;
        int result = 0;
        ResultSet keys;
    try {
           String badgeid = punch.getBadgeID();
           int terminalid = punch.getTerminalID();
           int eventtypeid = punch.getEventTypeID();
           String sql = "INSERT INTO event(badgeid, originaltimestamp, terminalid, eventtypeid) VALUES (?, ?, ?, ?)";
           prepstate = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
           prepstate.setString(1, badgeid);
           prepstate.setString(2, (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(punch.getOriginalTimestamp().getTime()));
           prepstate.setInt(3, terminalid);
           prepstate.setInt(4, eventtypeid);
           result = prepstate.executeUpdate();
           if(result == 1){
               keys = prepstate.getGeneratedKeys();
               if(keys.next()){
                   punchid = keys.getInt(1);
               }
           }
           prepstate.close();
       }
       catch(Exception e){}

       return punchid;
    }
    
    //method for inserting the adjusted timestamp and event data back into the database after adjusting
    public void insertAdjusted(GregorianCalendar ats, int id, String event_data) {
        try {          
           prepstate = conn.prepareStatement("INSERT INTO event(adjustedtimestamp,eventdata) VALUES (?,?) WHERE id = ?");
           prepstate.setString(1, (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(ats.getTime()));
           prepstate.setString(2, event_data);
           prepstate.setInt(3,id);
           prepstate.executeUpdate();
           prepstate.close();
       }
       catch(Exception e){}
    }
    
    //Method for retrieving all punches in a day and parsing all the data into a JSON string
    public String getPunchListAsJSON(Punch p) {
        collectPunch(p);
        
        for (Punch dayp: day_punches) {
            HashMap<String, String>  punchData = new HashMap<>();
            //use getters and insert punch data into hash maps
        }
        
        return "";
    }
    
    /* finds which shift rules should apply to a given punch based on its first clock-in of the day
       (currently only tests for 1st and 2nd shift) */
    public int findShift(Punch p) {  
        int s = 0;
        
        if (p.getOriginalTimestamp().get(Calendar.HOUR_OF_DAY) < 10 && p.getOriginalTimestamp().get(Calendar.HOUR_OF_DAY) >= 5) {
            s = 1;
        }
        else if (p.getOriginalTimestamp().get(Calendar.HOUR_OF_DAY) < 15 && p.getOriginalTimestamp().get(Calendar.HOUR_OF_DAY) >= 10) {
            s = 2;
        }
        
        return s;
    }
    
    /* Finds which shift the punch passed in belongs to, collects punches with the same Badge ID as that punch and stores them
       in an ArrayList, finds punches in that list that are on the same day as the punch and adds them to a new list, adjusts all the 
       timestamps of those punches, and finally totals the minutes worked deducting lunchtime if applicable */
    public int getMinutesAccrued(Punch p) {
        int total = 0;
        int lunch_deduct;
        long clock_in = 0;
        long clock_out = 0;
        long diff = 0;
        int diff_in_min = 0;
        boolean lunch_break = false;
        
        //find shift rules and collect punches
        Shift s = getShift(findShift(p));
        lunch_deduct = s.getLunchDeduct();
        collectPunch(p);     
        
        //adjust the punches
        for (Punch punch: day_punches) {
            punch.adjust(s);
        }
        
        //checks if the employee took a lunch break
        if (day_punches.size() > 2) {
            lunch_break = true;
        }
        
        //totals the time worked for the day
        while (!day_punches.isEmpty()) {
            switch (day_punches.get(0).getEventTypeID()) {
                case 1:
                    clock_in = day_punches.get(0).getAdjustedTimestamp().getTimeInMillis();
                    day_punches.remove(0);
                    break;
                case 0:
                    clock_out = day_punches.get(0).getAdjustedTimestamp().getTimeInMillis();
                    diff = (clock_out - clock_in);
                    diff_in_min = (int) (diff / 60000);
                    total += diff_in_min;
                    day_punches.remove(0);
                    break;
                case 2:
                    day_punches.remove(0);
                    break;
                default:
                    break;
            }
        }        
        
        //checks if lunch_deduct should be applied if the employee did not take a break
        if ((lunch_break == false) && (total >= lunch_deduct)) {
            total -= s.getLunchLength();
        }
        
        return total;
    }
    
    //method for collecting all the punches for 1 employee in a day
    private ArrayList<Punch> collectPunch(Punch p){
        String badgeID = p.getBadgeID();

        try{
            prepstate = conn.prepareStatement("SELECT *, unix_timestamp(originaltimestamp)"
                    + "AS ots FROM event WHERE badgeid = ?");
            prepstate.setString(1, badgeID);
            result = prepstate.executeQuery();
            while (result != null){
                result.next();
                Punch collectedPunch = new Punch(result.getInt("id"), result.getInt("terminalid"),
                                        result.getString("badgeid"), result.getLong("ots"),
                                        result.getInt("eventtypeid"), result.getString("eventdata"));
                                        
                badge_punches.add(collectedPunch);
               
            }
        }
        catch(Exception e){}
        
        //find punches of the same day and add them to new list
        for(Punch dayp: badge_punches){
            if((p.getOriginalTimestamp().get(Calendar.MONTH) == dayp.getOriginalTimestamp().get(Calendar.MONTH))
               && (p.getOriginalTimestamp().get(Calendar.DAY_OF_MONTH) == dayp.getOriginalTimestamp().get(Calendar.DAY_OF_MONTH))) {
                day_punches.add(dayp);
            }
        }
        return day_punches;
    }
    
    //method for getting the shift rules of a given shift
    public Shift getShift(int id) {
        ResultSet result;
        Shift shift = null;
        
        //Retrieve shift rules from database and store info in new Shift object       
        try {
            prepstate = conn.prepareStatement("SELECT *, unix_timestamp(start) AS shift_start,"
                    + "unix_timestamp(stop) AS shift_stop, unix_timestamp(lunchstart) AS l_start,"
                    + "unix_timestamp(lunchstop) AS l_stop FROM shift WHERE id = ?");
            prepstate.setInt(1, id);
            result = prepstate.executeQuery();            
            if (result != null) {
                result.next();
                shift = new Shift(id, result.getString("description"),
                        result.getLong("shift_start"), result.getLong("shift_stop"),
                        result.getInt("interval"), result.getInt("graceperiod"),
                        result.getInt("dock"), result.getLong("l_start"),
                        result.getLong("l_stop"), result.getInt("lunchdeduct"),
                        result.getInt("maxtime"), result.getInt("overtimethreshold"));
            }
            result.close();
            prepstate.close();
  
        } catch (Exception e) {}
        
        return shift;
    }
    
    //method for getting an employee's name given their badge ID
    public Badge getBadge(String id) {
        ResultSet result;
        Badge badge = null;
        
        try {
            prepstate = conn.prepareStatement("SELECT * FROM badge WHERE id = ?");
            prepstate.setString(1, id);
            result = prepstate.executeQuery();            
            if (result != null) {
                result.next();
                badge = new Badge(result.getString("id"), result.getString("description"));               
            }
            result.close();
            prepstate.close();
  
        } catch (Exception e) {}
        
        
        return badge;
    }
        
}
