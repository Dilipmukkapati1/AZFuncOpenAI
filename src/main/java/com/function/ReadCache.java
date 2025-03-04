package com.function;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;

import com.azure.core.credential.TokenRequestContext;
import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.data.appconfiguration.models.ConfigurationSetting;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class ReadCache {
    /**
     * This function listens at endpoint "/api/readCache". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/readCache
     * 2. curl {your host}/api/readCache?name=HTTP%20Query
     */

    public String cacheHostname = "azhoncache.redis.cache.windows.net";

    // @FunctionName("readCache")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", 
            methods = {HttpMethod.GET, HttpMethod.POST}, 
            authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String query = request.getQueryParameters().get("name");
        String name = request.getBody().orElse(query);

        if (name == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
        } else {
            // testRedis();
            getConfigVal();
            return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + name).build();
        }
    }

    public void testRedis() {
        boolean useSsl = true;

        //Construct a Token Credential from Identity library, e.g. DefaultAzureCredential / ClientSecretCredential / Client CertificateCredential / ManagedIdentityCredential etc.
        DefaultAzureCredential defaultAzureCredential = new DefaultAzureCredentialBuilder()
        .managedIdentityClientId("e4a56c20-fb1d-4bf0-b28c-8c8504facbfd")
        .build();

        // Fetch a Microsoft Entra token to be used for authentication. This token will be used as the password.
                String token = defaultAzureCredential
                        .getToken(new TokenRequestContext()
                                .addScopes("https://redis.azure.com/.default"))
                                .block()
                                .getToken();

        // String username = "3306f525-7beb-4919-ad64-0dfc0d6ec88d";
        // String username = "45ecc91a-159b-4203-b346-a0a975a0a0b4";
        String username = System.getenv("USER_NAME");

        System.out.println("Username: " + username);
        
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_CACHE_PORT", "6380"));

        // Connect to the Azure Cache for Redis over the TLS/SSL port using the key.
        Jedis jedis = new Jedis(cacheHostname, port, DefaultJedisClientConfig.builder()
                .password(token) // Microsoft Entra access token as password is required.
                .user(username) // Username is Required
                .ssl(useSsl) // SSL Connection is Required
                .build());


        // Simple PING command
        System.out.println( "\nCache Command  : Ping" );
        System.out.println( "Cache Response : " + jedis.ping());

        // Simple get and put of integral data types into the cache
        System.out.println( "\nCache Command  : GET Message" );
        System.out.println( "Cache Response : " + jedis.get("Message"));

        System.out.println( "\nCache Command  : SET Message" );
        System.out.println( "Cache Response : " + jedis.set("Message", "Hello! The cache is working from Java!"));

        // Demonstrate "SET Message" executed as expected...
        System.out.println( "\nCache Command  : GET Message" );
        System.out.println( "Cache Response : " + jedis.get("Message"));

        // Get the client list, useful to see if connection list is growing...
        System.out.println( "\nCache Command  : CLIENT LIST" );
        System.out.println( "Cache Response : " + jedis.clientList());

        jedis.close();
    }

    public void getConfigVal() {
        DefaultAzureCredential defaultAzureCredential = new DefaultAzureCredentialBuilder().build();
        String token = defaultAzureCredential.getTokenSync(new TokenRequestContext().addScopes("https://azconfig.io/.default"))
        .getToken();

        System.out.println("\n" + token + "\n");

        ConfigurationClient client = new ConfigurationClientBuilder()
        .credential(defaultAzureCredential)
        .endpoint("https://azhonconfiguration.azconfig.io")
        .buildClient();

        // Read a specific key
        String key = "USER_NAME"; // Replace with your key
        try {
            ConfigurationSetting setting = client.getConfigurationSetting(key, null); // null for no label
            if (setting != null && setting.getValue() != null) {
                System.out.println("Key: " + setting.getKey() + ", Value: " + setting.getValue());
            } else {
                System.out.println("Key '" + key + "' not found.");
            }
        } catch (Exception e) {
            System.err.println("Error retrieving configuration: " + e.getMessage());
        }
    }
}