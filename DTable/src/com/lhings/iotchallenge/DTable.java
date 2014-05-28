package com.lhings.iotchallenge;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.lhings.library.Event;
import com.lhings.library.LhingsDevice;
import com.lhings.library.Stats;

public class DTable extends LhingsDevice {
	
    
	private final String availableCardKey = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
	private final String taxiCardKey = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
	private final String coworkingApiKey = "1314064a-32f5-41dd-bf27-7469d3ae9dd0";

	private String user;
	private String userApikey;
	private boolean available = true;

	private boolean sendCheckedIn = false;
	private boolean sendCheckedOut = false;
	private boolean sendTaxiRequested = false;
	private boolean sendAvailable = false;
	private boolean sendNotAvailable = false;

    private boolean checkIn = false;
    
    static Thread rfid_thread;
    static RP_Rfid rfid;
    
    private boolean on=false;
    
	public DTable() {
		// Co-working space credenti
        super("david@lhings.com", "coworking", 5000, "DTable");

	}

	@Override
	public void setup() {
        //First, setup the RFID
        setupRFID();
	
	}

	@Override
	public void loop() {
		try {
            //Since we created a thread for the RFID, we check if the credentials are corret/not null in the update
            updateRfid();
            //give just a bit of time to breath
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

    private void setupRFID() {
        //Created a thread so its not blocking the app the RFID
		rfid = new RP_Rfid();
		rfid_thread = new Thread(rfid,"RFID Table");
		rfid_thread.start();
	}
    
    private void updateRfid() {
		String apikey = rfid.getStringApiKey();
		if(apikey != null){
            if(!checkIn){
                if (apikey.equals(taxiCardKey)){
                    return;
                }else if(apikey.equals(availableCardKey)){
                    return;
                }
                
                doCheckin(apikey);
                userApikey = apikey;
                checkIn= true;
                sendCheckedIn = true;
                // we set up the AVAILABLE to true since we just checked in and we show co-workers that are Available
                setAvailable(true);
                toogleLight();
            }else{
                if(userApikey.equals(apikey)){
                    doCheckout();
                    checkIn = false;
                    sendCheckedOut= true;
                }else if (apikey.equals(taxiCardKey)){
                    requestTaxi();
                }else if(apikey.equals(availableCardKey)){
                    toggleAvailable();
                }
            
            }

        }
	}


    
    // ************************************
    // ************* EVENTS ***************
    // ************************************
	@Event(name="CheckedIn")
	public String checkedIn(){
		if (sendCheckedIn){
			sendCheckedIn = false;
			return userApikey;
		} else
			return null;
	}

	@Event(name="CheckedOut")
	public String checkedOut(){
		if (sendCheckedOut){
			sendCheckedOut = false;
			return "checkout";
		} else
			return null;
	}

	@Event(name="TaxiRequested")
	public String taxiRequested(){
		if (sendTaxiRequested){
			sendTaxiRequested = false;
			return "";
		} else
			return null;
	}

	@Event(name="Available")
	public String available(){
		if (sendAvailable){
			sendAvailable = false;
			return "";
		} else
			return null;
	}

	@Event(name="NotAvailable")
	public String notAvailable(){
		if (sendNotAvailable){
			sendNotAvailable = false;
			return "";
		} else
			return null;
	}

    // ************************************
    // ************* STATUS ***************
    // ************************************
    @Stats(name ="checkedIn", type="boolean")
    public boolean isCheckedIn(){
        return checkIn;
    }

	@Stats(name = "availability", type = "boolean")
	public boolean isAvailable(){
		return available;
	}
	
	@Stats(name = "light", type = "boolean")
	public boolean lightStatus(){
		return on;
	}

	// ************************************
    // ************* ACTIONS **************
    // ************************************

    @Action (name = "toggleAvailable", description = "Toggle between green/red light of availability of user", argumentNames = {}, argumentTypes = {})
	private void toggleAvailable(){
		setAvailable(!available);
    
    }

    @Action (name = "requestTaxi", description = "Requests a taxi", argumentNames = {}, argumentTypes = {})
	private void requestTaxi(){
        System.out.println("TODO: Request a Taxi!");
	}

    @Action (name = "toggleLight", description = "Toggle light on/off ", argumentNames = {}, argumentTypes = {})
    private void toggleLight(){
        setLightOn(!on);
    }
    
	private void doCheckin(String apikey){
        sendApikeyToCoffee(apikey);
        setLightOn(true);
        setAvailable(true);
        getDevicesFromUser(apikey);//and send welcome, send to desktop app apikey
        System.out.println("Checked in done with apikey+"+apikey);
	}
    private void setLightOn(boolean value){
        if (this.on == false && value == true){
			callWebService_lightOnOff("{\"on\":true, \"hue\":0}");
		}
		else if (this.on == true && value == false){
			callWebService_lightOnOff("{\"on\":false}");
		}
        
		this.on = value;
        
        if(this.on)
            System.out.println("Light ON");
        else
            System.out.println("Light OFF");
    }
    
	private void doCheckout(String apikey){
		// llamamos al shutdown de DTable y DLamp para terminarlos, ponemos offline DCoffeeMakerU
		// se utiliza el servicio Device.endSession para poner offline DCoffeMakerU
        sendGoodByeMessage(apikey);
        System.out.println("checkout!");
	}
    
    private void getDevicesFromUser(String apikey){
        System.out.print("TODO: Send Apikey of Coworking to Pereda: i send to plughlings welcome text and Pereda will do welcome in his app");
    }
    private void sendApikeyToCoffee(String apikey){
        System.out.println("TODO: Send Apikey of user to coffeemaker");
    }
    
    private void sendGoodByeMessage(String apikey){
        System.out.println("TODO: Send goodby to Pereda and Lhings");
    }
	private void setAvailable(boolean available) {
		
		if (this.available == false && available == true){
			callWebService_available("{\"on\":true, \"hue\":25000}");//green {"on":true, "hue": 25000}
			sendAvailable = true;
		}
		else if (this.available == true && available == false){
			callWebService_available("{\"on\":true, \"hue\":1000}");//red {"on":true, "hue": 1000}
			sendNotAvailable = true;
		}
			

		this.available = available;
        
        if(this.available)
            System.out.println("I am Available right now");
        else
            System.out.println("I am NOT Available right now");
	}
    
	private void uploadDescriptor(String apikey, String uuid, String descriptor){
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpPost post = new HttpPost("https://www.lhings.com/laas/api/v1/devices/" + uuid + "/");
			post.addHeader("X-Api-Key", apikey);
			StringEntity requestBody = new StringEntity(descriptor);
			post.setEntity(requestBody);
			CloseableHttpResponse response = httpclient.execute(post);
			if (response.getStatusLine().getStatusCode() != 200) {
				System.err.println("Unable to upload descriptor for device " + uuid + ", request failed: " + response.getStatusLine());
				response.close();
				System.exit(1);
			}
			String responseBody = EntityUtils.toString(response.getEntity());
			response.close();
			
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
			System.exit(1);
		}
        
	}
	
	private String createDevice(String apikey, String devName){
            //register device
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpPost post = new HttpPost("https://www.lhings.com/laas/api/v1/devices/");
			post.addHeader("X-Api-Key", apikey);
            post.addHeader("Content-Type", "application/json");
            
			StringEntity requestBody = new StringEntity("{ \"name\": \"deviceName\", \"value\": \"" + devName + "\"}");
			post.setEntity(requestBody);
			CloseableHttpResponse response = httpclient.execute(post);
			if (response.getStatusLine().getStatusCode() != 200) {
				System.err.println("Unable to register device Lamp, request failed: " + response.getStatusLine());
				response.close();
				System.exit(1);
			}
			String responseBody = EntityUtils.toString(response.getEntity());
			response.close();
			JSONObject device = new JSONObject(responseBody);
			return device.getString("uuid");
			
			
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
			System.exit(1);
		}
		return null;
	}

