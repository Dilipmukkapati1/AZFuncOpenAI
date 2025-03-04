package com.function;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.ThroughputProperties;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class CosmosDBOperator {


    CosmosAsyncClient cosmosAsyncClient;
    String databaseName = "CustomerProfile";
    String containerName = "info";

    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
          * @throws Exception 
          */
    // @FunctionName("cosmosdboperator")
    public HttpResponseMessage run(
        @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS)
                    HttpRequestMessage<Optional<String>> request,
                final ExecutionContext context) throws Exception {
        context.getLogger().info("---------------Java HTTP trigger processed a request.---------------");

        cosmosAsyncClient = gClient(context);
        context.getLogger().info("---------------Established connection to CosmosDB---------------");

        createDatabaseIfNotExists(context);
        context.getLogger().info("---------------Created database if not exist---------------");
        
        createContainerIfNotExists(context);
        context.getLogger().info("---------------Created container if not exist---------------");

        // Parse query parameter
        String query = request.getQueryParameters().get("name");
        String name = request.getBody().orElse(query);

        if (name == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
        } else {
            // createItem(name);
            // context.getLogger().info("Created item in CosmosDB");

            // return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + name).build();
            return request.createResponseBuilder(HttpStatus.OK).body(readItem(name)).build();
        }
    }


    public CosmosAsyncClient gClient(final ExecutionContext context) {
        context.getLogger().info("---------------Creating Async Client.---------------");
        // CosmosAsyncClient cosmosAsyncClient = new CosmosClientBuilder()
        // .endpoint("https://azhoncosmosdb.documents.azure.com:443/")
        // // .preferredRegions(Collections.singletonList("Central US"))
        // // .consistencyLevel(ConsistencyLevel.EVENTUAL)
        // .buildAsyncClient();

        DefaultAzureCredential defaultCredential = new DefaultAzureCredentialBuilder()
        .managedIdentityClientId("dbbb9c57-fb1d-4320-b707-c9d83021f60c")
        .build();

        CosmosAsyncClient cosmosAsyncClient = new CosmosClientBuilder()
        .endpoint("https://azhoncosmosdb.documents.azure.com:443/")
        .credential(defaultCredential)
        .buildAsyncClient();

        context.getLogger().info("---------------Created Async Client.---------------");

        return cosmosAsyncClient;
    }

    private void createDatabaseIfNotExists(final ExecutionContext context) throws Exception {

        //  Create database if not exists
        //  <CreateDatabaseIfNotExists>
        cosmosAsyncClient
        .createDatabaseIfNotExists(databaseName)
        .block();
        //  </CreateDatabaseIfNotExists>
    }

    private void createContainerIfNotExists(final ExecutionContext context) throws Exception {

        //  Create container if not exists
        //  <CreateContainerIfNotExists>

        CosmosContainerProperties containerProperties = new CosmosContainerProperties(containerName, "/id");
        cosmosAsyncClient
        .getDatabase(databaseName)
        .createContainerIfNotExists(containerProperties, ThroughputProperties.createManualThroughput(400))
        .block();

        //  </CreateContainerIfNotExists>
    }

    public Customer readItem(String id) {
        return cosmosAsyncClient
        .getDatabase(databaseName)
        .getContainer(containerName)
        .readItem(id, new PartitionKey(id), Customer.class)
        .block()
        .getItem();
    }


    public void createItem(String name) {
        Customer customer = new Customer();
        customer.id = name;
        customer.name = name;
        customer.email = "test@test.com";
        customer.phone = "123-456-7890";

        cosmosAsyncClient
        .getDatabase(databaseName)
        .getContainer(containerName)
        .createItem(customer)
        .block();
    }
}
