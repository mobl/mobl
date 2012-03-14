package PhoneGab.strategies;


import java.io.File;
import java.security.InvalidParameterException;

import javax.activity.InvalidActivityException;
import javax.security.sasl.AuthenticationException;



import mobl.Activator;

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


public class build$On$Cloudf_0_0 extends Strategy {  

    public static build$On$Cloudf_0_0 instance = new build$On$Cloudf_0_0();
     
    @Override
    public IStrategoTerm invoke(final Context context, final IStrategoTerm current) {
    	Job job = new Job("native build") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				 String path = uglify_0_0.getStringFromTerm(current);
			    	monitor.beginTask("native build", 100);
				 	try{	
				    	String name = "blaat";
						File dir = new File(path+"/nativejava/zip/");
						if (!dir.exists()) { 
							dir.mkdirs();
						} 
						monitor.worked(1);
						monitor.subTask("zippig dir");
						context.getIOAgent().printError("zipping dir");
						ZipHelper.zipDir(path+"/nativejava/zip/" + name + ".zip", path+"/nativejava/src/.");
						monitor.worked(14);
						PhonegapAPIHelper phonegap = new PhonegapAPIHelper();
						monitor.subTask("checking Credentials");
						phonegap.setCriedentials("chrismelman@hotmail.com", "weetikveel");
						context.getIOAgent().printError("checking Credentials");
						if (phonegap.TryAuthenticate()) {
							monitor.worked(10);
							context.getIOAgent().printError("Ok");
							monitor.subTask("get Application ID");
							context.getIOAgent().printError("get Application ID");
							int id = phonegap.getAppId(name);
							monitor.worked(10);
							String filelocation =path+"/nativejava/zip/" + name + ".zip";
							if (id == -1) {
								monitor.subTask("Create New Application");
								context.getIOAgent().printError("Create New Application");
								phonegap.createApp(name, filelocation);
								monitor.worked(15);
							} else {
								monitor.subTask("Update Source");
								context.getIOAgent().printError("Update Source");
								phonegap.updateAppSource(id, filelocation);
								monitor.worked(15);
							}
							String platform = "android";
							int seconds = 1;
							context.getIOAgent().printError("Start Building");
							monitor.subTask("Start Building");
							monitor.worked(1);
							monitor.subTask("build pending");
							while (phonegap.checkBuildingStatusApp(id, platform).equals(
									Status.PENDING)&&!monitor.isCanceled()) {
//								System.out.println(platform + " build pending (" + seconds
//										+ "s)");
//								context.getIOAgent().printError(platform + " build pending (" + seconds
//										+ "s)");
								monitor.subTask(platform + " build pending (" + seconds
										+ "s)");
								Thread.sleep(1000 * Math.min(seconds, 10));
								seconds += Math.min(seconds, 10);
								
							}
							if (monitor.isCanceled()){
						        return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.Status.CANCEL, Activator.kPluginID,"job canceled by user");
							}
							monitor.subTask("completed Building");
							monitor.worked(29);
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
							
							context.getIOAgent().printError("downloadingfile");
							phonegap.getApp(name, id, platform,path);
							monitor.worked(15);
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
							;
						
			        return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.Status.OK, Activator.kPluginID,"job finished succesfully");
			    }
 
			
		}; 
		job.schedule();
		//    	Display.getCurrent().
//    	IProgressMonitor monitor = new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).getProgressMonitor();
//    	monitor.beginTask("Build native", 100);
//    	 String path = uglify_0_0.getStringFromTerm(current);
//    	try{	
//	    	String name = "blaat";
//			File dir = new File(path+"/nativejava/zip/");
//			if (!dir.exists()) { 
//				dir.mkdirs();
//			} 
////			monitor.worked(1);
////			monitor.subTask("zippig dir");
//			context.getIOAgent().printError("zipping dir");
//			ZipHelper.zipDir(path+"/nativejava/zip/" + name + ".zip", path+"/nativejava/src/.");
////			monitor.worked(14);
//			PhonegapAPIHelper phonegap = new PhonegapAPIHelper();
////			monitor.subTask("checking Credentials");
//			phonegap.setCriedentials("chrismelman@hotmail.com", "weetikveel");
////			context.getIOAgent().printError("checking Credentials");
//			if (phonegap.TryAuthenticate()) {
////				monitor.worked(10);
//				context.getIOAgent().printError("Ok");
////				monitor.subTask("get Application ID");
//				context.getIOAgent().printError("get Application ID");
//				int id = phonegap.getAppId(name);
////				monitor.worked(10);
//				String filelocation =path+"/nativejava/zip/" + name + ".zip";
//				if (id == -1) {
////					monitor.subTask("Create New Application");
//					context.getIOAgent().printError("Create New Application");
//					phonegap.createApp(name, filelocation);
////					monitor.worked(15);
//				} else {
////					monitor.subTask("Update Source");
//					context.getIOAgent().printError("Update Source");
//					phonegap.updateAppSource(id, filelocation);
////					monitor.worked(15);
//				}
//				String platform = "android";
//				int seconds = 1;
//				context.getIOAgent().printError("Start Building");
////				monitor.subTask("Start Building");
////				monitor.worked(1);
////				monitor.subTask("build pending");
//				while (phonegap.checkBuildingStatusApp(id, platform).equals(
//						Status.PENDING)/*&&!monitor.isCanceled()*/) {
////					System.out.println(platform + " build pending (" + seconds
////							+ "s)");
//					context.getIOAgent().printError(platform + " build pending (" + seconds
//							+ "s)");
//					Thread.sleep(1000 * Math.min(seconds, 10));
//					seconds += Math.min(seconds, 10);
//					
//				}
////				monitor.subTask("completed Building");
////				monitor.worked(29);
//				switch (phonegap.checkBuildingStatusApp(id, platform)) {
//				case COMPLETE:
//					break;
//				case ERROR:
//					phonegap.getBuildError(id, platform);
//					break;
//				case NULL:
//					throw new InvalidParameterException(
//							"probably the platform doesn't have a good key for building");
//				default:
//					throw new InvalidActivityException(
//							"Something interfered with the building process");
//				}
//				
//				context.getIOAgent().printError("downloadingfile");
//				phonegap.getApp(name, id, platform,path);
////				monitor.worked(15);
//			} else {
//				throw new AuthenticationException(
//						"username/password combination is invalid");
//			}
//    	}catch (Exception e) {
//    		context.getIOAgent().printError(e.toString());
//			Environment.logException(e);
//			
//			return null;
//		}
//    	finally{
////    		monitor.done();
//    	}
//				
//			
//        return current;
		return current;
    }
    

}