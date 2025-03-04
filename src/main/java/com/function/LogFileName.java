package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Azure Blob trigger.
 */
public class LogFileName {
    /**
     * This function will be invoked when a new or updated blob is detected at the specified path. The blob contents are provided as input to this function.
     */
    @FunctionName("LogFileName")
    // @StorageAccount("AzureWebJobsStorage_azHonFuncJava")
    @StorageAccount("BLOB_STORAGE_CONNECTION__accountName")
    public void run(
        @BlobTrigger(name = "content", path = "samples-workitems/{name}", dataType = "binary") byte[] content,
        @BindingName("name") String name,
        @BlobOutput(name = "outputBlob", path = "dilipscontaineroutput/{name}")
        OutputBinding<byte[]> outputBlob,

        final ExecutionContext context
    ) {
        outputBlob.setValue(content);
        context.getLogger().info("Java Blob trigger function processed a blob. Name: " + name + "\n  Size: " + content.length + " Bytes");
    }
}
