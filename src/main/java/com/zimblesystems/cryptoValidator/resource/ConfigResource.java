package com.zimblesystems.cryptoValidator.resource;

import com.zimblesystems.cryptoValidator.service.VaultService;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/config/load")
@Tag(name = "Config Resource Load", description = "Loads the config info")
public class ConfigResource {

    private final VaultService vaultService;

    public ConfigResource(VaultService vaultService) {
        this.vaultService = vaultService;
    }



    @POST
    @Path("/crypto")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "loadCryptoData",
            summary = "Loads Crypto Product and Keys",
            description = "Reloads  Crypto Product and Keys from Database into memory"
    )
    @APIResponses({
            @APIResponse(responseCode = "200", description = "All  Crypto Product and Keys is loaded from db"),
            @APIResponse(responseCode = "500", description = "any of the errors", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Uni<Response> loadCryptoData(){

        return vaultService.loadAllKeysNonBlocking()
                .onItem().transform(unused ->  Response.ok().entity(new JsonObject().put("message", "Crypto Keys loaded")).build())
                .onFailure().recoverWithItem(throwable -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new JsonObject().put("message", "Error Loading Crypto Keys")).build());
    }

}
