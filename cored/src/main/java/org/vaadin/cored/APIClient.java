package org.vaadin.cored;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

public class APIClient  {

	private static DefaultHttpClient httpclient = new DefaultHttpClient();;

	//return url to deployed app
	public static String depployApp(String appName, String warName,
			String warLocation,String deployLocation,String date,String paasApiUrl,String memory)  {
		try{
			HttpResponse response = null;
			final String appID;
			final String envID;
			String url = "";
			String xmlData = "";
			
			//this is for testing without creating new envs or apps
			boolean createEnvAndApp=true;
			if (createEnvAndApp){
				//Create an environment
				url = paasApiUrl+"environment";
				String manifest = createManifest(appName,warName, warLocation, deployLocation, date,memory);
				response = makeRequest(url,manifest,null);
				if (response.getStatusLine().getStatusCode()!=200){
					return "Creating environment with " + url + " failed: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
				}
				xmlData = getXML(response);
				envID = parseEnvID(xmlData);
				if (envID.equals("-1")){
					return "Creating environment with " + url + " failed; Data was: " + xmlData;
				}
				
				//create the application
				url = paasApiUrl+"app";
				response = makeRequest(url,manifest,null);
				if (response.getStatusLine().getStatusCode()!=200){
					return "Creating app with " + url + " failed: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
				}
				xmlData = getXML(response);
				appID = parseAppID(xmlData);
				if (appID.equals("-1")){
					return "Creating application with " + url + " failed; Data was: " + xmlData;
				}
			}else{
				//these are for testing... Remember to change
				appID="1";
				envID="1";
			}
			
			//Deploy the application
			url = paasApiUrl+"app/"+appID+"/action/deploy/env/"+envID;
			response = makeRequest(url,null,new File(warLocation,warName));
			xmlData = getXML(response);
			if (response.getStatusLine().getStatusCode()!=200){
				return "Deploying app with " + url + " failed: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
			}
			
			//Start the application
			url = paasApiUrl+"app/"+appID+"/start";
			response = makeRequest(url,null,null);
			if (response.getStatusLine().getStatusCode()!=200){
				return "Starting app with " + url + " failed: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
			}
			xmlData = getXML(response);
			String serviceurl = parseUrl(xmlData);
			if (appID.equals("-1")){
				return "Starting application with " + url + " failed; Data was: " + xmlData;
			}
			
			return "http://"+serviceurl;			
			
		} catch (Exception e) {
			String output = "something else failed in deploying" + e.toString(); 
			return output;
		}
	}

	private static String createManifest(String appName, String warName,
			String warLocation, String deployLocation, String date, String memory) {
		String xml = ""+
		"<?xml version=\"1.0\" encoding=\"UTF8\"?>\n"+
		"<paas_application_manifest name=\"" + appName +"Manifest\">\n"+
		"<description>This manifest describes a " + appName + ".</description>\n"+
		"	<paas_application name=\"" + appName + "\" environement=\"JavaWebEnv\">\n"+
		"		<description>"+appName+" description.</description>\n"+
		"		<paas_application_version name=\"version1.0\" label=\"1.0\">\n"+
		"			<paas_application_deployable name=\""+warName+"\" content_type=\"artifact\" location=\""+deployLocation+"\" multitenancy_level=\"SharedInstance\"/>\n"+
		"			<paas_application_version_instance name=\"Instance1\" initial_state=\"1\" default_instance=\"true\"/>\n"+
		"		</paas_application_version>\n"+
		"	</paas_application>\n"+
		"	<paas_environment name=\"JavaWebEnv\" template=\"TomcatEnvTemp\">\n"+			
		"		<paas_environment_template name=\"TomcatEnvTemp\" memory=\"" + memory + "\">\n"+
		"			<description>TomcatServerEnvironmentTemplate</description>\n"+
		"			<paas_environment_node content_type=\"container\" name=\"tomcat\" version=\"\" provider=\"CF\"/>\n"+			
		"		</paas_environment_template>\n"+
		"	</paas_environment>\n"+
		"</paas_application_manifest>\n";		
		return xml;
	}

	private static String getXML(HttpResponse response) {
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		try {
			response.getEntity().writeTo(outstream);
		} catch (IOException e) {
			return "IOException";
		}
		byte [] responseBody = outstream.toByteArray();
		String responseBodyString = new String(responseBody);
		return responseBodyString;
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

	private static HttpResponse makeRequest(String urlString, String requestXML,File file) throws IOException{
		
		HttpPost post = new HttpPost(urlString);

		//create new post with xml data
		if (requestXML!=null){
			StringEntity data = new StringEntity(requestXML);
			data.setContentType("application/xml");
			post.setEntity(data);
		}//create new post with filecontent  
		else if (file!=null){			
			MultipartEntity entity = new MultipartEntity();	// Should work! 200 OK
			FileBody fileBody = new FileBody(file);
			entity.addPart("file", fileBody);	
			post.setEntity(entity);
		}

		//make post
		return httpclient.execute(post);
	}	
}
