package com.bluedragonmc.api

object RpcRetryPolicy {

    /**
     * Idempotent and non-order-sensitive gRPC messages which are safe to retry automatically.
     */
    internal val retryableMethods: Map<String, Set<String>> = mapOf(
        "InstanceService" to setOf("InitGameServer", "CreateInstance", "RemoveInstance", "GetTotalPlayerCount"),
        "QueueService" to setOf("AddToQueue", "BulkAddToQueue", "RemoveFromQueue"),
        "MapService" to setOf("GetAvailableMaps", "UpdateMapConfig"),
        "PlayerTracker" to setOf("QueryPlayer"),
        "PartyService" to setOf("PartyList"),
        "Jukebox" to setOf("GetSongQueue", "SetSongQueue"),
        "GsClientService" to setOf("GetInstances", "EndGame", "CreateInstance"),
        "PlayerHolder" to setOf("GetPlayers"),
        "PlayerTransfer" to setOf("PrepareTransfer"),
        "LobbyService" to setOf("FindLobby"),
    )

    /**
     * A gRPC service config, suitable for [io.grpc.ManagedChannelBuilder.defaultServiceConfig].
     */
    val serviceConfig: Map<String, Any> = mapOf(
        "methodConfig" to retryableMethods.map { (service, methods) ->
            mapOf(
                "name" to methods.map { mapOf("service" to service, "method" to it) },
                "retryPolicy" to mapOf(
                    "maxAttempts" to 3,
                    "initialBackoff" to "0.1s",
                    "maxBackoff" to "1s",
                    "backoffMultiplier" to 2.0,
                    "retryableStatusCodes" to listOf("UNAVAILABLE"),
                ),
            )
        },
        "retryThrottling" to mapOf("maxTokens" to 10.0, "tokenRatio" to 0.1),
    )
}
