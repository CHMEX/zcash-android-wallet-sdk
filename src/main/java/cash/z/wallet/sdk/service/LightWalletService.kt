package cash.z.wallet.sdk.service

import cash.z.wallet.sdk.rpc.CompactFormats
import cash.z.wallet.sdk.rpc.Service

/**
 * Service for interacting with lightwalletd. Implementers of this service should make blocking
 * calls because async concerns are handled at a higher level.
 */
interface LightWalletService {
    /**
     * Return the given range of blocks.
     *
     * @param heightRange the inclusive range to fetch. For instance if 1..5 is given, then every
     * block in that range will be fetched, including 1 and 5.
     */
    fun getBlockRange(heightRange: IntRange): List<CompactFormats.CompactBlock>

    /**
     * Return the latest block height known to the service.
     */
    fun getLatestBlockHeight(): Int

    /**
     * Submit a raw transaction.
     */
    fun submitTransaction(spendTransaction: ByteArray): Service.SendResponse

    /**
     * Cleanup any connections when the service is shutting down and not going to be used again.
     */
    fun shutdown()
}