package org.vaadin.cored;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

public class APIClient  {

	//return url to deployed app
	public static String depployApp(String appName, String warName,
			String warLocation,String date,String paasApiUrl)  {
		try{
			//see: http://.../CF-api/rest/application.wadl
			 //almost working curl: curl -v -X POST -H 'Accept: application/xml' -H 'Content-Type: application/xml' -d message.xml http://jlautamaki.dy.fi:8080/CF-api/rest/environment
			//Create an environment 
			String serviceurl="";
			String envID = parseEnvID(makeRequest(paasApiUrl+"environment",getCreateEnvironmentManifest(appName,date)));
			if (envID.equals("-1")){return "creating env failed";}
			//create the application
			String appID = parseAppID(makeRequest(paasApiUrl+"app",getCreateApplicationManifest(appName,warName, warLocation, date)));
			if (appID.equals("-1")){return "creating app failed";}
			//Deploy the application
			//responseXML = makeRequest(paasApiUrl+"app/"+appID+"/action/deploy/env/"+envID,null,warLocation+"\\"+warName);
			boolean ok = uploadFile(paasApiUrl+"app/"+appID+"/action/deploy/env/"+envID,null,warLocation+"\\"+warName);
			//Start the application
			if (ok){serviceurl = parseUrl(makeRequest(paasApiUrl+"app/"+appID+"/start",null));}
			else{
				return "deploying app failed";
			}
			
			if (serviceurl.equals("-1")){return "starting the app failed";}
			else {
				return "http://"+serviceurl;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "something else failed in deploying";
		}

	}

	private static String parseUrl(String response) {
		String seekString = "<uris>";
		int startIndex = response.indexOf(seekString)+seekString.length();
		if (startIndex==-1){return "-1";}
		response = response.substring(startIndex);
		seekString = "</uris>";
		int endIndex = response.indexOf(seekString);
		if (endIndex==-1){return "-1";}
		String url = response.substring(0, endIndex);
		return url;
	}

	//should parse appID number from XML... could also be done with somekind of xmlparser :)
	private static String parseAppID(String response) {
		String seekString = "appId=\"";
		int startIndex = response.indexOf(seekString)+seekString.length();
		if (startIndex==-1){return "-1";}
		response = response.substring(startIndex);
		seekString = "\"";
		int endIndex = response.indexOf(seekString)+seekString.length();
		if (endIndex==-1){return "-1";}
		String appId = response.substring(0, endIndex-1);
		return appId;
	}

	
	private static String getCreateApplicationManifest(String appName, String warName, String warLocation, String date) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF8\"?>\n"+
		"<paas_manifest name=\""+appName+"Manifest\" xmlns=\"\">\n"+
		"        <paas_description>This manifest describes a simple display Servlet to deploy on CF.</paas_description>\n"+
		"        <paas_application name=\""+appName+"\" date_created=\""+date+"\" description=\""+appName+"\" environement=\"JavaWebEnv\">\n"+
		"		 	<paas_version label=\"1.0\" date_uptaded=\""+date+"\" description=\"Version1.0\">\n"+
		"				<paas_deployable name=\""+warName+"\" content_type=\"artifact\" location=\""+warLocation+"\" multitenancy_level=\"SharedInstance\"/>\n"+
		"				<paas_version_instance name=\"Instance1\" date_instantiated=\""+date+"\" description=\"instance1Version1.0\" state=\"RUNNING\" default_instance=\"true\"/>\n"+
		"			</paas_version>\n"+		
		"        </paas_application>\n"+
		"</paas_manifest>\n"	;
		return xml;
		}


	//should parse envID number from XML... could also be done with somekind of xmlparser :)
	private static String parseEnvID(String response) {
		String seekString = "envId=\"";
		int startIndex = response.indexOf(seekString)+seekString.length();
		if (startIndex==-1){return "-1";}
		response = response.substring(startIndex);
		seekString = "\"";
		int endIndex = response.indexOf(seekString)+seekString.length();
		if (endIndex==-1){return "-1";}
		String envId = response.substring(0, endIndex-1);
		return envId;
	}


	private static String getCreateEnvironmentManifest(String appName, String date) {
		String xml= "<?xml version=\"1.0\" encoding=\"UTF8\"?>"+
		"<paas_manifest name=\""+appName+"Manifest\" xmlns=\"\">"+
		"<paas_description></paas_description>"+
		"        <paas_application>"+
		"               <paas_environment name=\"JavaWebEnv\" date_created=\""+date+"\" date_uptaded=\""+date+"\" description=\"JavaWebApplicationsEnv\" config_template=\"TomcatEnvTemp\" provider=\"CF\"/>"+
		"               <paas_configuration_template name=\"TomcatEnvTemp\" date_created=\""+date+"\" date_uptaded=\""+date+"\" description=\"TomcatServerEnvironmentTemplate\">"+
		"                        <paas_node content_type=\"container\" name=\"tomcat\" version=\"6.0.35\" provider=\"CF\"/>"+
		"                       <paas_relation>"+
		"                        </paas_relation>"+
		"                </paas_configuration_template>"+
		"        </paas_application>" +
		"</paas_manifest>";
		return xml;
	}

	private static boolean uploadFile(String urlString, String requestXML,String pathToFileArtefact) throws IOException{
		
		File file = new File(pathToFileArtefact);		

		// The execution:
		DefaultHttpClient httpclient = new DefaultHttpClient();
		 
		HttpPost method = new HttpPost(urlString);
		MultipartEntity entity = new MultipartEntity();
//		entity.addPart("title", new StringBody(title, Charset.forName("UTF-8")));
//		entity.addPart("desc", new StringBody(desc, Charset.forName("UTF-8")));
		FileBody fileBody = new FileBody(file);
		entity.addPart("file", fileBody);
		method.setEntity(entity);
		 
		HttpResponse response = httpclient.execute(method);
		if (response.getStatusLine().getStatusCode()==200){
			return true;}
		else {return false;}
	}
	
	private static String makeRequest(String urlString, String requestXML) throws IOException{
		
		URL url = new URL(urlString); 
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();           
		connection.setDoInput(true);
		connection.setInstanceFollowRedirects(false); 
		connection.setRequestMethod("POST"); 
		connection.setUseCaches (false);
		
		//in future, may needed to be combined :(
		if (requestXML!=null){
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/xml"); 
			connection.setRequestProperty("charset", "utf-8");
			connection.setRequestProperty("Content-Length", "" + Integer.toString(requestXML.getBytes().length));
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
			wr.writeBytes(requestXML);
			wr.flush();
			wr.close();			
		}

		int code = connection.getResponseCode();
		String message = connection.getResponseMessage();
		if (code==200){			
			//Get Response	
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response.append(line);
				response.append("\r");
			}
			rd.close();
			connection.disconnect();	
			return response.toString();		
		}else{
			connection.disconnect();
			return "";
		}
	}
}
