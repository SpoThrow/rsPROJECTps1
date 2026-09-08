package core.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import server.game.players.Client;
public class rspswebstore {
//Created by Soccerjunki @ rune-server.org // http://rspsdata.org/ Copyright 2013 
public static String secret = "04025959b191f8f9de3f924f0940515ff56ec2ab97d604832d90f6394e3f341f";//This is found on http://rspsdata.org/system/webstore.php?setup=317
public static String email = "jordy-474@outlook.com"; //This is the one you use for RSPSDATA


//Do not play with any of these voids, changing them could cause potential risk to your server
//for example allowing them to claim the donation more than once.
		public static boolean isNumeric(String s) {  
				return s.matches("[-+]?\\d*\\.?\\d+");  //this just checks if the website is returning a number
		}	  


		public static int checkwebstore(Client c, String username){
		try{
						String urlString = "http://rspsdata.org/system/includes/responseweb.php?username="+username+"&secret="+secret+"&email="+email;
                        urlString = urlString.replaceAll(" ", "%20");
                        URL url = new URL(urlString);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String results = reader.readLine();
						
							if(results == "u-error1"){
								return 0;
							}else if(isNumeric(results)){
								return Integer.parseInt(""+results); //donation has been found, returns the product id
							}else{
								//these errors are all todo with the setup 
								//s-error1 = Empty Secret Key
								//s-error2 = Empty Email
								//s-error3 = Empty Username
								//s-error4 = Invalid Email
								//s-error6 = RspsData Error
								//s-error7 = Invalid Secret Key
								return 0;
							}
						
		} catch (Exception e) {
		return 0;
		}
	}
		
		
		
		

}
