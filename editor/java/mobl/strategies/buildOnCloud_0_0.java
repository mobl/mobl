package mobl.strategies;


import java.io.File;
import java.security.InvalidParameterException;

import javax.activity.InvalidActivityException;
import javax.security.sasl.AuthenticationException;

import mobl.Builder.PhonegapAPIHelper;
import mobl.Builder.Status;
import mobl.Builder.ZipHelper;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;


public class buildOnCloud_0_0 extends Strategy {

    public static buildOnCloud_0_0 instance = new buildOnCloud_0_0();

    @Override
    public IStrategoTerm invoke(Context context, IStrategoTerm current) {
    	try{	
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
				phonegap.getApp(name, id, platform);
	
			} else {
				throw new AuthenticationException(
						"username/password combination is invalid");
			}
    	}catch (Exception e) {
			e.printStackTrace();
			return null;
		}
				
			
        return current;
    }
    

}