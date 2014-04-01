import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class textNpic implements java.io.Serializable {
	public String text;
	public int[] pic = new int[65535];
	textNpic () {
		
	}
	textNpic (String text,String file) {
		this.text = text;
		FileInputStream input = null;
		int read;
		int i = 0;
		byte[] buffer = new byte[128];
		try {
			input = new FileInputStream(file);
			try {
				while ((read = input.read()) !=-1 ) {
					this.pic[i] = read;
					i++;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
