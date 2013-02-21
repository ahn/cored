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

	//return url to deployed app
	public static String depployApp(String appName, String warName,
			String warLocation,String date,String paasApiUrl)  {
		try{
			//Create an environment
			String url = paasApiUrl+"environment";
			HttpResponse response = makeRequest(url,getCreateEnvironmentManifest(appName,date),null);
			if (response.getStatusLine().getStatusCode()!=200){
				return "Creating environment with " + url + " failed: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
			}
			
			String xmlData = getXML(response);
			String envID = parseEnvID(xmlData);
			if (envID.equals("-1")){
				return "Creating environment with " + url + " failed; Data was: " + xmlData;
			}
			
			//create the application
			url = paasApiUrl+"app";
			response = makeRequest(url,getCreateApplicationManifest(appName,warName, warLocation, date),null);
			if (response.getStatusLine().getStatusCode()!=200){
				return "Creating app with " + url + " failed: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
			}

			xmlData = getXML(response);
			String appID = parseAppID(xmlData);
			if (appID.equals("-1")){
				return "Creating application with " + url + " failed; Data was: " + xmlData;
			}

			//Deploy the application
			url = paasApiUrl+"app/"+appID+"/action/deploy/env/"+envID;
			response = makeRequest(url,null,warLocation+"\\"+warName);
			if (response.getStatusLine().getStatusCode()!=200){
				return "Deploying app with " + url + " failed: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
			}

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
		} catch (IOException e) {
			String output = "something else failed in deploying" + e.toString(); 
			return output;
		}

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


/*	
	<?xml version="1.0" encoding="UTF8"?>
	<paas_manifest name="ServletSampleApplicationManifest" xmlns="">
	<paas_description></paas_description>
		<paas_application>
			<paas_environment name="JavaWebEnv" date_created="2012-10-10" date_uptaded="2012-10-10" description="JavaWebApplicationsEnv" config_template="TomcatEnvTemp" provider="CF"/>			
			<paas_configuration_template name="TomcatEnvTemp" date_created="2012-10-10" date_uptaded="2012-10-10" description="TomcatServerEnvironmentTemplate">
				<paas_node content_type="container" name="tomcat" version="6.0.35" provider="CF"/>
				<paas_relation>
				</paas_relation>	
			</paas_configuration_template>
		</paas_application>
	</paas_manifest>*/
	
	private static String getCreateEnvironmentManifest(String appName, String date) {
		String xml= "<?xml version=\"1.0\" encoding=\"UTF8\"?>\n"+
		"<paas_manifest name=\""+appName+"Manifest\" xmlns=\"\">\n"+
		"<paas_description></paas_description>\n"+
		"        <paas_application>\n"+
		"               <paas_environment name=\"JavaWebEnv\" date_created=\""+date+"\" date_uptaded=\""+date+"\" description=\"JavaWebApplicationsEnv\" config_template=\"TomcatEnvTemp\" provider=\"CF\"/>\n"+
		"               <paas_configuration_template name=\"TomcatEnvTemp\" date_created=\""+date+"\" date_uptaded=\""+date+"\" description=\"TomcatServerEnvironmentTemplate\">\n"+
		"                        <paas_node content_type=\"container\" name=\"tomcat\" version=\"6.0.35\" provider=\"CF\"/>\n"+
		"                       <paas_relation>\n"+
		"                        </paas_relation>\n"+
		"                </paas_configuration_template>\n"+
		"        </paas_application>\n"+
		"</paas_manifest>";
		return xml;
	}

	private static HttpResponse makeRequest(String urlString, String requestXML,String pathToFileArtefact) throws IOException{
		
		HttpPost post = new HttpPost(urlString);

		if (requestXML!=null){
			StringEntity data = new StringEntity(requestXML);
			data.setContentType("application/xml");
			post.setEntity(data);
		}
		
		if (pathToFileArtefact!=null){
			MultipartEntity entity = new MultipartEntity();
			File file = new File(pathToFileArtefact);		
//			FileEntity entity = new FileEntity(file, "binary/octet-stream");	//415 unsupported mediatype
//			FileEntity entity = new FileEntity(file, "file");	//500 internal server error

			FileBody fileBody = new FileBody(file);
			entity.addPart("file", fileBody);
			post.setEntity(entity);
		}
	
		DefaultHttpClient httpclient = new DefaultHttpClient();
		return httpclient.execute(post);
	}	
}
