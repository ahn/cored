package org.vaadin.cored;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class APIClient  {

	//return url to deployed app
	public static String depployApp(String appName, String warName,
			String warLocation,String date,String paasApiUrl) throws IOException  {
		String responseXML = "";
		//see: http://.../CF-api/rest/application.wadl
		 //almost working curl: curl -v -X POST -H 'Accept: application/xml' -H 'Content-Type: application/xml' -d message.xml http://jlautamaki.dy.fi:8080/CF-api/rest/environment
		String envID = parseEnvID(makeRequest(paasApiUrl+"environment",getCreateEnvironmentString(appName,date)));
		String appID = parseAppID(makeRequest(paasApiUrl+"app",getCreateApplicationString(appName,date)));
		responseXML = makeRequest(paasApiUrl+"app/"+appID+"/version/create",getCreateAppVersionString(appName, warName,warLocation,date));
		responseXML = makeRequest(paasApiUrl+"app/"+appID+"/version/1/instance",getCreateApplicationVersionInstanceString(appName, date));
		responseXML = makeRequest(paasApiUrl+"environment/"+envID+"/action/deploy/app/"+appID+"/version/1/instance/1", ""); // deploys the application
		String serviceurl = parseUrl(makeRequest(paasApiUrl+"app/"+appID+"/version/1/instance/1/action/start","")); //Starts the application		
		return serviceurl;
	}

	private static String parseUrl(String response) {
		String seekString = "<uris>";
		int startIndex = response.indexOf(seekString)+seekString.length();
		response = response.substring(startIndex);
		seekString = "</uris>";
		int endIndex = response.indexOf(seekString);
		String url = response.substring(0, endIndex);
		return url;
	}

	private static String getCreateApplicationVersionInstanceString(String appName, String date) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF8\"?>"+
		"<paas_manifest name=\""+appName+"Manifest\" xmlns=\"\">"+
		"<paas_description></paas_description>"+
		"        <paas_application>"+
		"                <paas_version>"+
		"                        <paas_version_instance name=\"Instance1\" date_instantiated=\""+date+"\" description=\"instance1Version1.0\" state=\"RUNNING\" default_instance=\"true\"/>"+
		"                </paas_version>"+
		"        </paas_application>"+
		"</paas_manifest>";
		return xml;
	}


	private static String getCreateAppVersionString(String appName, String warName, String warLocation, String date) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF8\"?>\n"+
		"<paas_manifest name=\""+appName+"Manifest\" xmlns=\"\">"+
		"<paas_description></paas_description>\n"+
		"        <paas_application>\n"+
		"                <paas_version label=\"1.0\" date_uptaded=\""+date+"\" description=\"Version1.0\">\n"+
		"                        <paas_deployable name=\""+warName+"\" content_type=\"artifact\" location=\""+warLocation+"\" multitenancy_level=\"SharedInstance\"/>\n"+
		"                        <paas_version_instance/>\n"+
		"                </paas_version>\n"+
		"        </paas_application>\n"+
		"</paas_manifest>";
		return xml;
	}

	//should parse appID number from XML... could also be done with somekind of xmlparser :)
	private static String parseAppID(String response) {
		String seekString = "appId=\"";
		int startIndex = response.indexOf(seekString)+seekString.length();
		response = response.substring(startIndex);
		seekString = "\"";
		int endIndex = response.indexOf(seekString)+seekString.length();
		String appId = response.substring(0, endIndex-1);
		return appId;
	}

	
	private static String getCreateApplicationString(String appName, String date) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF8\"?>"+
		"<paas_manifest name=\""+appName+"Manifest\" xmlns=\"\">"+
		"        <paas_description>This manifest describes a simple display Servlet to deploy on CF.</paas_description>"+
		"        <paas_application name=\""+appName+"\" date_created=\""+date+"\" description=\""+appName+"\" environement=\"JavaWebEnv\">"+
		"        </paas_application>"+
		"</paas_manifest>"	;
		return xml;
		}


	//should parse envID number from XML... could also be done with somekind of xmlparser :)
	private static String parseEnvID(String response) {
		String seekString = "envId=\"";
		int startIndex = response.indexOf(seekString)+seekString.length();
		response = response.substring(startIndex);
		seekString = "\"";
		int endIndex = response.indexOf(seekString)+seekString.length();
		String envId = response.substring(0, endIndex-1);
		return envId;
	}


	private static String getCreateEnvironmentString(String appName, String date) {
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

	private static String makeRequest(String urlString, String requestXML) throws IOException{
		
		URL url = new URL(urlString); 
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();           
		connection.setDoInput(true);
		connection.setInstanceFollowRedirects(false); 
		connection.setRequestMethod("POST"); 
		connection.setUseCaches (false);
		
		if (requestXML.length()>0){
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/xml"); 
			connection.setRequestProperty("charset", "utf-8");
			connection.setRequestProperty("Content-Length", "" + Integer.toString(requestXML.getBytes().length));
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
			wr.writeBytes(requestXML);
			wr.flush();
			wr.close();			
		}
		
		
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
		
		
		/*
		// Get the method name
		String method = request.getParameter("method");
		// Get the method path
		String path = request.getParameter("path");
		// Get the request body
		String body = request.getParameter("body");
		// Get the PaaS name (0 CloudFoundry, 1 Openshift, -1 not specfied)
		paas = Integer.parseInt(request.getParameter("paas"));

		if (method == null || method.equals("") || path == null
				|| path.equals("")) {
			request.setAttribute("status", 404);
			request.setAttribute("output",
					"Select an Action from the proposed list.");
			request.getRequestDispatcher("/index.jsp").forward(request,
					response);
		} else if (paas==-1) {
			request.setAttribute("status", 404);
			request.setAttribute("output",
					"Select a PaaS Solution from the proposed list.");
			request.getRequestDispatcher("/index.jsp").forward(request,
					response);					
				}
		else {

			ClientConfig config = new DefaultClientConfig();
			Client client = Client.create(config);
			client.setConnectTimeout(0);
			WebResource service = client.resource(getBaseURI());

			// Get the type of the methods (i.e. GET, POST,...)
			String[] methodSplit = method.split("-");
			method = methodSplit[0];

			ClientResponse cr = null;
			String output = null;
			if (method.equals("GET")) {
				cr = service.path(path).type(MediaType.APPLICATION_XML)
						.get(ClientResponse.class);
			} else if (method.equals("POST")) {
				cr = service.path(path).type(MediaType.APPLICATION_XML)
						.entity(body).post(ClientResponse.class);
			} else if (method.equals("DELETE")) {
				cr = service.path(path).type(MediaType.APPLICATION_XML)
						.delete(ClientResponse.class);
			}

			request.setAttribute("status", cr.getStatus());
			if (cr.getStatus() == 200 || cr.getStatus() == 202)// if the
				// response will be an xml descriptor, format it
				request.setAttribute("output",
						prettyFormat(cr.getEntity(String.class), 2));
			else
				request.setAttribute("output", cr.getEntity(String.class));
			request.getRequestDispatcher("/index.jsp").forward(request,
					response);
		}*/
		
	}
}
