package testSim.main;

import java.io.File;
import java.io.FileWriter;

import com.google.gson.Gson;

/**
 * write settings
 */
public class SettingsWriter
{
	public static void writeSettings(SimSettings settings)
	{
		// Used to convert the json returned from the
		// filewriter into a java object
		// for andcroid= just add the write compile

		Gson gson = new Gson();
		String json = gson.toJson(settings);
		try {
			FileWriter fos = new FileWriter(new File(settings.TRACE_OUT_DIR, "settings.json"));
			fos.write(json);
			fos.close();
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Failed to write settings JSON file");
		}
	}
}
