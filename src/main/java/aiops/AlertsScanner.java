package aiops;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;




// eyJrIjoiS3I2MVZXcXJCSEVKb1JDWjh5V01YUkl3dTV3UWRLMTAiLCJuIjoiQWxlcnRpbmciLCJpZCI6MX0= 5000 C:/Users/Vamsi Krishna Yepuri/Desktop/alerts.json
public class AlertsScanner {
	
	 
	static String authHeaderValue= "";
	static int updateInterval = 5000;
	static String fileLocation = "";
	static String influxDbUrl="";
	static String influxDbName="";
	
	
	public static void main(String a[]){
		
	     authHeaderValue = "Bearer "+a[0];
		 updateInterval = Integer.parseInt(a[1]);
		 fileLocation = a[2];
		 influxDbUrl = a[3];
		 influxDbName = a[4];
		 
		 System.out.println("Authorization Header:" +authHeaderValue + ",\nUpdate Interval:"+updateInterval+",\nFile Location:"+fileLocation+",\nInfluxDB Url:"+influxDbUrl+",\nInfluxDB Name:"+influxDbName+"\n\n\n");
		 
		//create a task
		TimerTask tt = new TimerTask() {  
		    @Override  
		    public void run() {         
		    	
				   File file = new File(fileLocation); 		   
				   try {
						   BufferedReader br = new BufferedReader(new FileReader(file));
						   JSONArray arr = new JSONArray(getStringContentFromBufferedReader(br));
					       for(int i=0; i< arr.length();i++) {
					    	  JSONObject json = (JSONObject) arr.get(i); 
				              updateAlertCountToInflux(json.get("dashboardName").toString(), json.get("alertApiUrl").toString()); 
					       }
				   }
				   catch(FileNotFoundException e) {					   			   
					   System.out.println("Cannot find the file : "+file.getAbsolutePath());
					}	  
			       	
		    	
		    };  
		};  
		
		//run task for evary x seconds using Timer
		Timer t = new Timer();  
		t.schedule(tt, 0,updateInterval); 
			      
	}
	
	
	
	
	static void updateAlertCountToInflux(String dashboardName,String alertApiUrl) {		
				try {
					 HttpURLConnection myURLConnection;
					 URL myURL = new URL(alertApiUrl);
					 myURLConnection = (HttpURLConnection)myURL.openConnection();
					 myURLConnection.setRequestProperty ("Authorization", authHeaderValue);
					 myURLConnection.setRequestMethod("GET");
					 myURLConnection.setRequestProperty("Content-Type", "application/json");
					 myURLConnection.setConnectTimeout(2000);
					 
					 BufferedReader in = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));	
					 JSONArray responseArray = new JSONArray(getStringContentFromBufferedReader(in));		 
					 int alertsCount = 0;
					 for(int i=0;i<responseArray.length();i++) {
						 JSONObject dashboard = (JSONObject) responseArray.get(i);
					     if (dashboard.get("state").equals("alerting")) {
					    	 alertsCount++;
					     }
					 }		
					 writeCountToInfluxDB(alertsCount,dashboardName);
				} 
				catch (MalformedURLException e) {
					e.printStackTrace();
					System.out.println("In updateAlertCountToInflux  --> Dashboard Name : "+dashboardName+", Url : "+alertApiUrl);
				}catch (IOException e) {
					e.printStackTrace();
					System.out.println("In updateAlertCountToInflux  --> Dashboard Name : "+dashboardName+", Url : "+alertApiUrl);
				} 
				
	 }
	 
	
	
	
	
	 static void writeCountToInfluxDB(int alertsCount,String dashboardName) {		 
		
		 String influxDbWriteUrl = influxDbUrl+"/write?db="+influxDbName;
		 long timeStamp = System.currentTimeMillis() * 1000000;
		 String data = "dashboard,name="+dashboardName+" alertsCount="+alertsCount+" "+timeStamp;
		 //System.out.println(influxDbWriteUrl +"->"+data);
		  
			try {
				 URL myURL = new URL(influxDbWriteUrl);
				 HttpURLConnection myURLConnection = (HttpURLConnection)myURL.openConnection(); 
				 myURLConnection.setRequestMethod("POST");
				 myURLConnection.setRequestProperty("Content-Type", "text/plain");
				 myURLConnection.setDoOutput(true);
				 OutputStream os = myURLConnection.getOutputStream();
				 os.write(data.getBytes("UTF-8"));
				 os.flush();
				 os.close();
			} catch (MalformedURLException e) {
				e.printStackTrace();
				System.out.println("In writeCountToInfluxDB() --> Dashboard : "+dashboardName);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("In writeCountToInfluxDB() --> Dashboard : "+dashboardName);
			}
	 }

	 
	 
	 static String getStringContentFromBufferedReader(BufferedReader in) {
		 StringBuffer response = new StringBuffer();
		 try {
				 String inputLine;				 
				 while ((inputLine = in.readLine()) != null) 
					response.append(inputLine);				 
				 in.close();	
		}
		 catch (IOException e) {
			System.out.println(e.getMessage());
		}
		return response.toString(); 
	 }
}
