package PhoneGab.Builder;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.ConnectException;
import java.security.InvalidParameterException;
import java.util.Arrays; 
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.security.sasl.AuthenticationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PhonegapAPIHelper {
	private final class HashMapExtension extends HashMap<String, String> {

		private static final long serialVersionUID = -5958277237132598851L;

		{
			put("android", "apk");
			put("ios", "ipa");
			put("blackberry", "jad");
			put("symbian", "wgz"); 
			put("webos", "ipk");
		}
	}

	private DefaultHttpClient httpclient = new DefaultHttpClient();
	private Gson gson = new Gson();
	private JsonParser parser = new JsonParser(); 
	private final String[] platforms = { "android", "ios", "blackberry",
			"symbian", "webos" };
	private final Map<String, String> extensionMap = new HashMapExtension();

	public PhonegapAPIHelper() {
		super();
	}

	public void getBuildError(int id, String platform) throws NamingException,
			IllegalStateException, IOException {
		if (!Arrays.asList(platforms).contains(platform)) {
			throw new NamingException("unsupported platfom :" + platform);
		}

		HttpGet httpget = new HttpGet("https://build.phonegap.com/api/v1/apps/"
				+ id + "/");
		HttpResponse response = httpclient.execute(httpget);

		if (response == null) {
			throw new ConnectException("Could not connect to the server");
		}

		JsonObject asJsonObject = getJsonFromResponse(response)
				.getAsJsonObject();
		if (response.getStatusLine().getStatusCode() == 401) {
			System.out.println(asJsonObject.get("error"));

			throw new AuthenticationException(
					"username/password combination is invalid "+ asJsonObject.get("error").toString() );

		} else if (response.getStatusLine().getStatusCode() != 200) {
			System.out.println(asJsonObject.get("error"));
			throw new ConnectException("Could not connect to the server " + asJsonObject.get("error").toString());
		} else if (asJsonObject.get("error").getAsJsonObject().has(platform)) {
			System.out.println(asJsonObject.get("error").getAsJsonObject()
					.get(platform));
			throw new Error(asJsonObject.get("error").getAsJsonObject()
					.get(platform).toString());
		} else {
			throw new InvalidParameterException(
					"there is no error for platform: " + platform);
		}

	}

	public void getApp(String name, int id, String platform, String path) throws NamingException, ClientProtocolException, IOException {
    	if(!Arrays.asList(platforms).contains(platform)){
    		throw new NamingException("unsupported platfom :" + platform);
    	}
    	
    	HttpGet httpget = new HttpGet("https://build.phonegap.com/api/v1/apps/"+ id + "/"+ platform);
		 HttpResponse response = httpclient.execute(httpget);

		 if(response == null){
			 throw new ConnectException("Could not connect to the server");
		 }
		 if (response.getStatusLine().getStatusCode() == 401){
			System.out.println(getJsonFromResponse(response).getAsJsonObject().get("error"));
		 }
		 else{
			 File file = new File(path+"/native/" + platform + "/");
			 if(!file.exists()){
				 file.mkdirs();
			 }
			 file = new File(path + "/native/" + platform + "/"+ name + "." +extensionMap.get(platform) );
			 response.getEntity().writeTo(new FileOutputStream(file));
			 
		 }

	}

	public Status checkBuildingStatusApp(int id, String platform)
			throws IllegalStateException, IOException {
		if (!Arrays.asList(platforms).contains(platform)) {
			throw new InvalidParameterException("unsupported platfom :" + platform);
		}

		HttpGet httpget = new HttpGet("https://build.phonegap.com/api/v1/apps/"
				+ id + "/");
		HttpResponse response = httpclient.execute(httpget);
		if (response == null) {
			throw new ConnectException("Could not connect to the server");
		}

		JsonObject asJsonObject = getJsonFromResponse(response)
				.getAsJsonObject();
		if (response.getStatusLine().getStatusCode() == 401) {
			System.out.println(asJsonObject.get("error"));

			return Status.INVALID;
		} else if (response.getStatusLine().getStatusCode() != 200) {
			System.out.println(asJsonObject.get("error"));
			throw new ConnectException("Could not connect to the server:" + asJsonObject.get("error").toString() );
		} else if (asJsonObject.get("status").getAsJsonObject().get(platform)
				.isJsonNull()) {
			return Status.NULL;
		} else {
			return Status.valueOf(asJsonObject.get("status").getAsJsonObject()
					.get(platform).getAsString().toUpperCase());

		}
	}

	public void updateAppSource(int id, String filelocation)
			throws InvalidNameException, ClientProtocolException, IOException {
		File file = checkFile(filelocation);
		HttpPut httpput = new HttpPut("https://build.phonegap.com/api/v1/apps/"
				+ id);
		FileBody bin = new FileBody(file);
		MultipartEntity reqEntity = new MultipartEntity(
				HttpMultipartMode.BROWSER_COMPATIBLE);
		reqEntity.addPart("file", bin);
		httpput.setEntity(reqEntity);
		HttpResponse response = httpclient.execute(httpput);

		if (response == null) {
			throw new ConnectException("Could not connect to the server");
		}
		if (response.getStatusLine().getStatusCode() == 401) {
			System.out.println(getJsonFromResponse(response).getAsJsonObject()
					.get("error"));
			throw new AuthenticationException(
					"username/password combination is invalid");
		} else if (response.getStatusLine().getStatusCode() != 200) {
			System.out.println(getJsonFromResponse(response).getAsJsonObject()
					.get("error"));
			throw new ConnectException("Could not connect to the server");
		} else {
			System.out.println(getJsonFromResponse(response).toString());
		}
	}

	public void createApp(String name, String filelocation)
			throws InvalidNameException, ClientProtocolException, IOException {
		File file = checkFile(filelocation);
		HttpPost httppost = new HttpPost(
				"https://build.phonegap.com/api/v1/apps");

		FileBody bin = new FileBody(file);

		StringBody Data = new StringBody(gson.toJson(new Data(name)).toString());

		MultipartEntity reqEntity = new MultipartEntity(
				HttpMultipartMode.BROWSER_COMPATIBLE);
		reqEntity.addPart("data", Data);
		reqEntity.addPart("file", bin);
		httppost.setEntity(reqEntity);

		HttpResponse response = httpclient.execute(httppost);

		if (response == null) {
			throw new ConnectException("Could not connect to the server");
		}
		if (response.getStatusLine().getStatusCode() == 401) {
			System.out.println(getJsonFromResponse(response).getAsJsonObject()
					.get("error"));
			throw new AuthenticationException(
					"username/password combination is invalid");
		} else if (response.getStatusLine().getStatusCode() != 201) {
			System.out.println(getJsonFromResponse(response).getAsJsonObject()
					.get("error"));
			throw new ConnectException("Could not connect to the server");
		} else {
			System.out.println(getJsonFromResponse(response).toString());
		}

	}

	@SuppressWarnings("unused")
	private void printResponse(HttpResponse response) throws IOException {
		HttpEntity resEntity = response.getEntity();
		System.out.println(response.getStatusLine());
		if (resEntity != null) {
			System.out.println("Response content length: "
					+ resEntity.getContentLength());
			Scanner x = new Scanner(resEntity.getContent());
			while (x.hasNextLine()) {
				System.out.println(x.nextLine());

			}
		}
	}

	private File checkFile(String filelocation) throws InvalidNameException {
		File file = new File(filelocation);
		if (!file.isFile()
				|| !(file.getName().equals("index.html")
						|| file.getName().endsWith(".zip") || file.getName()
						.endsWith(".tar.gz"))) {
			throw new InvalidParameterException(
					"the file should be a zip/tar.gz file or index.html instead of: "
							+ file.getName());
		}
		return file;
	}

	public int getAppId(String name) throws ClientProtocolException,
			IOException {
		HttpGet httpget = new HttpGet("https://build.phonegap.com/api/v1/apps");
		HttpResponse response = httpclient.execute(httpget);
		if (response == null) {
			throw new ConnectException("Could not connect to the server");
		}
		if (response.getStatusLine().getStatusCode() == 401) {
			System.out.println(getJsonFromResponse(response).getAsJsonObject()
					.get("error"));

			return -1;
		} else if (response.getStatusLine().getStatusCode() != 200) {
			System.out.println(getJsonFromResponse(response).getAsJsonObject()
					.get("error"));
			throw new ConnectException("Could not connect to the server");
		} else {
			JsonArray apps = getJsonFromResponse(response).getAsJsonObject() 
					.get("apps").getAsJsonArray();
			for (int i = 0; i < apps.size(); i++) {
				if (apps.get(i).getAsJsonObject().get("title").getAsString()
						.equals(name)) {
					return apps.get(i).getAsJsonObject().get("id").getAsInt();
				}
			}
			return -1;
		}

	}

	public void setCriedentials(String username, String password) {
		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope("build.phonegap.com", 443),
				new UsernamePasswordCredentials(username, password));

	}

	public boolean TryAuthenticate() throws ClientProtocolException,
			IOException {
		HttpGet httpget = new HttpGet("https://build.phonegap.com/api/v1/me");
		HttpResponse response = httpclient.execute(httpget);

		if (response == null) {
			throw new ConnectException("Could not connect to the server");
		}
		if (response.getStatusLine().getStatusCode() == 401) {
			System.out.println(getJsonFromResponse(response).getAsJsonObject()
					.get("error"));

			return false;
		} else if (response.getStatusLine().getStatusCode() != 200) {
			System.out.println(getJsonFromResponse(response).getAsJsonObject()
					.get("error"));
			throw new ConnectException("Could not connect to the server");
		} else {
			EntityUtils.consume(response.getEntity());
			return true;
		}

	}

	private JsonElement getJsonFromResponse(HttpResponse response)
			throws IllegalStateException, IOException {
		JsonElement json = null;
		if (response.getEntity() != null) {
			Scanner x = new Scanner(response.getEntity().getContent()); 
			if (x.hasNextLine()) {
				json = parser.parse(x.nextLine());
			}
		}
		EntityUtils.consume(response.getEntity());
		if (json == null) {
			throw new ParseException("could not parse the json");
		}
		return json;
	}

	private class Data {
		@SuppressWarnings("unused")
		private String create_method = "file";
		@SuppressWarnings("unused")
		private String title = "test";
		@SuppressWarnings("unused")
		private String version = "1.0";
   
		public Data(String title) { 
			super();
			this.title = title;
		}

	}

}
