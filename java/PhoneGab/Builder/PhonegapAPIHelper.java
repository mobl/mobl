package PhoneGab.Builder;
/*
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 * 
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.ConnectException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.activity.InvalidActivityException;
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

/** 
 * Example how to use multipart/form encoded POST request.
 */
public class PhonegapAPIHelper {
	private final class HashMapExtension extends HashMap<String, String> {
		/**
		 * 
		 */
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

	public static void main(String[] args) throws Exception {

		String name = "blaat";
		File dir = new File("nativejava/zip/");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		ZipHelper.zipDir("nativejava/zip/" + name + ".zip", "nativejava/src/.");

		PhonegapAPIHelper phonegap = new PhonegapAPIHelper();
		phonegap.setCriedentials("chrismelman@hotmail.com", "weetikveel");
		if (phonegap.TryAuthenticate()) {

			int id = phonegap.getAppId(name);
			String filelocation = "draw.zip";
			if (id == -1) {
				phonegap.createApp(name, filelocation);
				id = phonegap.getAppId(name);
			} else {
				phonegap.updateAppSource(id, filelocation);
			}
			String platform = "android";
			int seconds = 1;
			while (phonegap.checkBuildingStatusApp(id, platform).equals(
					Status.PENDING)) {
				System.out.println(platform + " build pending (" + seconds
						+ "s)");
				Thread.sleep(1000 * Math.min(seconds, 10));
				seconds += Math.min(seconds, 10);
			}
			switch (phonegap.checkBuildingStatusApp(id, platform)) {
			case COMPLETE:
				break;
			case ERROR:
				phonegap.getBuildError(id, platform);
				break;
			case NULL:
				throw new InvalidParameterException(
						"probably the platform doesn't have a good key for building");
			default:
				throw new InvalidActivityException(
						"Something interfered with the building process");
			}
			phonegap.getApp(name, id, platform,".");

		} else {
			throw new AuthenticationException(
					"username/password combination is invalid");
		}

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
