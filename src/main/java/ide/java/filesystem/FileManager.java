package ide.java.filesystem;

import java.io.*;

public class FileManager {

    public String openFile(File file){
        StringBuilder content = new StringBuilder();

        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null){
                content.append(line).append("\n");
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    return content.toString();

    }
    public void saveFile(File file, String content){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);

        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
