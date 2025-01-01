package net.oldschoolminecraft.iph;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Level;

public class UpdateManager
{
    private final IPHub plugin;
    private final String apiUrl;
    private final String currentVersion;

    public UpdateManager(IPHub plugin, String apiUrl)
    {
        this.plugin = plugin;
        this.apiUrl = apiUrl;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkForUpdates()
    {
        try
        {
            JsonObject updateData = fetchUpdateData(apiUrl);

            if (updateData == null)
            {
                Bukkit.getLogger().log(Level.WARNING, "Failed to fetch update data.");
                return;
            }

            String updateVersion = updateData.get("version").getAsString();
            String fileURL = updateData.get("url").getAsString();

            if (isNewVersion(updateVersion))
                downloadUpdate(fileURL);
        } catch (Exception ignored) {}
    }

    private JsonObject fetchUpdateData(String apiUrl) throws Exception
    {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream()))
            {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
        return null;
    }

    private boolean isNewVersion(String updateVersion)
    {
        return !currentVersion.equalsIgnoreCase(updateVersion);
    }

    /**
     * Write the plugin update into Bukkit's update folder.
     * When the server next tries loading the plugin, it should detect the update-
     * and replace the file automatically on it's own.
     * @param fileURL
     */
    private void downloadUpdate(String fileURL)
    {
        try
        {
            File updateFolderHandle = new File("plugins/update/");
            if (!updateFolderHandle.exists()) updateFolderHandle.mkdirs();
            Path updateFolder = new File(Bukkit.getUpdateFolder()).toPath();
            Path updateFile = updateFolder.resolve(plugin.getPluginFile().getName());

            HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
            connection.setRequestMethod("GET");

            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(updateFile.toFile()))
            {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer, 0, 1024)) != -1)
                {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception ignored) {}
    }
}