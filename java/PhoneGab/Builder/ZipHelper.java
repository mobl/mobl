package PhoneGab.Builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipHelper {
	public static void zipDir(String zipFileName, String dir)
			throws Exception {
		File dirFile = new File(dir);
		ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(
				zipFileName));
		addDirectory(dirFile, zip, dirFile.getPath());
		zip.close();
	} 
 
	private static void addDirectory(File dir, ZipOutputStream zip,
			String removeDirpart) throws IOException {
		File[] files = dir.listFiles();
		String x = new File(".").getAbsolutePath();
		x = x.toLowerCase();
		for (File file : files) {
			if (file.isDirectory()) {
				addDirectory(file, zip, removeDirpart);
			} else {
				addFile(zip, file, removeDirpart);
			}
		}
	}

	private static void addFile(ZipOutputStream zip, File file,
			String removeDirpart) throws IOException, FileNotFoundException {
		byte[] Buf = new byte[1024];

		zip.putNextEntry(new ZipEntry(file.getPath().replace(removeDirpart,
				"")));
		FileInputStream in = new FileInputStream(file);
		int size = 0;
		while ((size = in.read(Buf)) != -1) {
			zip.write(Buf, 0, size);
		}
		in.close();
		zip.closeEntry();
	}

}