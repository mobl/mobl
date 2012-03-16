package PhoneGab.strategies;


import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.activity.InvalidActivityException;
import javax.security.sasl.AuthenticationException;



import mobl.Activator;
import mobl.strategies.uglify_0_0;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import PhoneGab.Builder.PhonegapAPIHelper;
import PhoneGab.Builder.Status;
import PhoneGab.Builder.ZipHelper;


public class build$On$Cloud_0_4 extends Strategy {  

    public static build$On$Cloud_0_4 instance = new build$On$Cloud_0_4();
     
    @Override
    public IStrategoTerm invoke( final Context context,   IStrategoTerm current,  IStrategoTerm usernameTerm,  IStrategoTerm passwordTerm,  IStrategoTerm appnameTerm,  IStrategoTerm platformsTerm) {
    	 final String path = uglify_0_0.getStringFromTerm(current);
		 final String username = uglify_0_0.getStringFromTerm(usernameTerm);
		 final String password = uglify_0_0.getStringFromTerm(passwordTerm);
		 final String appname = uglify_0_0.getStringFromTerm(appnameTerm);
		 final ArrayList<String> platforms = new ArrayList<String>();
		 for (IStrategoTerm term: platformsTerm.getAllSubterms()){
			 platforms.add(uglify_0_0.getStringFromTerm(term));
		 }
    	Job job = new Job("native build") {

			 
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
			    	monitor.beginTask("native build", 100);
				 	try{	
						File dir = new File(path+"/native/zip/");
						if (!dir.exists()) { 
							dir.mkdirs();
						} 
						
						monitor.worked(1);
						monitor.subTask("zippig dir");
						context.getIOAgent().printError("zipping dir");
						ZipHelper.zipDir(path+"/native/zip/" + appname + ".zip", path+"/native/src/.");
						monitor.worked(9);
						PhonegapAPIHelper phonegap = new PhonegapAPIHelper();
						monitor.subTask("checking Credentials");
						phonegap.setCriedentials(username,password);
						context.getIOAgent().printError("checking Credentials");
						if (phonegap.TryAuthenticate()) {
							monitor.worked(5);
							context.getIOAgent().printError("Ok");
							monitor.subTask("get Application ID");
							context.getIOAgent().printError("get Application ID");
							int id = phonegap.getAppId(appname);
							monitor.worked(5);
							String filelocation =path+"/native/zip/" + appname + ".zip";
							if (id == -1) {
								monitor.subTask("Create New Application");
								context.getIOAgent().printError("Create New Application");
								phonegap.createApp(appname, filelocation);
								id = phonegap.getAppId(appname);
								monitor.worked(15);
							} else {
								monitor.subTask("Update Source");
								context.getIOAgent().printError("Update Source");
								phonegap.updateAppSource(id, filelocation);
								monitor.worked(15);
							}
							monitor.worked(1);
							int restBuild = 38;
							int restDownload = 20;
							int i = platforms.size();
							for(String platform: platforms){
								int seconds = 1;
								context.getIOAgent().printError("Start Building");
								monitor.subTask("Start Building");
								
								monitor.subTask("build pending");
								while (phonegap.checkBuildingStatusApp(id, platform).equals(
										Status.PENDING)&&!monitor.isCanceled()) {
									context.getIOAgent().printError(platform + " build pending (" + seconds
											+ "s)");
									monitor.subTask(platform + " build pending (" + seconds
											+ "s)");
									Thread.sleep(1000 * Math.min(seconds, 10));
									seconds += Math.min(seconds, 10);
									
								}
								if (monitor.isCanceled()){
							        return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.Status.CANCEL, Activator.kPluginID,"job canceled by user");
								}
								monitor.subTask("completed Building");
								monitor.worked(restBuild/i);
								restBuild = restBuild-(restBuild/i);
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
								monitor.subTask("downloading file for "+ platform);
								context.getIOAgent().printError("downloading file " + platform);
								phonegap.getApp(appname, id, platform,path);
								monitor.worked(restDownload/i);
								restDownload = restDownload-(restDownload/i);
								i--;
							}

						} else {
							throw new AuthenticationException(
									"username/password combination is invalid");
						}
			    	}catch (Exception e) {
			    		context.getIOAgent().printError(e.toString());
						Environment.logException(e);
						
						return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.Status.ERROR, Activator.kPluginID,e.getMessage(), e);
					}
			    	finally{
			    		monitor.done();
			    	}
			        return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.Status.OK, Activator.kPluginID,"job finished succesfully");
			    }
 
			
		}; 
		job.schedule();
		return current;
    }
    

}