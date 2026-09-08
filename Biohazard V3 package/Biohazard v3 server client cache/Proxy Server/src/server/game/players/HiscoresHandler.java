package server.game.players;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class HiscoresHandler {

    public static Connection con = null;
    public static Statement stmt;
    public static boolean connectionMade;
	
    public static void createConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();//opens class
            String IP="localhost";//connection ip
            String DB="hiscores";//database name
            String User="root";//username
            String Pass="------"; //password
            con = DriverManager.getConnection("jdbc:mysql://"+IP+"/"+DB, User, Pass);//creates connection
            stmt = con.createStatement();
        } catch (Exception e) {//catches if connection failed
            System.out.println("Hiscores Connection to SQL database failed!");//tells you it failed @ the run.bat
            e.printStackTrace();
        }
    }
 
 
    public static ResultSet query(String s) throws SQLException {
        try {
            if (s.toLowerCase().startsWith("select")) {
                ResultSet rs = stmt.executeQuery(s);
                return rs;
            } else {
                stmt.executeUpdate(s);
            }
            return null;
        } catch (Exception e) {
            destroyConnection();
        }
        return null;
    }
 
    public static void destroyConnection() {
        try {
            stmt.close();
            con.close();
        } catch (Exception e) {
        }
    }
    public static int[] getOverall(Client player) {
        int totalLevel = 0;
        int totalXp = 0;
        for(int i = 0; i <= 22; i++) {
            totalLevel += player.getLevelForXP(player.playerXP[i]);
        }
        for(int i = 0; i <= 22; i++) {
            totalXp += player.playerXP[i];
        }
        return new int[] {totalLevel, totalXp};
    }
	
	public static boolean saveHighScore(Client player) {//saves hiscores
        try {
        	createConnection();//creates connection
        	ResultSet rs = query("SELECT * FROM `hs` WHERE `username`='"+player.playerName+"'");
        	//query("DELETE FROM `hiscores`.`hs` (`username`) VALUES('"+player.getUsername()+"')");
        	//query("INSERT INTO `hs`(`username`) VALUES('"+player.getUsername()+"')");
            int[] overall = getOverall(player);//just a int
			int r = 1;
			while(rs.next() && r == 1) {
				for(int i = 0; i < 23; i++) {
					String lvl = "lvl_"+(i+1);
					String xp = "xp_"+(i+1);
					int level = player.getLevelForXP(player.playerXP[i]);
					if(level > 99 && i != 24) 
						level = 99;
					query("UPDATE hs SET "+lvl+"='"+level+"', "+xp+"='"+player.playerXP[i]+"' WHERE username='"+player.playerName+"'");
				}
				query("UPDATE hs SET total_exp='"+overall[1]+"', total_lvl='"+overall[0]+"' WHERE username='"+player.playerName+"'");
				System.out.println("Highscores have been updated for "+player.playerName);
				r = 0;
				destroyConnection();
				return false;
			}
			query("INSERT INTO `hs`(`username`) VALUES('"+player.playerName+"')");
        } catch (Exception e) {
        	System.out.println("Hiscores Error, could not save highscores for " + player.playerName +".");//couldnt save the player on hiscores, it tells u it didnt on the run.bat
            return false;
        }
        return true;
    }
}
