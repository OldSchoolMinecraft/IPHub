package net.oldschoolminecraft.iph.util;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class IPHubRequest
{
    private static final Gson gson = new Gson();

    private IPHubResponse response;
    private ExceptionPipe errorPipe;

    private String ip;
    private String apiKey;
    private int statusCode = 500;

    public IPHubRequest(String ip, String apiKey)
    {
        this.ip = ip;
        this.apiKey = apiKey;
    }

    public IPHubRequest onFail(ExceptionPipe pipe)
    {
        this.errorPipe = pipe;
        return this;
    }

    public IPHubRequest complete()
    {
        try (CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpGet httpGet = new HttpGet("http://v2.api.iphub.info/ip/" + ip);
            httpGet.setHeader("X-Key", apiKey);
            try (CloseableHttpResponse res = client.execute(httpGet))
            {
                HttpEntity ent = res.getEntity();
                this.statusCode = res.getStatusLine().getStatusCode();
                String rawResponse = EntityUtils.toString(ent);
                this.response = gson.fromJson(rawResponse, IPHubResponse.class);
//            System.out.println(String.format("[IPHub Log] %s: %s %s, %s (%s)", username, response.countryCode, ip, response.isp, response.asn));
                EntityUtils.consume(ent);
            }
        } catch (Exception ex) {
            if (errorPipe != null)
                errorPipe.flush(ex);
        }
        return this;
    }

    public IPHubResponse getResponse()
    {
        return response;
    }

    public int getStatusCode()
    {
        return statusCode;
    }
}