        // ************* private methods (Lamp related) ***************
	private void callWebService_available(String payload) {
		
		try {
			URL hueColorService = new URL(
					"http://192.168.0.111/api/newdeveloper/lights/4/state");
			HttpURLConnection conn = (HttpURLConnection) hueColorService.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Content-Type", "application/json");

			String input = payload;

			OutputStream os = conn.getOutputStream();
			os.write(input.getBytes());
			os.flush();

			BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));

			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				System.out.println(output);
			}

			conn.disconnect();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 

    private void callWebService_lightOnOff(String payload) {
		
		try {
			URL hueColorService = new URL(
                                          "http://192.168.0.111/api/newdeveloper/lights/3/state");
			HttpURLConnection conn = (HttpURLConnection) hueColorService.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Content-Type", "application/json");
            
			String input = payload;
            
			OutputStream os = conn.getOutputStream();
			os.write(input.getBytes());
			os.flush();
            
			BufferedReader br = new BufferedReader(new InputStreamReader(
                                                                         (conn.getInputStream())));
            
			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				System.out.println(output);
			}
            
			conn.disconnect();
		} catch (Exception e) {
                // TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		// starting your device is as easy as creating an instance!!
		DTable table = new DTable();

	}

	private Map<String,String> getAllDevicesInAccount(String apikey) {
		
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpGet get = new HttpGet("https://www.lhings.com/laas/api/v1/devices/");
			get.addHeader("X-Api-Key", apikey);
			CloseableHttpResponse response = httpclient.execute(get);
			if (response.getStatusLine().getStatusCode() != 200) {
				System.err.println("Device.list request failed: " + response.getStatusLine());
				response.close();
				System.exit(1);
			}
			String responseBody = EntityUtils.toString(response.getEntity());
			response.close();
			JSONArray listDevices = new JSONArray(responseBody);
			int numElements = listDevices.length();
			Map<String,String> results = new HashMap<String, String>();
			for (int i = 0; i<numElements; i++){
				JSONObject device = listDevices.getJSONObject(i);
				String uuid = device.getString("uuid");
				String name = device.getString("name");
				results.put(name, uuid);
			}
			
			return results;
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
			System.exit(1);
		}
		return null;
	} 

}
